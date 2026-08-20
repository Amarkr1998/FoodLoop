package com.foodloop.ai.api;

import com.foodloop.ai.agent.matching.MatchingAgent;
import com.foodloop.commons.web.ApiException;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The on-demand trigger from spec §17 / docs/architecture/05 §3 ({@code POST /ai/matching/suggest}). */
@RestController
public class MatchingController {

    private final MatchingAgent matchingAgent;

    public MatchingController(MatchingAgent matchingAgent) {
        this.matchingAgent = matchingAgent;
    }

    @PostMapping("/api/v1/ai/matching/suggest")
    public SuggestMatchResponse suggest(JwtAuthenticationToken authentication, @Valid @RequestBody SuggestMatchRequest request) {
        MatchingAgent.SuggestionResult result = matchingAgent.suggest(tenantId(authentication), request.foodListingId());
        return SuggestMatchResponse.from(result.agentRun(), result.proposal());
    }

    private UUID tenantId(JwtAuthenticationToken authentication) {
        String tenantClaim = authentication.getToken().getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new ApiException("TENANT_NOT_RESOLVED", HttpStatus.BAD_REQUEST, "Request's JWT carried no tenant_id claim.");
        }
        return UUID.fromString(tenantClaim);
    }
}
