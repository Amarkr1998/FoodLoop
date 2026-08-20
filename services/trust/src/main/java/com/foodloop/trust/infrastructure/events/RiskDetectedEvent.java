package com.foodloop.trust.infrastructure.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** No consumer exists yet — Identity's future enforcement workflow is the natural subscriber, same precedent as match.proposed.v1 predating NGO's consumer. */
public record RiskDetectedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID riskCaseId, UUID targetUserId, BigDecimal riskScore, boolean requiresHumanReview) {

    public static final String TOPIC = "risk.detected.v1";

    public static RiskDetectedEvent of(UUID tenantId, UUID riskCaseId, UUID targetUserId, BigDecimal riskScore, boolean requiresHumanReview) {
        return new RiskDetectedEvent(
                UUID.randomUUID(), "RISK_DETECTED", 1, tenantId, Instant.now(), "trust-service",
                riskCaseId, targetUserId, riskScore, requiresHumanReview);
    }
}
