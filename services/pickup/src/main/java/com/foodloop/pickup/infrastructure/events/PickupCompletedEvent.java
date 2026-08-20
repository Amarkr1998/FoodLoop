package com.foodloop.pickup.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code donorUserId}/{@code receiverUserId} were added for the Impact
 * service's fact table (Phase 11) — PickupTask already carries both on
 * itself, so this is a zero-lookup addition, not a new cross-service call.
 * Existing consumers (Food's own copy of this record) simply don't declare
 * these fields and Jackson ignores the extra JSON properties by default —
 * an additive, backward-compatible event change.
 */
public record PickupCompletedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID pickupTaskId, UUID foodListingId, UUID claimId, UUID donorUserId, UUID receiverUserId) {

    public static final String TOPIC = "pickup.completed.v1";

    public static PickupCompletedEvent of(
            UUID tenantId, UUID pickupTaskId, UUID foodListingId, UUID claimId, UUID donorUserId, UUID receiverUserId) {
        return new PickupCompletedEvent(
                UUID.randomUUID(), "PICKUP_COMPLETED", 1, tenantId, Instant.now(), "pickup-service",
                pickupTaskId, foodListingId, claimId, donorUserId, receiverUserId);
    }
}
