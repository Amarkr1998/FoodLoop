package com.foodloop.ai.tool.ngo;

import com.foodloop.ai.client.NgoRequirementDto;
import com.foodloop.ai.client.NgoServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Read tool granted to NGO Coordination (spec §19, §25). Returns null (not
 * an error) when the NGO hasn't registered requirements yet — the agent
 * still has the request's own category/quantity to work from.
 */
@Component
public class GetNgoRequirementsTool implements AgentTool<UUID, NgoRequirementDto> {

    private final NgoServiceClient ngoServiceClient;

    public GetNgoRequirementsTool(NgoServiceClient ngoServiceClient) {
        this.ngoServiceClient = ngoServiceClient;
    }

    @Override
    public String name() {
        return "getNGORequirements";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, UUID input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(UUID input) {
        if (input == null) {
            throw new IllegalArgumentException("ngoOrgId must not be null");
        }
    }

    @Override
    public NgoRequirementDto execute(UUID input) {
        return ngoServiceClient.getRequirements(TenantContext.get(), input);
    }

    @Override
    public void validateOutput(NgoRequirementDto output) {
        // Null is a valid, expected outcome — see class Javadoc.
    }
}
