package com.foodloop.ai.api;

import com.foodloop.ai.domain.AgentRun;
import java.util.UUID;

public record RescueCheckResponse(UUID agentRunId, String status, boolean escalated, String outcomeSummary) {

    public static RescueCheckResponse from(AgentRun agentRun) {
        return new RescueCheckResponse(agentRun.getId(), agentRun.getStatus().name(), agentRun.isEscalated(), agentRun.getOutcomeSummary());
    }
}
