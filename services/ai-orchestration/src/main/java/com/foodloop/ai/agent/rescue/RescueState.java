package com.foodloop.ai.agent.rescue;

import com.foodloop.ai.client.FoodListingDto;
import com.foodloop.ai.client.MatchCandidateDto;
import java.util.List;
import java.util.UUID;

record RescueState(
        UUID listingId,
        RescueThreshold threshold,
        FoodListingDto listing,
        List<MatchCandidateDto> candidates,
        int notifiedCount,
        UUID proposedOrgId,
        boolean listingNoLongerAvailable) {

    static RescueState initial(UUID listingId, RescueThreshold threshold) {
        return new RescueState(listingId, threshold, null, null, 0, null, false);
    }

    RescueState withListing(FoodListingDto listing) {
        boolean stillAvailable = listing != null && "AVAILABLE".equals(listing.status());
        return new RescueState(listingId, threshold, listing, candidates, notifiedCount, proposedOrgId, !stillAvailable);
    }

    RescueState withCandidates(List<MatchCandidateDto> candidates) {
        return new RescueState(listingId, threshold, listing, candidates, notifiedCount, proposedOrgId, listingNoLongerAvailable);
    }

    RescueState withNotifiedCount(int notifiedCount) {
        return new RescueState(listingId, threshold, listing, candidates, notifiedCount, proposedOrgId, listingNoLongerAvailable);
    }

    RescueState withProposedOrgId(UUID proposedOrgId) {
        return new RescueState(listingId, threshold, listing, candidates, notifiedCount, proposedOrgId, listingNoLongerAvailable);
    }
}
