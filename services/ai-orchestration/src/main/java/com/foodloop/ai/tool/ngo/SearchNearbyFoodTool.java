package com.foodloop.ai.tool.ngo;

import com.foodloop.ai.client.FoodSearchResultDto;
import com.foodloop.ai.client.FoodServiceClient;
import com.foodloop.ai.tool.AgentCallerContext;
import com.foodloop.ai.tool.AgentTool;
import com.foodloop.ai.tool.AuthorizationResult;
import com.foodloop.commons.tenant.TenantContext;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Read tool granted to NGO Coordination (spec §19, §25) — Food's own public
 * geo search, already filtered to AVAILABLE listings server-side, same
 * endpoint a receiver's app would call. checkFoodEligibility (also named in
 * §5's tool list) isn't a separate tool here: NgoCoordinationAgent's own
 * candidate selection re-checks status/expiry the same way Matching/Rescue's
 * deterministic code does, and createMatchProposal re-validates eligibility
 * server-side regardless (tool-side validation, not prompt trust).
 */
@Component
public class SearchNearbyFoodTool implements AgentTool<SearchNearbyFoodInput, List<FoodSearchResultDto>> {

    private final FoodServiceClient foodServiceClient;

    public SearchNearbyFoodTool(FoodServiceClient foodServiceClient) {
        this.foodServiceClient = foodServiceClient;
    }

    @Override
    public String name() {
        return "searchNearbyFood";
    }

    @Override
    public AuthorizationResult authorize(AgentCallerContext caller, SearchNearbyFoodInput input) {
        return AuthorizationResult.allow();
    }

    @Override
    public void validateInput(SearchNearbyFoodInput input) {
        if (input == null || input.category() == null || input.category().isBlank()) {
            throw new IllegalArgumentException("category must not be null or blank");
        }
    }

    @Override
    public List<FoodSearchResultDto> execute(SearchNearbyFoodInput input) {
        return foodServiceClient.searchNearby(TenantContext.get(), input.lat(), input.lng(), input.radiusKm(), input.category());
    }

    @Override
    public void validateOutput(List<FoodSearchResultDto> output) {
        if (output == null) {
            throw new IllegalStateException("Food service returned no search result list.");
        }
    }
}
