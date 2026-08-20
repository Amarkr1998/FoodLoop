package com.foodloop.ai.api;

import com.foodloop.ai.domain.AgentRun;
import java.util.UUID;

public record AssessTrustRiskResponse(UUID agentRunId, String status, boolean escalated, String outcomeSummary) {

    public static AssessTrustRiskResponse from(AgentRun agentRun) {
        return new AssessTrustRiskResponse(
                agentRun.getId(), agentRun.getStatus().name(), agentRun.isEscalated(), agentRun.getOutcomeSummary());
    }
}
