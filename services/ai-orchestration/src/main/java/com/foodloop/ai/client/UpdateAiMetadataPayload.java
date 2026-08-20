package com.foodloop.ai.client;

import java.util.List;

/** Mirrors Food's {@code UpdateAiMetadataRequest} exactly — the wire shape of the PUT ai-metadata request body. */
public record UpdateAiMetadataPayload(
        String category,
        List<String> dietaryTypes,
        List<String> allergens,
        Integer estimatedServings,
        String urgency,
        List<String> missingInformation,
        String suggestedDescription,
        Double confidence) {
}
