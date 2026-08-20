package com.foodloop.ai.tool.pickup;

import com.foodloop.ai.client.PickupServiceClient;
import com.foodloop.ai.client.PickupTaskDto;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import org.springframework.stereotype.Component;

/**
 * {@code name()} is {@code "updateFoodStatus"} to match §5's permission
 * table entry for Pickup verbatim ("updateFoodStatus (pickup substates
 * only)") — the name is Food-shaped but the action is a pickup sub-state
 * transition, never Food listing content (see AgentPermissionRegistry's
 * "Cannot do: modify food listing content" for this same agent). The only
 * transition wired up so far is Pickup's system-unassign (spec §20:
 * "whether to recommend reassignment") — see
 * PickupService#systemUnassignVolunteer's Javadoc for why it's safe to
 * automate without a human-approval gate.
 */
@Component
public class UpdatePickupStatusTool implements AgentTool<UpdatePickupStatusCommand, PickupTaskDto> {

    private final PickupServiceClient pickupServiceClient;

    public UpdatePickupStatusTool(PickupServiceClient pickupServiceClient) {
        this.pickupServiceClient = pickupServiceClient;
    }

    @Override
    public String name() {
        return "updateFoodStatus";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, UpdatePickupStatusCommand input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(UpdatePickupStatusCommand input) {
        if (input.pickupTaskId() == null) {
            throw new IllegalArgumentException("pickupTaskId must not be null");
        }
    }

    @Override
    public PickupTaskDto execute(UpdatePickupStatusCommand input) {
        return pickupServiceClient.systemUnassign(TenantContext.get(), input.pickupTaskId());
    }

    @Override
    public void validateOutput(PickupTaskDto output) {
        if (output == null || output.id() == null) {
            throw new IllegalStateException("Pickup service returned no task.");
        }
    }
}
