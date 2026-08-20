package com.foodloop.ai.agent.safety;

import com.foodloop.ai.client.FoodListingDto;
import java.util.UUID;

record SafetyState(
        UUID listingId,
        FoodListingDto listing,
        String providerName,
        String modelName,
        String rawModelOutput,
        SafetyOutput output,
        String escalationReason,
        int retryCount,
        boolean flagged) {

    static SafetyState initial(UUID listingId) {
        return new SafetyState(listingId, null, null, null, null, null, null, 0, false);
    }

    SafetyState withListing(FoodListingDto listing) {
        return new SafetyState(listingId, listing, providerName, modelName, rawModelOutput, output, escalationReason, retryCount, flagged);
    }

    SafetyState withModelOutput(String providerName, String modelName, String rawModelOutput) {
        return new SafetyState(listingId, listing, providerName, modelName, rawModelOutput, output, escalationReason, retryCount, flagged);
    }

    SafetyState withOutput(SafetyOutput output) {
        return new SafetyState(listingId, listing, providerName, modelName, rawModelOutput, output, null, retryCount, flagged);
    }

    SafetyState withValidationFailure(String reason) {
        return new SafetyState(listingId, listing, providerName, modelName, rawModelOutput, null, reason, retryCount + 1, flagged);
    }

    SafetyState withFlagged() {
        return new SafetyState(listingId, listing, providerName, modelName, rawModelOutput, output, escalationReason, retryCount, true);
    }
}
