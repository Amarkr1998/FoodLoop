package com.foodloop.trust.api;

import com.foodloop.commons.web.ApiException;
import com.foodloop.trust.application.ReportService;
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
public class ReportController {

    private static final String AI_ORCHESTRATION_CLIENT_ID = "foodloop-ai-orchestration";

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/api/v1/trust/reports")
    public ResponseEntity<ReportResponse> create(
            JwtAuthenticationToken authentication, @Valid @RequestBody CreateReportRequest request) {
        var report = reportService.create(
                com.foodloop.commons.tenant.TenantContext.get(), callerUserId(authentication), request.targetUserId(),
                request.reason(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(report));
    }

    /** The Trust & Risk Agent's getReportHistory tool (spec §21), also usable by a TRUST_OPS reviewer. */
    @GetMapping("/api/v1/trust/reports")
    public List<ReportResponse> listForUser(JwtAuthenticationToken authentication, @RequestParam UUID targetUserId) {
        requireAiOrchestrationOrTrustOpsCaller(authentication);
        return reportService.listForUser(targetUserId).stream().map(ReportResponse::from).toList();
    }

    /** The Trust & Risk Agent's getUserBehaviorSignals tool (spec §21). */
    @GetMapping("/api/v1/trust/signals/{targetUserId}")
    public UserBehaviorSignalResponse getSignals(JwtAuthenticationToken authentication, @PathVariable UUID targetUserId) {
        requireAiOrchestrationOrTrustOpsCaller(authentication);
        return UserBehaviorSignalResponse.from(reportService.getSignals(targetUserId));
    }

    // Package-private (not private) so a dedicated authorization test can exercise this directly.
    void requireAiOrchestrationOrTrustOpsCaller(JwtAuthenticationToken authentication) {
        String azp = authentication.getToken().getClaimAsString("azp");
        if (AI_ORCHESTRATION_CLIENT_ID.equals(azp)) {
            return;
        }
        Object realmAccess = authentication.getToken().getClaims().get("realm_access");
        List<String> roles = (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof List<?> rawRoles)
                ? rawRoles.stream().map(String::valueOf).toList()
                : List.of();
        if (!roles.contains("TRUST_OPS") && !roles.contains("ADMIN")) {
            throw new ApiException("FORBIDDEN_SIGNAL_READ", HttpStatus.FORBIDDEN,
                    "Only the AI orchestration service or TRUST_OPS/ADMIN may read another user's report history or behavior signals.");
        }
    }

    private UUID callerUserId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getSubject());
    }
}
