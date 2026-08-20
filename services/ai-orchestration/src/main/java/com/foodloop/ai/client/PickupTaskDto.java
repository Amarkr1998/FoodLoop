package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PickupTaskDto(
        UUID id, UUID assignedVolunteerId, String status, Instant scheduledWindowStart, Instant scheduledWindowEnd,
        double latitude, double longitude) {
}
