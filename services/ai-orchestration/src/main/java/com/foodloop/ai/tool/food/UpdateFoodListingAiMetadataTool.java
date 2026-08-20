package com.foodloop.ai.tool.food;

import com.foodloop.ai.client.FoodListingDto;
import com.foodloop.ai.client.FoodServiceClient;
import com.foodloop.ai.client.UpdateAiMetadataPayload;
import com.foodloop.ai.agent.foodintelligence.FoodIntelligenceOutput;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import org.springframework.stereotype.Component;

/**
 * The Food Intelligence Agent's only write tool (docs/architecture/05 §5) —
 * it cannot publish a listing or change its status, only record suggestions
 * (see Food's {@code FoodListing.recordAiMetadata}, which enforces DRAFT-only
 * and never touches the donor-controlled fields).
 */
@Component
public class UpdateFoodListingAiMetadataTool implements AgentTool<UpdateFoodListingAiMetadataCommand, FoodListingDto> {

    private final FoodServiceClient foodServiceClient;

    public UpdateFoodListingAiMetadataTool(FoodServiceClient foodServiceClient) {
        this.foodServiceClient = foodServiceClient;
    }

    @Override
    public String name() {
        return "updateFoodListingAiMetadata";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, UpdateFoodListingAiMetadataCommand input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(UpdateFoodListingAiMetadataCommand input) {
        if (input.listingId() == null) {
            throw new IllegalArgumentException("listingId must not be null");
        }
        FoodIntelligenceOutput analysis = input.analysis();
        if (analysis == null || analysis.category() == null || analysis.category().isBlank()) {
            throw new IllegalArgumentException("analysis.category is required");
        }
    }

    @Override
    public FoodListingDto execute(UpdateFoodListingAiMetadataCommand input) {
        FoodIntelligenceOutput analysis = input.analysis();
        UpdateAiMetadataPayload payload = new UpdateAiMetadataPayload(
                analysis.category(), analysis.dietaryTypes(), analysis.allergens(), analysis.estimatedServings(),
                analysis.urgency(), analysis.missingInformation(), analysis.suggestedDescription(),
                analysis.confidence());
        return foodServiceClient.updateAiMetadata(TenantContext.get(), input.listingId(), payload);
    }

    @Override
    public void validateOutput(FoodListingDto output) {
        if (output == null) {
            throw new IllegalStateException("Food service returned no listing after AI metadata update.");
        }
    }
}
