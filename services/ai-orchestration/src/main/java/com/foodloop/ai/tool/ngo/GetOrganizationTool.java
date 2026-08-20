package com.foodloop.ai.tool.ngo;

import com.foodloop.ai.client.OrganizationDto;
import com.foodloop.ai.client.TenantServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Read tool granted to NGO Coordination — the NGO org's own current
 * location, the center point searchNearbyFood needs. Not named in §5's
 * summary table (which predates this org-location lookup being a distinct
 * step), but NGO's own bounded context deliberately makes zero outbound
 * calls (see services/ngo's pom.xml), so this can't be resolved from
 * getNGORequirements/getNGORequest — Organization &amp; Tenant is the only
 * context that owns it.
 */
@Component
public class GetOrganizationTool implements AgentTool<UUID, OrganizationDto> {

    private final TenantServiceClient tenantServiceClient;

    public GetOrganizationTool(TenantServiceClient tenantServiceClient) {
        this.tenantServiceClient = tenantServiceClient;
    }

    @Override
    public String name() {
        return "getOrganization";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, UUID input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(UUID input) {
        if (input == null) {
            throw new IllegalArgumentException("organizationId must not be null");
        }
    }

    @Override
    public OrganizationDto execute(UUID input) {
        return tenantServiceClient.getOrganization(TenantContext.get(), input);
    }

    @Override
    public void validateOutput(OrganizationDto output) {
        if (output == null || output.id() == null) {
            throw new IllegalStateException("Tenant service returned no organization.");
        }
    }
}
