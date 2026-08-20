package com.foodloop.ai.tool.pickup;

import com.foodloop.ai.client.PickupServiceClient;
import com.foodloop.ai.client.PickupTaskDto;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Read tool granted to Pickup — not named in §5's summary table, but fills
 * the same "re-fetch the trigger's referenced entity before acting on it"
 * role getFoodListing plays for Food Intelligence/Matching/Rescue (see
 * those tools' Javadoc, and NgoCoordinationAgent's getNGORequest for the
 * same precedent applied to a different agent).
 */
@Component
public class GetPickupTaskTool implements AgentTool<UUID, PickupTaskDto> {

    private final PickupServiceClient pickupServiceClient;

    public GetPickupTaskTool(PickupServiceClient pickupServiceClient) {
        this.pickupServiceClient = pickupServiceClient;
    }

    @Override
    public String name() {
        return "getPickupTask";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, UUID input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(UUID input) {
        if (input == null) {
            throw new IllegalArgumentException("pickupTaskId must not be null");
        }
    }

    @Override
    public PickupTaskDto execute(UUID input) {
        return pickupServiceClient.getTask(TenantContext.get(), input);
    }

    @Override
    public void validateOutput(PickupTaskDto output) {
        if (output == null || output.id() == null) {
            throw new IllegalStateException("Pickup service returned no task.");
        }
    }
}
