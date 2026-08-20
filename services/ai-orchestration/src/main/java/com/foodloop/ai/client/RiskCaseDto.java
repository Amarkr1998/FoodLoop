package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RiskCaseDto(
        UUID id, UUID targetUserId, BigDecimal riskScore, String riskFactors, boolean requiresHumanReview, String status) {
}
