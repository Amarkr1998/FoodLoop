package com.foodloop.impact.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * One row per completed pickup — the fact this whole service's aggregates
 * are computed from (spec Phase 11). Immutable once written: a correction
 * would mean re-deriving from source data, not editing history in place.
 */
@Entity
@Table(name = "rescue_record", schema = "impact")
public class RescueRecord {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "pickup_task_id", nullable = false, updatable = false)
    private UUID pickupTaskId;

    @Column(name = "food_listing_id", nullable = false, updatable = false)
    private UUID foodListingId;

    @Column(name = "donor_user_id", nullable = false, updatable = false)
    private UUID donorUserId;

    @Column(name = "donor_org_id", nullable = false, updatable = false)
    private UUID donorOrgId;

    @Column(name = "receiver_user_id", nullable = false, updatable = false)
    private UUID receiverUserId;

    @Column(name = "food_category", nullable = false, updatable = false)
    private String foodCategory;

    @Column(name = "quantity_value", nullable = false, updatable = false)
    private BigDecimal quantityValue;

    @Column(name = "quantity_unit", nullable = false, updatable = false)
    private String quantityUnit;

    @Column(name = "estimated_kg_saved", nullable = false, updatable = false)
    private BigDecimal estimatedKgSaved;

    @Column(name = "estimated_co2_saved_kg", nullable = false, updatable = false)
    private BigDecimal estimatedCo2SavedKg;

    @Column(name = "completed_at", nullable = false, updatable = false)
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RescueRecord() {
        // JPA
    }

    public RescueRecord(
            UUID tenantId, UUID pickupTaskId, UUID foodListingId, UUID donorUserId, UUID donorOrgId,
            UUID receiverUserId, String foodCategory, BigDecimal quantityValue, String quantityUnit,
            BigDecimal estimatedKgSaved, BigDecimal estimatedCo2SavedKg, Instant completedAt) {
        this.tenantId = tenantId;
        this.pickupTaskId = pickupTaskId;
        this.foodListingId = foodListingId;
        this.donorUserId = donorUserId;
        this.donorOrgId = donorOrgId;
        this.receiverUserId = receiverUserId;
        this.foodCategory = foodCategory;
        this.quantityValue = quantityValue;
        this.quantityUnit = quantityUnit;
        this.estimatedKgSaved = estimatedKgSaved;
        this.estimatedCo2SavedKg = estimatedCo2SavedKg;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getPickupTaskId() {
        return pickupTaskId;
    }

    public UUID getFoodListingId() {
        return foodListingId;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public UUID getDonorOrgId() {
        return donorOrgId;
    }

    public UUID getReceiverUserId() {
        return receiverUserId;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public BigDecimal getQuantityValue() {
        return quantityValue;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public BigDecimal getEstimatedKgSaved() {
        return estimatedKgSaved;
    }

    public BigDecimal getEstimatedCo2SavedKg() {
        return estimatedCo2SavedKg;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
