package com.foodloop.ai.tool.pickup;

import com.foodloop.ai.client.NotificationDto;
import com.foodloop.ai.client.NotificationServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import org.springframework.stereotype.Component;

/**
 * Pickup's write tool for alerting a specific volunteer (spec §20) — same
 * declared tool name ("sendNotification") as Rescue's SendNotificationTool
 * but a distinct bean, since Rescue notifies a receiver org and Pickup
 * notifies a volunteer user; see Notification's V2 migration Javadoc for why
 * those need different underlying calls. Queues a real notification record
 * (see services/notification's pom.xml Javadoc for why "sent" means "queued"
 * in this phase, not "delivered").
 */
@Component
public class NotifyVolunteerTool implements AgentTool<NotifyVolunteerCommand, NotificationDto> {

    private final NotificationServiceClient notificationServiceClient;

    public NotifyVolunteerTool(NotificationServiceClient notificationServiceClient) {
        this.notificationServiceClient = notificationServiceClient;
    }

    @Override
    public String name() {
        return "sendNotification";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, NotifyVolunteerCommand input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(NotifyVolunteerCommand input) {
        if (input.recipientUserId() == null || input.channel() == null || input.subject() == null || input.body() == null) {
            throw new IllegalArgumentException("recipientUserId, channel, subject, and body are required");
        }
    }

    @Override
    public NotificationDto execute(NotifyVolunteerCommand input) {
        return notificationServiceClient.queueForUser(
                TenantContext.get(), input.recipientUserId(), input.channel(), input.subject(), input.body(),
                input.sourceAgentRunId());
    }

    @Override
    public void validateOutput(NotificationDto output) {
        if (output == null || output.id() == null) {
            throw new IllegalStateException("Notification service returned no notification.");
        }
    }
}
