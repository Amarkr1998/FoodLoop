package com.foodloop.ai.tool.pickup;

import com.foodloop.ai.client.PickupServiceClient;
import com.foodloop.ai.client.VolunteerProfileDto;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import java.util.List;
import org.springframework.stereotype.Component;

/** Read tool granted to Pickup (spec §20, §25) — available volunteers near a task's own pickup location. */
@Component
public class FindAvailableVolunteersTool implements AgentTool<FindAvailableVolunteersInput, List<VolunteerProfileDto>> {

    private final PickupServiceClient pickupServiceClient;

    public FindAvailableVolunteersTool(PickupServiceClient pickupServiceClient) {
        this.pickupServiceClient = pickupServiceClient;
    }

    @Override
    public String name() {
        return "findAvailableVolunteers";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, FindAvailableVolunteersInput input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(FindAvailableVolunteersInput input) {
        if (input.pickupTaskId() == null) {
            throw new IllegalArgumentException("pickupTaskId must not be null");
        }
    }

    @Override
    public List<VolunteerProfileDto> execute(FindAvailableVolunteersInput input) {
        return pickupServiceClient.findNearbyVolunteers(TenantContext.get(), input.pickupTaskId(), input.radiusKm());
    }

    @Override
    public void validateOutput(List<VolunteerProfileDto> output) {
        if (output == null) {
            throw new IllegalStateException("Pickup service returned no volunteer list.");
        }
    }
}
