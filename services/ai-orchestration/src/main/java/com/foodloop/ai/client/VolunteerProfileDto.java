package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VolunteerProfileDto(
        UUID id, UUID userId, String vehicleType, Integer capacityServings, boolean available,
        Double latitude, Double longitude) {
}
