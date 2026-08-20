package com.foodloop.ai.tool.food;

import com.foodloop.ai.agent.foodintelligence.FoodIntelligenceOutput;
import java.util.UUID;

public record UpdateFoodListingAiMetadataCommand(UUID listingId, FoodIntelligenceOutput analysis) {
}
