package com.foodloop.ai.api;

import com.foodloop.ai.domain.AgentRun;
import java.time.Instant;
import java.util.UUID;

public record AgentRunResponse(UUID id, String agentName, String status, boolean escalated, String outcomeSummary, Instant completedAt) {

    public static AgentRunResponse from(AgentRun agentRun) {
        return new AgentRunResponse(
                agentRun.getId(), agentRun.getAgentName(), agentRun.getStatus().name(), agentRun.isEscalated(),
                agentRun.getOutcomeSummary(), agentRun.getCompletedAt());
    }
}
