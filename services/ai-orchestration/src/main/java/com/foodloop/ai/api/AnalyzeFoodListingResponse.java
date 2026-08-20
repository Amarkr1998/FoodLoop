package com.foodloop.ai.api;

import com.foodloop.ai.agent.foodintelligence.FoodIntelligenceOutput;
import com.foodloop.ai.agent.safety.SafetyAgent;
import com.foodloop.ai.domain.AgentRun;
import java.util.UUID;

public record AnalyzeFoodListingResponse(
        UUID agentRunId,
        String status,
        boolean escalated,
        String outcomeSummary,
        FoodIntelligenceOutput analysis,
        SafetyView safety) {

    /** The Safety Agent's outcome for the same listing (spec §22) — runs alongside Food Intelligence, not a separate trigger. */
    public record SafetyView(UUID agentRunId, String status, boolean flagged) {

        static SafetyView from(SafetyAgent.SafetyResult result) {
            AgentRun agentRun = result.agentRun();
            return new SafetyView(agentRun.getId(), agentRun.getStatus().name(), result.flagged());
        }
    }

    public static AnalyzeFoodListingResponse from(
            AgentRun agentRun, FoodIntelligenceOutput analysis, SafetyAgent.SafetyResult safetyResult) {
        return new AnalyzeFoodListingResponse(
                agentRun.getId(),
                agentRun.getStatus().name(),
                agentRun.isEscalated(),
                agentRun.getOutcomeSummary(),
                agentRun.isEscalated() ? null : analysis,
                SafetyView.from(safetyResult));
    }
}
