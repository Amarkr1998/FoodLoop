package com.foodloop.impact.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FoodListingDto(UUID id, UUID donorOrgId, String foodCategory, BigDecimal quantityValue, String quantityUnit) {
}
