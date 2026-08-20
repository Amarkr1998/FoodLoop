package com.foodloop.matching.application;

import java.math.BigDecimal;
import java.util.UUID;

/** Read-model output of {@link MatchingService#findCandidates} — never persisted; a proposal is only written once one candidate is chosen. */
public record MatchCandidate(UUID receiverOrgId, String receiverOrgName, double distanceMeters, BigDecimal score) {
}
