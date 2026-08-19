package com.foodloop.food.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * First-come-first-served claiming (donor-approval flow is a later
 * iteration — spec §13 supports both, this phase implements FCFS only).
 * {@code idempotency_key} plus the partial unique index on
 * (food_listing_id) WHERE status IN ('PENDING','CONFIRMED') is the actual
 * double-claim guard (V1__create_food_listing_and_claim.sql); the
 * optimistic lock on FoodListing is the first line of defense, this table
 * constraint is the backstop.
 */
@Entity
@Table(name = "claim", schema = "food")
public class Claim {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "food_listing_id", nullable = false, updatable = false)
    private UUID foodListingId;

    @Column(name = "receiver_user_id", nullable = false, updatable = false)
    private UUID receiverUserId;

    @Column(name = "receiver_org_id")
    private UUID receiverOrgId;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.CONFIRMED;

    @CreationTimestamp
    @Column(name = "claimed_at", nullable = false, updatable = false)
    private Instant claimedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected Claim() {
        // JPA
    }

    public Claim(UUID tenantId, UUID foodListingId, UUID receiverUserId, UUID receiverOrgId,
            String idempotencyKey, Instant expiresAt) {
        this.tenantId = tenantId;
        this.foodListingId = foodListingId;
        this.receiverUserId = receiverUserId;
        this.receiverOrgId = receiverOrgId;
        this.idempotencyKey = idempotencyKey;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getFoodListingId() {
        return foodListingId;
    }

    public UUID getReceiverUserId() {
        return receiverUserId;
    }

    public UUID getReceiverOrgId() {
        return receiverOrgId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
