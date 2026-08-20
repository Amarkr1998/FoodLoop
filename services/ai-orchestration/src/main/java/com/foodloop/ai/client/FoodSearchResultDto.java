package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** The subset of Food's {@code GET /api/v1/food-listings} (public search) response the NGO Coordination Agent reasons about. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FoodSearchResultDto(
        UUID id, UUID donorOrgId, String foodCategory, BigDecimal quantityValue, String quantityUnit,
        Instant expiryTime, String status) {
}
