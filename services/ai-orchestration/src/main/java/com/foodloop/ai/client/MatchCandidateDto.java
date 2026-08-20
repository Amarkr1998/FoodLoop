package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchCandidateDto(UUID receiverOrgId, String receiverOrgName, double distanceMeters, BigDecimal score) {
}
