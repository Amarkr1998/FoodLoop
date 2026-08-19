package com.foodloop.food.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Carries enough context for the Pickup context to create a pickup task
 * without a synchronous cross-service call back into Food — event-driven
 * decoupling only holds if the event is self-sufficient (matching context
 * doesn't exist yet, Phase 7, so this is the direct FCFS-claim equivalent
 * of what would otherwise be a MATCH_ACCEPTED event).
 */
public record FoodClaimedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID foodListingId, UUID claimId, UUID receiverUserId, UUID donorUserId,
        Instant pickupStartTime, Instant pickupEndTime, double latitude, double longitude) {

    public static final String TOPIC = "food.claimed.v1";

    public static FoodClaimedEvent of(
            UUID tenantId, UUID foodListingId, UUID claimId, UUID receiverUserId, UUID donorUserId,
            Instant pickupStartTime, Instant pickupEndTime, double latitude, double longitude) {
        return new FoodClaimedEvent(
                UUID.randomUUID(), "FOOD_CLAIMED", 1, tenantId, Instant.now(), "food-service",
                foodListingId, claimId, receiverUserId, donorUserId, pickupStartTime, pickupEndTime, latitude, longitude);
    }
}
