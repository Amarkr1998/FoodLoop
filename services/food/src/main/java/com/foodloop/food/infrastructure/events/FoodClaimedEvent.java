package com.foodloop.food.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

public record FoodClaimedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID foodListingId, UUID claimId, UUID receiverUserId) {

    public static final String TOPIC = "food.claimed.v1";

    public static FoodClaimedEvent of(UUID tenantId, UUID foodListingId, UUID claimId, UUID receiverUserId) {
        return new FoodClaimedEvent(
                UUID.randomUUID(), "FOOD_CLAIMED", 1, tenantId, Instant.now(), "food-service",
                foodListingId, claimId, receiverUserId);
    }
}
