package com.foodloop.matching.infrastructure.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * NGO Coordination Agent (services/ngo) is this event's consumer:
 * {@code ngoRequestId} is non-null only when this proposal fulfills an open
 * NGO bulk request, letting NGO mark that request MATCHED without a
 * synchronous cross-service call from the agent's tool node.
 */
public record MatchProposedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID matchProposalId, UUID foodListingId, UUID receiverOrgId, BigDecimal score, UUID ngoRequestId) {

    public static final String TOPIC = "match.proposed.v1";

    public static MatchProposedEvent of(
            UUID tenantId, UUID matchProposalId, UUID foodListingId, UUID receiverOrgId, BigDecimal score,
            UUID ngoRequestId) {
        return new MatchProposedEvent(
                UUID.randomUUID(), "MATCH_PROPOSED", 1, tenantId, Instant.now(), "matching-service",
                matchProposalId, foodListingId, receiverOrgId, score, ngoRequestId);
    }
}
