package com.foodloop.ai.client;

import java.util.UUID;

/** Mirrors Notification's CreateNotificationRequest — the wire shape of the POST /api/v1/notifications request body. Exactly one of recipientOrgId/recipientUserId must be set. */
public record CreateNotificationPayload(
        UUID recipientOrgId, UUID recipientUserId, String channel, String subject, String body, UUID sourceAgentRunId) {
}
