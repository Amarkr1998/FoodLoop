package com.foodloop.notification.api;

import com.foodloop.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateNotificationRequest(
        @NotNull UUID recipientOrgId,
        @NotNull NotificationChannel channel,
        @NotBlank String subject,
        @NotBlank String body,
        UUID sourceAgentRunId) {
}
