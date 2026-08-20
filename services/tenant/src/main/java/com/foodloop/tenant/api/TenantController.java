package com.foodloop.tenant.api;

import com.foodloop.commons.web.ApiException;
import com.foodloop.tenant.application.TenantService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant rows have no RLS policy (see Tenant's own Javadoc) — listing them
 * all is inherently a platform-level, not a tenant-scoped, read, so it's
 * gated by caller identity instead: only the Rescue Agent's scheduled
 * cross-tenant expiry sweep (Phase 8) needs this today, so only its service
 * account is trusted, the same {@code azp}-based pattern
 * {@link com.foodloop.commons.tenant.TenantFilter} and Food's ai-metadata
 * endpoint already use.
 */
@RestController
public class TenantController {

    private static final String AI_ORCHESTRATION_CLIENT_ID = "foodloop-ai-orchestration";

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/api/v1/tenants")
    public List<TenantResponse> listActive(JwtAuthenticationToken authentication) {
        requireAiOrchestrationCaller(authentication);
        return tenantService.listActiveTenants().stream().map(TenantResponse::from).toList();
    }

    void requireAiOrchestrationCaller(JwtAuthenticationToken authentication) {
        String azp = authentication.getToken().getClaimAsString("azp");
        if (!AI_ORCHESTRATION_CLIENT_ID.equals(azp)) {
            throw new ApiException("FORBIDDEN_TENANT_LIST", HttpStatus.FORBIDDEN,
                    "Only the AI orchestration service may list tenants.");
        }
    }
}
