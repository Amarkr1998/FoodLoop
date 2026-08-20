package com.foodloop.ai.tool.rescue;

import com.foodloop.ai.client.NotificationDto;
import com.foodloop.ai.client.NotificationServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import org.springframework.stereotype.Component;

/**
 * Rescue's write tool for alerting a candidate receiver (spec §18). Queues a
 * real notification record (see services/notification's pom.xml Javadoc for
 * why "sent" means "queued" in this phase, not "delivered") — never a fake
 * "notification sent" claim with nothing behind it.
 */
@Component
public class SendNotificationTool implements AgentTool<SendNotificationCommand, NotificationDto> {

    private final NotificationServiceClient notificationServiceClient;

    public SendNotificationTool(NotificationServiceClient notificationServiceClient) {
        this.notificationServiceClient = notificationServiceClient;
    }

    @Override
    public String name() {
        return "sendNotification";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, SendNotificationCommand input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(SendNotificationCommand input) {
        if (input.recipientOrgId() == null || input.channel() == null || input.subject() == null || input.body() == null) {
            throw new IllegalArgumentException("recipientOrgId, channel, subject, and body are required");
        }
    }

    @Override
    public NotificationDto execute(SendNotificationCommand input) {
        return notificationServiceClient.queue(
                TenantContext.get(), input.recipientOrgId(), input.channel(), input.subject(), input.body(),
                input.sourceAgentRunId());
    }

    @Override
    public void validateOutput(NotificationDto output) {
        if (output == null || output.id() == null) {
            throw new IllegalStateException("Notification service returned no notification.");
        }
    }
}
