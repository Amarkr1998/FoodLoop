package com.foodloop.matching.api;

import com.foodloop.matching.domain.MatchProposal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MatchProposalResponse(
        UUID id,
        UUID tenantId,
        UUID foodListingId,
        UUID receiverOrgId,
        BigDecimal distanceMeters,
        BigDecimal score,
        String aiRationale,
        String status,
        Instant createdAt) {

    public static MatchProposalResponse from(MatchProposal proposal) {
        return new MatchProposalResponse(
                proposal.getId(),
                proposal.getTenantId(),
                proposal.getFoodListingId(),
                proposal.getReceiverOrgId(),
                proposal.getDistanceMeters(),
                proposal.getScore(),
                proposal.getAiRationale(),
                proposal.getStatus().name(),
                proposal.getCreatedAt());
    }
}
