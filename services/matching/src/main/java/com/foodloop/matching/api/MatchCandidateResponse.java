package com.foodloop.matching.api;

import com.foodloop.matching.application.MatchCandidate;
import java.math.BigDecimal;
import java.util.UUID;

public record MatchCandidateResponse(UUID receiverOrgId, String receiverOrgName, double distanceMeters, BigDecimal score) {

    public static MatchCandidateResponse from(MatchCandidate candidate) {
        return new MatchCandidateResponse(
                candidate.receiverOrgId(), candidate.receiverOrgName(), candidate.distanceMeters(), candidate.score());
    }
}
