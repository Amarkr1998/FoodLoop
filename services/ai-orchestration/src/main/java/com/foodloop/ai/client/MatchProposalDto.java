package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchProposalDto(
        UUID id, UUID foodListingId, UUID receiverOrgId, BigDecimal distanceMeters, BigDecimal score,
        String aiRationale, String status) {
}
