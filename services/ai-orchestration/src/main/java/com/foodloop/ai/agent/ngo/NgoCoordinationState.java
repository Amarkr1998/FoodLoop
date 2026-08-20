package com.foodloop.ai.agent.ngo;

import com.foodloop.ai.client.FoodSearchResultDto;
import com.foodloop.ai.client.MatchProposalDto;
import com.foodloop.ai.client.NgoRequestDto;
import com.foodloop.ai.client.OrganizationDto;
import java.util.List;
import java.util.UUID;

record NgoCoordinationState(
        UUID ngoRequestId,
        NgoRequestDto request,
        OrganizationDto ngoOrg,
        List<FoodSearchResultDto> candidates,
        FoodSearchResultDto chosenCandidate,
        boolean requestNoLongerOpen,
        boolean escalated,
        MatchProposalDto proposal,
        boolean skippedAsDuplicate) {

    static NgoCoordinationState initial(UUID ngoRequestId) {
        return new NgoCoordinationState(ngoRequestId, null, null, null, null, false, false, null, false);
    }

    NgoCoordinationState withRequest(NgoRequestDto request) {
        boolean stillOpen = request != null && "OPEN".equals(request.status());
        return new NgoCoordinationState(
                ngoRequestId, request, ngoOrg, candidates, chosenCandidate, !stillOpen, escalated, proposal, skippedAsDuplicate);
    }

    NgoCoordinationState withNgoOrg(OrganizationDto ngoOrg) {
        return new NgoCoordinationState(
                ngoRequestId, request, ngoOrg, candidates, chosenCandidate, requestNoLongerOpen, escalated, proposal, skippedAsDuplicate);
    }

    NgoCoordinationState withCandidates(List<FoodSearchResultDto> candidates, FoodSearchResultDto chosenCandidate) {
        return new NgoCoordinationState(
                ngoRequestId, request, ngoOrg, candidates, chosenCandidate, requestNoLongerOpen, escalated, proposal, skippedAsDuplicate);
    }

    NgoCoordinationState withEscalated() {
        return new NgoCoordinationState(
                ngoRequestId, request, ngoOrg, candidates, chosenCandidate, requestNoLongerOpen, true, proposal, skippedAsDuplicate);
    }

    NgoCoordinationState withProposal(MatchProposalDto proposal) {
        return new NgoCoordinationState(
                ngoRequestId, request, ngoOrg, candidates, chosenCandidate, requestNoLongerOpen, escalated, proposal, skippedAsDuplicate);
    }

    NgoCoordinationState withSkippedAsDuplicate() {
        return new NgoCoordinationState(
                ngoRequestId, request, ngoOrg, candidates, chosenCandidate, requestNoLongerOpen, escalated, proposal, true);
    }
}
