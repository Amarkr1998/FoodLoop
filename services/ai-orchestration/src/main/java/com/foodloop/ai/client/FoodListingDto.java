package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The subset of Food's {@code GET /api/v1/food-listings/{id}} response an
 * agent actually reasons about. {@code ignoreUnknown} because this is a
 * deliberately narrower view of Food's full response shape, not a mirror of
 * it — Food is free to add fields without breaking this client.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FoodListingDto(
        UUID id,
        String title,
        String description,
        String foodCategory,
        List<String> dietaryTypes,
        List<String> allergens,
        BigDecimal quantityValue,
        String quantityUnit,
        Integer estimatedServings,
        Instant expiryTime,
        Instant pickupStartTime,
        Instant pickupEndTime,
        String status) {
}
