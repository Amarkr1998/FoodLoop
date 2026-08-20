package com.foodloop.matching.infrastructure.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * No consumer exists yet — NGO Coordination (Phase 8) is the natural future
 * subscriber for approval/notification workflows — published proactively
 * now anyway, same precedent as food.listed.v1 predating Pickup's consumer.
 */
public record MatchProposedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID matchProposalId, UUID foodListingId, UUID receiverOrgId, BigDecimal score) {

    public static final String TOPIC = "match.proposed.v1";

    public static MatchProposedEvent of(
            UUID tenantId, UUID matchProposalId, UUID foodListingId, UUID receiverOrgId, BigDecimal score) {
        return new MatchProposedEvent(
                UUID.randomUUID(), "MATCH_PROPOSED", 1, tenantId, Instant.now(), "matching-service",
                matchProposalId, foodListingId, receiverOrgId, score);
    }
}
