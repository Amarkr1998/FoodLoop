package com.foodloop.notification.api;

import com.foodloop.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Exactly one of recipientOrgId/recipientUserId must be set — validated in NotificationController, not via bean validation (no built-in "exactly one of two fields" annotation). */
public record CreateNotificationRequest(
        UUID recipientOrgId,
        UUID recipientUserId,
        @NotNull NotificationChannel channel,
        @NotBlank String subject,
        @NotBlank String body,
        UUID sourceAgentRunId) {
}
