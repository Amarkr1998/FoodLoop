package com.foodloop.food.domain;

import java.time.Instant;
import java.util.List;

/**
 * The Food Intelligence Agent's suggestions for one listing (spec §16),
 * persisted verbatim into {@code food_listing.ai_metadata} (JSONB, added in
 * V1 ahead of this phase specifically for this). Advisory only — nothing on
 * {@link FoodListing} besides {@link FoodListing#recordAiMetadata} ever reads
 * or writes this; the donor's own fields (category, dietaryTypes, ...) are
 * never overwritten by it.
 */
public record FoodAiMetadata(
        String category,
        List<String> dietaryTypes,
        List<String> allergens,
        Integer estimatedServings,
        String urgency,
        List<String> missingInformation,
        String suggestedDescription,
        Double confidence,
        Instant analyzedAt) {
}
