package com.foodloop.ai.tool.rescue;

import java.util.UUID;

public record SendNotificationCommand(UUID recipientOrgId, String channel, String subject, String body, UUID sourceAgentRunId) {
}
