package com.foodloop.pickup.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

/**
 * This service's own view of the event contract published by Food
 * (services/food/.../FoodClaimedEvent) — a consumer defines what it needs
 * rather than sharing the producer's Java class, so the two services can
 * evolve independently as long as the wire schema stays compatible
 * (docs/architecture/04-event-catalog.md's additive-only evolution rule).
 */
public record FoodClaimedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID foodListingId, UUID claimId, UUID receiverUserId, UUID donorUserId,
        Instant pickupStartTime, Instant pickupEndTime, double latitude, double longitude) {

    public static final String TOPIC = "food.claimed.v1";
}
