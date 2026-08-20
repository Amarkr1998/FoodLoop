package com.foodloop.ai.api;

import com.foodloop.ai.client.MatchProposalDto;
import com.foodloop.ai.domain.AgentRun;
import java.util.UUID;

public record SuggestMatchResponse(
        UUID agentRunId, String status, boolean escalated, String outcomeSummary, MatchProposalDto proposal) {

    public static SuggestMatchResponse from(AgentRun agentRun, MatchProposalDto proposal) {
        return new SuggestMatchResponse(
                agentRun.getId(), agentRun.getStatus().name(), agentRun.isEscalated(), agentRun.getOutcomeSummary(), proposal);
    }
}
