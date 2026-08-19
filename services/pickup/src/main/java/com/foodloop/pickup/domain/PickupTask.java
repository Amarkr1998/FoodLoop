package com.foodloop.pickup.domain;

import com.foodloop.commons.web.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "pickup_task", schema = "pickup")
public class PickupTask {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "claim_id", nullable = false, updatable = false)
    private UUID claimId;

    @Column(name = "food_listing_id", nullable = false, updatable = false)
    private UUID foodListingId;

    @Column(name = "donor_user_id", nullable = false, updatable = false)
    private UUID donorUserId;

    @Column(name = "receiver_user_id", nullable = false, updatable = false)
    private UUID receiverUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PickupStatus status = PickupStatus.SCHEDULED;

    @Column(name = "scheduled_window_start", nullable = false)
    private Instant scheduledWindowStart;

    @Column(name = "scheduled_window_end", nullable = false)
    private Instant scheduledWindowEnd;

    @Column(name = "pickup_location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point pickupLocation;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected PickupTask() {
        // JPA
    }

    public PickupTask(
            UUID tenantId, UUID claimId, UUID foodListingId, UUID donorUserId, UUID receiverUserId,
            Instant scheduledWindowStart, Instant scheduledWindowEnd, Point pickupLocation) {
        this.tenantId = tenantId;
        this.claimId = claimId;
        this.foodListingId = foodListingId;
        this.donorUserId = donorUserId;
        this.receiverUserId = receiverUserId;
        this.scheduledWindowStart = scheduledWindowStart;
        this.scheduledWindowEnd = scheduledWindowEnd;
        this.pickupLocation = pickupLocation;
    }

    public void complete() {
        transitionTo(PickupStatus.COMPLETED);
        this.completedAt = Instant.now();
    }

    public void reportNoShow() {
        transitionTo(PickupStatus.NO_SHOW);
    }

    private void transitionTo(PickupStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new ApiException("INVALID_STATE_TRANSITION", HttpStatus.CONFLICT,
                    "Cannot transition pickup task from " + status + " to " + target + ".");
        }
        this.status = target;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getClaimId() {
        return claimId;
    }

    public UUID getFoodListingId() {
        return foodListingId;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public UUID getReceiverUserId() {
        return receiverUserId;
    }

    public PickupStatus getStatus() {
        return status;
    }

    public Instant getScheduledWindowStart() {
        return scheduledWindowStart;
    }

    public Instant getScheduledWindowEnd() {
        return scheduledWindowEnd;
    }

    public Point getPickupLocation() {
        return pickupLocation;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
