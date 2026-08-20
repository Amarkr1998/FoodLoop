package com.foodloop.matching.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FoodListingDto(UUID id, String status, Instant expiryTime, double latitude, double longitude) {
}
