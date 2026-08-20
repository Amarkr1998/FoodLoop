package com.foodloop.trust.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

/** No consumer exists yet — same precedent as RiskDetectedEvent's Javadoc. */
public record ModerationActionTakenEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID riskCaseId, UUID targetUserId, String resolutionAction, UUID resolvedByUserId) {

    public static final String TOPIC = "moderation.action_taken.v1";

    public static ModerationActionTakenEvent of(
            UUID tenantId, UUID riskCaseId, UUID targetUserId, String resolutionAction, UUID resolvedByUserId) {
        return new ModerationActionTakenEvent(
                UUID.randomUUID(), "MODERATION_ACTION_TAKEN", 1, tenantId, Instant.now(), "trust-service",
                riskCaseId, targetUserId, resolutionAction, resolvedByUserId);
    }
}
