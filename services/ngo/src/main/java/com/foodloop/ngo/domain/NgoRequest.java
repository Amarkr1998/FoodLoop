package com.foodloop.ngo.domain;

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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.http.HttpStatus;

/**
 * One NGO's bulk ask for a category/quantity of food by a deadline (spec
 * §19, docs/architecture/01-bounded-contexts.md's "bulk requests"). Distinct
 * from a {@code MatchProposal}: a request is the NGO's stated demand,
 * created before any match exists; {@code matchedProposalId} is set once
 * the NGO Coordination Agent (or a human) proposes a fulfilling match —
 * see MatchProposedListener.
 */
@Entity
@Table(name = "ngo_request", schema = "ngo")
public class NgoRequest {

    private static final Map<NgoRequestStatus, Set<NgoRequestStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(NgoRequestStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(NgoRequestStatus.OPEN, EnumSet.of(
                NgoRequestStatus.MATCHED, NgoRequestStatus.EXPIRED, NgoRequestStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(NgoRequestStatus.MATCHED, EnumSet.of(
                NgoRequestStatus.FULFILLED, NgoRequestStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(NgoRequestStatus.FULFILLED, EnumSet.noneOf(NgoRequestStatus.class));
        ALLOWED_TRANSITIONS.put(NgoRequestStatus.EXPIRED, EnumSet.noneOf(NgoRequestStatus.class));
        ALLOWED_TRANSITIONS.put(NgoRequestStatus.CANCELLED, EnumSet.noneOf(NgoRequestStatus.class));
    }

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "ngo_org_id", nullable = false, updatable = false)
    private UUID ngoOrgId;

    @Column(name = "food_category", nullable = false, updatable = false)
    private String foodCategory;

    @Column(name = "quantity_needed", nullable = false, updatable = false)
    private BigDecimal quantityNeeded;

    @Column(name = "quantity_unit", nullable = false, updatable = false)
    private String quantityUnit;

    @Column(name = "needed_before", nullable = false, updatable = false)
    private Instant neededBefore;

    @Column
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NgoRequestStatus status = NgoRequestStatus.OPEN;

    @Column(name = "matched_proposal_id")
    private UUID matchedProposalId;

    @Column(name = "matched_food_listing_id")
    private UUID matchedFoodListingId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected NgoRequest() {
        // JPA
    }

    public NgoRequest(
            UUID tenantId, UUID ngoOrgId, String foodCategory, BigDecimal quantityNeeded, String quantityUnit,
            Instant neededBefore, String notes) {
        this.tenantId = tenantId;
        this.ngoOrgId = ngoOrgId;
        this.foodCategory = foodCategory;
        this.quantityNeeded = quantityNeeded;
        this.quantityUnit = quantityUnit;
        this.neededBefore = neededBefore;
        this.notes = notes;
    }

    /** Idempotent by design: a redelivered match.proposed.v1 for the same request is a no-op once already MATCHED. */
    public void markMatched(UUID proposalId, UUID foodListingId) {
        if (status != NgoRequestStatus.OPEN) {
            return;
        }
        transitionTo(NgoRequestStatus.MATCHED);
        this.matchedProposalId = proposalId;
        this.matchedFoodListingId = foodListingId;
    }

    public void markFulfilled() {
        transitionTo(NgoRequestStatus.FULFILLED);
    }

    public void cancel() {
        transitionTo(NgoRequestStatus.CANCELLED);
    }

    public void expire() {
        transitionTo(NgoRequestStatus.EXPIRED);
    }

    private void transitionTo(NgoRequestStatus target) {
        if (!ALLOWED_TRANSITIONS.get(status).contains(target)) {
            throw new ApiException("INVALID_STATE_TRANSITION", HttpStatus.CONFLICT,
                    "Cannot transition NGO request from " + status + " to " + target + ".");
        }
        this.status = target;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getNgoOrgId() {
        return ngoOrgId;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public BigDecimal getQuantityNeeded() {
        return quantityNeeded;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public Instant getNeededBefore() {
        return neededBefore;
    }

    public String getNotes() {
        return notes;
    }

    public NgoRequestStatus getStatus() {
        return status;
    }

    public UUID getMatchedProposalId() {
        return matchedProposalId;
    }

    public UUID getMatchedFoodListingId() {
        return matchedFoodListingId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
