package com.foodloop.food.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

public record PickupNoShowEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID pickupTaskId, UUID foodListingId, UUID claimId) {

    public static final String TOPIC = "pickup.no_show.v1";
}
