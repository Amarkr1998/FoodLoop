package com.foodloop.ai.api;

import com.foodloop.ai.agent.trust.TrustRiskAgent;
import com.foodloop.commons.web.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The on-demand trigger for the Trust & Risk Agent (spec §21) — TRUST_OPS/ADMIN-gated, unlike Matching/Rescue's donor-facing triggers, since it assesses an arbitrary user, not the caller's own listing. */
@RestController
public class TrustController {

    private final TrustRiskAgent trustRiskAgent;

    public TrustController(TrustRiskAgent trustRiskAgent) {
        this.trustRiskAgent = trustRiskAgent;
    }

    @PostMapping("/api/v1/ai/trust/assess")
    public AssessTrustRiskResponse assess(JwtAuthenticationToken authentication, @Valid @RequestBody AssessTrustRiskRequest request) {
        requireTrustOpsCaller(authentication);
        var result = trustRiskAgent.assess(tenantId(authentication), request.targetUserId());
        return AssessTrustRiskResponse.from(result.agentRun());
    }

    void requireTrustOpsCaller(JwtAuthenticationToken authentication) {
        Object realmAccess = authentication.getToken().getClaims().get("realm_access");
        List<String> roles = (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof List<?> rawRoles)
                ? rawRoles.stream().map(String::valueOf).toList()
                : List.of();
        if (!roles.contains("TRUST_OPS") && !roles.contains("ADMIN")) {
            throw new ApiException("FORBIDDEN_TRUST_ASSESS", HttpStatus.FORBIDDEN,
                    "Only TRUST_OPS or ADMIN may trigger a Trust & Risk assessment.");
        }
    }

    private UUID tenantId(JwtAuthenticationToken authentication) {
        String tenantClaim = authentication.getToken().getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new ApiException("TENANT_NOT_RESOLVED", HttpStatus.BAD_REQUEST, "Request's JWT carried no tenant_id claim.");
        }
        return UUID.fromString(tenantClaim);
    }
}
