package com.foodloop.ai.agent.foodintelligence;

import com.foodloop.ai.client.FoodListingDto;
import java.util.UUID;

/**
 * State threaded through {@link FoodIntelligenceAgent}'s
 * {@link com.foodloop.ai.graph.AgentGraph} run. Records are immutable, so
 * each node returns a new instance via one of the {@code with*} methods
 * rather than mutating in place — keeps every step's transformation
 * explicit and the whole run replayable from its inputs.
 */
record FoodIntelligenceState(
        UUID listingId,
        FoodListingDto listing,
        String providerName,
        String modelName,
        String rawModelOutput,
        FoodIntelligenceOutput analysis,
        String escalationReason,
        int retryCount) {

    static FoodIntelligenceState initial(UUID listingId) {
        return new FoodIntelligenceState(listingId, null, null, null, null, null, null, 0);
    }

    FoodIntelligenceState withListing(FoodListingDto listing) {
        return new FoodIntelligenceState(
                listingId, listing, providerName, modelName, rawModelOutput, analysis, escalationReason, retryCount);
    }

    FoodIntelligenceState withModelOutput(String providerName, String modelName, String rawModelOutput) {
        return new FoodIntelligenceState(
                listingId, listing, providerName, modelName, rawModelOutput, analysis, escalationReason, retryCount);
    }

    FoodIntelligenceState withAnalysis(FoodIntelligenceOutput analysis) {
        return new FoodIntelligenceState(
                listingId, listing, providerName, modelName, rawModelOutput, analysis, null, retryCount);
    }

    /** Validation failed: clears any partial analysis, records why, and counts the attempt toward the retry budget. */
    FoodIntelligenceState withValidationFailure(String reason) {
        return new FoodIntelligenceState(
                listingId, listing, providerName, modelName, rawModelOutput, null, reason, retryCount + 1);
    }
}
