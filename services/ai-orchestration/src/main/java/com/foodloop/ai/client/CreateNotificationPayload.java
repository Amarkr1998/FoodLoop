package com.foodloop.ai.client;

import java.util.UUID;

/** Mirrors Notification's CreateNotificationRequest — the wire shape of the POST /api/v1/notifications request body. */
public record CreateNotificationPayload(UUID recipientOrgId, String channel, String subject, String body, UUID sourceAgentRunId) {
}
