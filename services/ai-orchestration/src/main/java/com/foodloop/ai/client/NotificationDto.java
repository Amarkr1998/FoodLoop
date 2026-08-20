package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationDto(UUID id, UUID recipientOrgId, String channel, String subject, String status) {
}
