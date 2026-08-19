package com.foodloop.food.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

public record FoodListedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID foodListingId, UUID donorOrgId, String foodCategory, Instant expiryTime) {

    public static final String TOPIC = "food.listed.v1";

    public static FoodListedEvent of(UUID tenantId, UUID foodListingId, UUID donorOrgId, String foodCategory, Instant expiryTime) {
        return new FoodListedEvent(
                UUID.randomUUID(), "FOOD_LISTED", 1, tenantId, Instant.now(), "food-service",
                foodListingId, donorOrgId, foodCategory, expiryTime);
    }
}
