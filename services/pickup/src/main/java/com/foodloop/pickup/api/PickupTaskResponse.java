package com.foodloop.pickup.api;

import com.foodloop.pickup.domain.PickupTask;
import java.time.Instant;
import java.util.UUID;

public record PickupTaskResponse(
        UUID id,
        UUID tenantId,
        UUID claimId,
        UUID foodListingId,
        UUID donorUserId,
        UUID receiverUserId,
        UUID assignedVolunteerId,
        String status,
        Instant scheduledWindowStart,
        Instant scheduledWindowEnd,
        double latitude,
        double longitude,
        Instant completedAt,
        Instant createdAt) {

    public static PickupTaskResponse from(PickupTask task) {
        return new PickupTaskResponse(
                task.getId(),
                task.getTenantId(),
                task.getClaimId(),
                task.getFoodListingId(),
                task.getDonorUserId(),
                task.getReceiverUserId(),
                task.getAssignedVolunteerId(),
                task.getStatus().name(),
                task.getScheduledWindowStart(),
                task.getScheduledWindowEnd(),
                task.getPickupLocation().getY(),
                task.getPickupLocation().getX(),
                task.getCompletedAt(),
                task.getCreatedAt());
    }
}
