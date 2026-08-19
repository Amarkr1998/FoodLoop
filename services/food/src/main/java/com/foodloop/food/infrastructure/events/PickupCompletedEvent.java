package com.foodloop.food.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

/**
 * This service's own view of the event contract published by Pickup
 * (services/pickup/.../PickupCompletedEvent) — see that class's producer-
 * side Javadoc, and Pickup's FoodClaimedEvent consumer-side Javadoc, for
 * why a consumer keeps its own copy rather than sharing the producer's
 * Java class.
 */
public record PickupCompletedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID pickupTaskId, UUID foodListingId, UUID claimId) {

    public static final String TOPIC = "pickup.completed.v1";
}
