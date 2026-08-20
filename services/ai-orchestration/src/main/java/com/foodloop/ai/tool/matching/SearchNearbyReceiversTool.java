package com.foodloop.ai.tool.matching;

import com.foodloop.ai.client.MatchCandidateDto;
import com.foodloop.ai.client.MatchingServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Read tool granted to Matching (docs/architecture/05 §5, adapted for Phase
 * 7 — see AgentPermissionRegistry's Javadoc). Returns the deterministic
 * MatchingEngine's already-ranked candidate set; the agent's LLM step only
 * re-ranks/explains among these, never invents a candidate outside it.
 */
@Component
public class SearchNearbyReceiversTool implements AgentTool<UUID, List<MatchCandidateDto>> {

    private static final double DEFAULT_RADIUS_KM = 10.0;

    private final MatchingServiceClient matchingServiceClient;

    public SearchNearbyReceiversTool(MatchingServiceClient matchingServiceClient) {
        this.matchingServiceClient = matchingServiceClient;
    }

    @Override
    public String name() {
        return "searchNearbyReceivers";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, UUID input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(UUID input) {
        if (input == null) {
            throw new IllegalArgumentException("foodListingId must not be null");
        }
    }

    @Override
    public List<MatchCandidateDto> execute(UUID input) {
        return matchingServiceClient.getCandidates(TenantContext.get(), input, DEFAULT_RADIUS_KM);
    }

    @Override
    public void validateOutput(List<MatchCandidateDto> output) {
        if (output == null) {
            throw new IllegalStateException("Matching service returned no candidate list.");
        }
    }
}
