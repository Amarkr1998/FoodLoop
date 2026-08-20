package com.foodloop.ai.tool.food;

import com.foodloop.ai.client.FoodListingDto;
import com.foodloop.ai.client.FoodServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Read tool granted to Food Intelligence, Rescue, and Safety
 * (docs/architecture/05-ai-agent-architecture.md §5) — every caller goes
 * through {@link com.foodloop.ai.tool.ToolExecutor}, which checks the
 * permission matrix before this ever runs, so {@link #authorize} itself has
 * nothing further to check for a plain read.
 */
@Component
public class GetFoodListingTool implements AgentTool<UUID, FoodListingDto> {

    private final FoodServiceClient foodServiceClient;

    public GetFoodListingTool(FoodServiceClient foodServiceClient) {
        this.foodServiceClient = foodServiceClient;
    }

    @Override
    public String name() {
        return "getFoodListing";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, UUID input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(UUID input) {
        if (input == null) {
            throw new IllegalArgumentException("listingId must not be null");
        }
    }

    @Override
    public FoodListingDto execute(UUID input) {
        return foodServiceClient.getFoodListing(TenantContext.get(), input);
    }

    @Override
    public void validateOutput(FoodListingDto output) {
        if (output == null || output.id() == null) {
            throw new IllegalStateException("Food service returned no listing.");
        }
    }
}
