package com.foodloop.food.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateAiMetadataRequest(
        @NotBlank String category,
        List<String> dietaryTypes,
        List<String> allergens,
        Integer estimatedServings,
        String urgency,
        List<String> missingInformation,
        String suggestedDescription,
        Double confidence) {
}
