package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReportDto(UUID id, UUID reporterUserId, UUID targetUserId, String reason, String description, Instant createdAt) {
}
