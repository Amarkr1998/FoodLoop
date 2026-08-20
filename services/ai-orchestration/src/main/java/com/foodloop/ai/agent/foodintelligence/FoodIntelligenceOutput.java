package com.foodloop.ai.agent.foodintelligence;

import java.util.List;

/**
 * The Food Intelligence Agent's structured output contract (spec §16,
 * docs/architecture/05-ai-agent-architecture.md §3) — what the model is
 * prompted to return as JSON, validated by
 * {@link com.foodloop.ai.guardrail.StructuredOutputValidator} before this
 * class is ever constructed. Field names are deliberately identical to
 * Food's {@code UpdateAiMetadataRequest} (mirrored as
 * {@link com.foodloop.ai.client.UpdateAiMetadataPayload}) — this is the one
 * shape the whole pipeline agrees on.
 */
public record FoodIntelligenceOutput(
        String category,
        List<String> dietaryTypes,
        List<String> allergens,
        Integer estimatedServings,
        String urgency,
        List<String> missingInformation,
        String suggestedDescription,
        Double confidence) {
}
