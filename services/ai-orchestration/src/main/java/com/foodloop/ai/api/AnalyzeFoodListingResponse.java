package com.foodloop.ai.api;

import com.foodloop.ai.agent.foodintelligence.FoodIntelligenceOutput;
import com.foodloop.ai.domain.AgentRun;
import java.util.UUID;

public record AnalyzeFoodListingResponse(
        UUID agentRunId,
        String status,
        boolean escalated,
        String outcomeSummary,
        FoodIntelligenceOutput analysis) {

    public static AnalyzeFoodListingResponse from(AgentRun agentRun, FoodIntelligenceOutput analysis) {
        return new AnalyzeFoodListingResponse(
                agentRun.getId(),
                agentRun.getStatus().name(),
                agentRun.isEscalated(),
                agentRun.getOutcomeSummary(),
                agentRun.isEscalated() ? null : analysis);
    }
}
