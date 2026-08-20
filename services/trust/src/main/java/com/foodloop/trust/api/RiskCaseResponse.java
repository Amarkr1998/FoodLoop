package com.foodloop.trust.api;

import com.foodloop.trust.domain.RiskCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RiskCaseResponse(
        UUID id, UUID targetUserId, BigDecimal riskScore, String riskFactors, boolean requiresHumanReview,
        String status, String resolutionAction, Instant createdAt, Instant resolvedAt) {

    public static RiskCaseResponse from(RiskCase riskCase) {
        return new RiskCaseResponse(
                riskCase.getId(), riskCase.getTargetUserId(), riskCase.getRiskScore(), riskCase.getRiskFactors(),
                riskCase.isRequiresHumanReview(), riskCase.getStatus().name(), riskCase.getResolutionAction(),
                riskCase.getCreatedAt(), riskCase.getResolvedAt());
    }
}
