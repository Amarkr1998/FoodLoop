package com.foodloop.ai.api;

import com.foodloop.ai.agent.foodintelligence.FoodIntelligenceAgent;
import com.foodloop.commons.web.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The donor-initiated trigger from spec §16 / docs/architecture/05 §3
 * ({@code POST /ai/food-listings/{id}/analyze}), reached through the
 * gateway's {@code /api/v1/ai/**} route. The other documented trigger — an
 * async background pass on {@code food.listed.v1} — isn't built in this
 * phase; nothing here depends on it existing.
 */
@RestController
public class FoodIntelligenceController {

    private final FoodIntelligenceAgent foodIntelligenceAgent;

    public FoodIntelligenceController(FoodIntelligenceAgent foodIntelligenceAgent) {
        this.foodIntelligenceAgent = foodIntelligenceAgent;
    }

    @PostMapping("/api/v1/ai/food-listings/{id}/analyze")
    public AnalyzeFoodListingResponse analyze(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        UUID tenantId = tenantId(authentication);
        FoodIntelligenceAgent.AnalysisResult result = foodIntelligenceAgent.analyze(tenantId, id);
        return AnalyzeFoodListingResponse.from(result.agentRun(), result.analysis());
    }

    private UUID tenantId(JwtAuthenticationToken authentication) {
        String tenantClaim = authentication.getToken().getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new ApiException("TENANT_NOT_RESOLVED", HttpStatus.BAD_REQUEST, "Request's JWT carried no tenant_id claim.");
        }
        return UUID.fromString(tenantClaim);
    }
}
