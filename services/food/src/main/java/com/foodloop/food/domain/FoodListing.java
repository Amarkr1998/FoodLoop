package com.foodloop.food.domain;

import com.foodloop.commons.web.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;

/**
 * The food state machine (FoodStatus, §11) lives on this aggregate:
 * {@link #transitionTo} is the only way its status changes, so an invalid
 * transition fails here rather than being possible to express at all.
 * Optimistic locking ({@code @Version}) is what makes a claim
 * race-condition-safe (§13, §46) — two concurrent transitions racing off
 * the same starting status will not both succeed; see ClaimService.
 */
@Entity
@Table(name = "food_listing", schema = "food")
public class FoodListing {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "donor_org_id", nullable = false, updatable = false)
    private UUID donorOrgId;

    @Column(name = "donor_user_id", nullable = false, updatable = false)
    private UUID donorUserId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_category", nullable = false)
    private FoodCategory foodCategory;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dietary_types", nullable = false, columnDefinition = "text[]")
    private List<String> dietaryTypes;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> allergens;

    @Column(name = "quantity_value", nullable = false)
    private BigDecimal quantityValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_unit", nullable = false)
    private QuantityUnit quantityUnit;

    @Column(name = "estimated_servings")
    private Integer estimatedServings;

    @Column(name = "preparation_time")
    private Instant preparationTime;

    @Column(name = "expiry_time", nullable = false)
    private Instant expiryTime;

    @Column(name = "pickup_start_time", nullable = false)
    private Instant pickupStartTime;

    @Column(name = "pickup_end_time", nullable = false)
    private Instant pickupEndTime;

    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "approx_location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point approxLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodStatus status = FoodStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private FoodVerificationStatus verificationStatus = FoodVerificationStatus.UNVERIFIED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_metadata")
    private FoodAiMetadata aiMetadata;

    /** The Safety Agent's pre-publish hold (spec §22) — see V2 migration's Javadoc for why this isn't a FoodStatus. */
    @Column(name = "requires_safety_review", nullable = false)
    private boolean requiresSafetyReview = false;

    @Column(name = "safety_review_reason")
    private String safetyReviewReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected FoodListing() {
        // JPA
    }

    public FoodListing(
            UUID tenantId, UUID donorOrgId, UUID donorUserId, String title, String description,
            FoodCategory foodCategory, List<String> dietaryTypes, List<String> allergens,
            BigDecimal quantityValue, QuantityUnit quantityUnit, Integer estimatedServings,
            Instant preparationTime, Instant expiryTime, Instant pickupStartTime, Instant pickupEndTime,
            Point location, Point approxLocation) {
        this.tenantId = tenantId;
        this.donorOrgId = donorOrgId;
        this.donorUserId = donorUserId;
        this.title = title;
        this.description = description;
        this.foodCategory = foodCategory;
        this.dietaryTypes = dietaryTypes;
        this.allergens = allergens;
        this.quantityValue = quantityValue;
        this.quantityUnit = quantityUnit;
        this.estimatedServings = estimatedServings;
        this.preparationTime = preparationTime;
        this.expiryTime = expiryTime;
        this.pickupStartTime = pickupStartTime;
        this.pickupEndTime = pickupEndTime;
        this.location = location;
        this.approxLocation = approxLocation;
    }

    /**
     * The only way {@link #status} changes. An illegal transition throws
     * rather than silently no-opping, so a caller can never accidentally
     * skip a state — matches spec §11's "invalid transitions must fail
     * safely."
     */
    public void transitionTo(FoodStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new ApiException("INVALID_STATE_TRANSITION", HttpStatus.CONFLICT,
                    "Cannot transition food listing from " + status + " to " + target + ".");
        }
        if (target == FoodStatus.PUBLISHED && requiresSafetyReview) {
            throw new ApiException("SAFETY_REVIEW_REQUIRED", HttpStatus.CONFLICT,
                    "This listing is on hold for safety review and cannot be published yet.");
        }
        this.status = target;
    }

    /**
     * The Safety Agent's write path (spec §22) — a deterministic guard
     * ({@link #transitionTo}), not the agent's own raw text, is what
     * actually blocks publish; this method only ever sets the flag, never
     * clears it, so an agent run can never talk its way past a hold a
     * previous run raised.
     */
    public void flagForSafetyReview(String reason) {
        this.requiresSafetyReview = true;
        this.safetyReviewReason = reason;
    }

    /** Human-only (see FoodListingController's requireTrustOpsCaller) — the one way a hold is lifted. */
    public void clearSafetyReview() {
        this.requiresSafetyReview = false;
        this.safetyReviewReason = null;
    }

    /**
     * Records the Food Intelligence Agent's suggestions (spec §16) — advisory
     * only, the donor's own fields above are never overwritten by this.
     * Restricted to DRAFT: the only trigger this phase implements is the
     * donor-initiated synchronous analyze call
     * (docs/architecture/05-ai-agent-architecture.md §3), not the
     * event-driven background quality pass on already-published listings —
     * that's future work, not a limitation baked into the data model.
     */
    public void recordAiMetadata(FoodAiMetadata metadata) {
        if (status != FoodStatus.DRAFT) {
            throw new ApiException("LISTING_NOT_DRAFT", HttpStatus.CONFLICT,
                    "AI analysis can only be recorded while a listing is in DRAFT (current status: " + status + ").");
        }
        this.aiMetadata = metadata;
        this.verificationStatus = FoodVerificationStatus.AI_REVIEWED;
    }

    public FoodAiMetadata getAiMetadata() {
        return aiMetadata;
    }

    public boolean isRequiresSafetyReview() {
        return requiresSafetyReview;
    }

    public String getSafetyReviewReason() {
        return safetyReviewReason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getDonorOrgId() {
        return donorOrgId;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public FoodCategory getFoodCategory() {
        return foodCategory;
    }

    public List<String> getDietaryTypes() {
        return dietaryTypes;
    }

    public List<String> getAllergens() {
        return allergens;
    }

    public BigDecimal getQuantityValue() {
        return quantityValue;
    }

    public QuantityUnit getQuantityUnit() {
        return quantityUnit;
    }

    public Integer getEstimatedServings() {
        return estimatedServings;
    }

    public Instant getExpiryTime() {
        return expiryTime;
    }

    public Instant getPickupStartTime() {
        return pickupStartTime;
    }

    public Instant getPickupEndTime() {
        return pickupEndTime;
    }

    public Point getLocation() {
        return location;
    }

    public Point getApproxLocation() {
        return approxLocation;
    }

    public FoodStatus getStatus() {
        return status;
    }

    public FoodVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }
}
