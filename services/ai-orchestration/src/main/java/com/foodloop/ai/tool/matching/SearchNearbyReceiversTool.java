package com.foodloop.ai.tool.matching;

import com.foodloop.ai.client.MatchCandidateDto;
import com.foodloop.ai.client.MatchingServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Read tool granted to Matching and Rescue (docs/architecture/05 §5, adapted
 * for Phase 7/8 — see AgentPermissionRegistry's Javadoc). Returns the
 * deterministic MatchingEngine's already-ranked candidate set; an agent's
 * reasoning step only re-ranks/explains among these, never invents a
 * candidate outside it. {@code radiusKm} is caller-supplied (nullable) so
 * Rescue can expand its search radius at a later expiry threshold without a
 * second copy of this tool.
 */
@Component
public class SearchNearbyReceiversTool implements AgentTool<SearchNearbyReceiversInput, List<MatchCandidateDto>> {

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
    public AuthorizationResult authorize(AgentCallerContext caller, SearchNearbyReceiversInput input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(SearchNearbyReceiversInput input) {
        if (input.foodListingId() == null) {
            throw new IllegalArgumentException("foodListingId must not be null");
        }
    }

    @Override
    public List<MatchCandidateDto> execute(SearchNearbyReceiversInput input) {
        double radiusKm = input.radiusKm() != null ? input.radiusKm() : DEFAULT_RADIUS_KM;
        return matchingServiceClient.getCandidates(TenantContext.get(), input.foodListingId(), radiusKm);
    }

    @Override
    public void validateOutput(List<MatchCandidateDto> output) {
        if (output == null) {
            throw new IllegalStateException("Matching service returned no candidate list.");
        }
    }
}
