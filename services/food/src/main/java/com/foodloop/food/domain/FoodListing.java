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
        this.status = target;
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
