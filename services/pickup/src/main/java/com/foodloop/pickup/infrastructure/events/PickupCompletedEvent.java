package com.foodloop.pickup.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

public record PickupCompletedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID pickupTaskId, UUID foodListingId, UUID claimId) {

    public static final String TOPIC = "pickup.completed.v1";

    public static PickupCompletedEvent of(UUID tenantId, UUID pickupTaskId, UUID foodListingId, UUID claimId) {
        return new PickupCompletedEvent(
                UUID.randomUUID(), "PICKUP_COMPLETED", 1, tenantId, Instant.now(), "pickup-service",
                pickupTaskId, foodListingId, claimId);
    }
}
