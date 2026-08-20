package com.foodloop.notification.api;

import com.foodloop.notification.domain.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID tenantId,
        UUID recipientOrgId,
        UUID recipientUserId,
        String channel,
        String subject,
        String body,
        String status,
        UUID sourceAgentRunId,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTenantId(),
                notification.getRecipientOrgId(),
                notification.getRecipientUserId(),
                notification.getChannel().name(),
                notification.getSubject(),
                notification.getBody(),
                notification.getStatus().name(),
                notification.getSourceAgentRunId(),
                notification.getCreatedAt());
    }
}
