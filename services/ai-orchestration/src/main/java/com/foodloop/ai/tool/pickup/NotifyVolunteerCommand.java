package com.foodloop.ai.tool.pickup;

import java.util.UUID;

public record NotifyVolunteerCommand(UUID recipientUserId, String channel, String subject, String body, UUID sourceAgentRunId) {
}
