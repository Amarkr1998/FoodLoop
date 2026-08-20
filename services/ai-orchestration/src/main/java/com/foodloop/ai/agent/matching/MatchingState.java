package com.foodloop.ai.agent.matching;

import com.foodloop.ai.client.FoodListingDto;
import com.foodloop.ai.client.MatchCandidateDto;
import com.foodloop.ai.client.MatchProposalDto;
import java.util.List;
import java.util.UUID;

record MatchingState(
        UUID listingId,
        FoodListingDto listing,
        List<MatchCandidateDto> candidates,
        String providerName,
        String modelName,
        String rawModelOutput,
        MatchingLlmOutput llmOutput,
        String escalationReason,
        int retryCount,
        MatchProposalDto proposal) {

    static MatchingState initial(UUID listingId) {
        return new MatchingState(listingId, null, null, null, null, null, null, null, 0, null);
    }

    MatchingState withListing(FoodListingDto listing) {
        return new MatchingState(listingId, listing, candidates, providerName, modelName, rawModelOutput,
                llmOutput, escalationReason, retryCount, proposal);
    }

    MatchingState withCandidates(List<MatchCandidateDto> candidates) {
        return new MatchingState(listingId, listing, candidates, providerName, modelName, rawModelOutput,
                llmOutput, escalationReason, retryCount, proposal);
    }

    MatchingState withModelOutput(String providerName, String modelName, String rawModelOutput) {
        return new MatchingState(listingId, listing, candidates, providerName, modelName, rawModelOutput,
                llmOutput, escalationReason, retryCount, proposal);
    }

    MatchingState withLlmOutput(MatchingLlmOutput llmOutput) {
        return new MatchingState(listingId, listing, candidates, providerName, modelName, rawModelOutput,
                llmOutput, null, retryCount, proposal);
    }

    MatchingState withValidationFailure(String reason) {
        return new MatchingState(listingId, listing, candidates, providerName, modelName, rawModelOutput,
                null, reason, retryCount + 1, proposal);
    }

    MatchingState withNoCandidates() {
        return new MatchingState(listingId, listing, candidates, providerName, modelName, rawModelOutput,
                llmOutput, "No eligible receiver organizations found nearby.", retryCount, proposal);
    }

    MatchingState withProposal(MatchProposalDto proposal) {
        return new MatchingState(listingId, listing, candidates, providerName, modelName, rawModelOutput,
                llmOutput, escalationReason, retryCount, proposal);
    }
}
