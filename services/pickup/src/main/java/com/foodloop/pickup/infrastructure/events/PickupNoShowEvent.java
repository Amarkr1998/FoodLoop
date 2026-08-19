package com.foodloop.pickup.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

public record PickupNoShowEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID pickupTaskId, UUID foodListingId, UUID claimId) {

    public static final String TOPIC = "pickup.no_show.v1";

    public static PickupNoShowEvent of(UUID tenantId, UUID pickupTaskId, UUID foodListingId, UUID claimId) {
        return new PickupNoShowEvent(
                UUID.randomUUID(), "PICKUP_NO_SHOW", 1, tenantId, Instant.now(), "pickup-service",
                pickupTaskId, foodListingId, claimId);
    }
}
