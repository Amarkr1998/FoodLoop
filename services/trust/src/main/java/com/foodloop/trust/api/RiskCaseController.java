package com.foodloop.trust.api;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.trust.application.RiskCaseService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RiskCaseController {

    private static final String AI_ORCHESTRATION_CLIENT_ID = "foodloop-ai-orchestration";

    private final RiskCaseService riskCaseService;

    public RiskCaseController(RiskCaseService riskCaseService) {
        this.riskCaseService = riskCaseService;
    }

    /**
     * Written only by the Trust &amp; Risk Agent's createRiskCase tool — same
     * {@code azp} trust check as Food's updateAiMetadata. riskScore/
     * requiresHumanReview are computed server-side (see RiskCaseService),
     * never accepted from the request body.
     */
    @PostMapping("/api/v1/trust/risk-cases")
    public ResponseEntity<RiskCaseResponse> create(
            JwtAuthenticationToken authentication, @Valid @RequestBody CreateRiskCaseRequest request) {
        requireAiOrchestrationCaller(authentication);
        var riskCase = riskCaseService.create(TenantContext.get(), request.targetUserId(), request.riskFactors());
        return ResponseEntity.status(HttpStatus.CREATED).body(RiskCaseResponse.from(riskCase));
    }

    @GetMapping("/api/v1/trust/risk-cases/{id}")
    public RiskCaseResponse get(@PathVariable UUID id) {
        return RiskCaseResponse.from(riskCaseService.get(id));
    }

    @GetMapping("/api/v1/trust/risk-cases")
    public List<RiskCaseResponse> listForUser(@RequestParam UUID targetUserId) {
        return riskCaseService.listForUser(targetUserId).stream().map(RiskCaseResponse::from).toList();
    }

    /** The TRUST_OPS review decision (spec §21, §26) — same realm-role check as Food's clearSafetyReview. */
    @PostMapping("/api/v1/trust/risk-cases/{id}/resolve")
    public RiskCaseResponse resolve(
            JwtAuthenticationToken authentication, @PathVariable UUID id, @Valid @RequestBody ResolveRiskCaseRequest request) {
        requireTrustOpsCaller(authentication);
        var riskCase = riskCaseService.resolve(id, request.resolutionAction(), callerUserId(authentication));
        return RiskCaseResponse.from(riskCase);
    }

    void requireAiOrchestrationCaller(JwtAuthenticationToken authentication) {
        String azp = authentication.getToken().getClaimAsString("azp");
        if (!AI_ORCHESTRATION_CLIENT_ID.equals(azp)) {
            throw new ApiException("FORBIDDEN_RISK_CASE_WRITE", HttpStatus.FORBIDDEN,
                    "Only the AI orchestration service may create a risk case.");
        }
    }

    void requireTrustOpsCaller(JwtAuthenticationToken authentication) {
        Object realmAccess = authentication.getToken().getClaims().get("realm_access");
        List<String> roles = (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof List<?> rawRoles)
                ? rawRoles.stream().map(String::valueOf).toList()
                : List.of();
        if (!roles.contains("TRUST_OPS") && !roles.contains("ADMIN")) {
            throw new ApiException("FORBIDDEN_RISK_CASE_RESOLVE", HttpStatus.FORBIDDEN,
                    "Only TRUST_OPS or ADMIN may resolve a risk case.");
        }
    }

    private UUID callerUserId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getSubject());
    }
}
