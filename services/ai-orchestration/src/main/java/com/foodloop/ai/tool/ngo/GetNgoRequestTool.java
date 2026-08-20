package com.foodloop.ai.tool.ngo;

import com.foodloop.ai.client.NgoRequestDto;
import com.foodloop.ai.client.NgoServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Read tool granted to NGO Coordination — not named in §5's summary table
 * (which lists getNGORequirements/searchNearbyFood/checkFoodEligibility as
 * its read tools), but every other trigger-driven agent
 * (Food Intelligence/Matching/Rescue) re-fetches the entity its trigger
 * references before acting on it via an equivalent tool
 * (getFoodListing) rather than trusting the triggering event's payload as
 * current truth, since the request's status may have changed (matched,
 * cancelled) between the trigger firing and this agent run executing. This
 * fills that same role for NgoCoordinationAgent, consistent with that
 * pattern rather than a deviation from it.
 */
@Component
public class GetNgoRequestTool implements AgentTool<UUID, NgoRequestDto> {

    private final NgoServiceClient ngoServiceClient;

    public GetNgoRequestTool(NgoServiceClient ngoServiceClient) {
        this.ngoServiceClient = ngoServiceClient;
    }

    @Override
    public String name() {
        return "getNGORequest";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, UUID input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(UUID input) {
        if (input == null) {
            throw new IllegalArgumentException("ngoRequestId must not be null");
        }
    }

    @Override
    public NgoRequestDto execute(UUID input) {
        return ngoServiceClient.getRequest(TenantContext.get(), input);
    }

    @Override
    public void validateOutput(NgoRequestDto output) {
        if (output == null || output.id() == null) {
            throw new IllegalStateException("NGO service returned no request.");
        }
    }
}
