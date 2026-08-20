package com.foodloop.ai.tool.food;

import com.foodloop.ai.client.FoodListingDto;
import com.foodloop.ai.client.FoodServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import org.springframework.stereotype.Component;

/**
 * Safety's only write tool (spec §22's {@code createSafetyCase}, docs/architecture/05
 * §5: "Cannot: delete listing, ban user, issue legal guidance"). No
 * separate SafetyCase entity exists yet (that's Trust & Risk's domain,
 * deferred — see AgentPermissionRegistry's Javadoc for the same pattern
 * applied to Rescue's escalation); its concrete effect for this phase is
 * exactly {@code FoodListing#flagForSafetyReview} — a hold the agent can
 * only raise, never clear (see that method's Javadoc).
 */
@Component
public class CreateSafetyCaseTool implements AgentTool<CreateSafetyCaseCommand, FoodListingDto> {

    private final FoodServiceClient foodServiceClient;

    public CreateSafetyCaseTool(FoodServiceClient foodServiceClient) {
        this.foodServiceClient = foodServiceClient;
    }

    @Override
    public String name() {
        return "createSafetyCase";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, CreateSafetyCaseCommand input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(CreateSafetyCaseCommand input) {
        if (input.foodListingId() == null || input.reason() == null || input.reason().isBlank()) {
            throw new IllegalArgumentException("foodListingId and a non-blank reason are required");
        }
    }

    @Override
    public FoodListingDto execute(CreateSafetyCaseCommand input) {
        return foodServiceClient.flagForSafetyReview(TenantContext.get(), input.foodListingId(), input.reason());
    }

    @Override
    public void validateOutput(FoodListingDto output) {
        if (output == null) {
            throw new IllegalStateException("Food service returned no listing after safety-flag update.");
        }
    }
}
