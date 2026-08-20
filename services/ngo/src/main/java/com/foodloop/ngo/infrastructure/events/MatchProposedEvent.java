package com.foodloop.ngo.infrastructure.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * This service's own view of the event contract published by Matching
 * (services/matching/.../MatchProposedEvent) — see that class's Javadoc for
 * why a consumer keeps its own copy rather than sharing the producer's Java
 * class. {@code ngoRequestId} is null for proposals not initiated by the
 * NGO Coordination Agent; this listener ignores those.
 */
public record MatchProposedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID matchProposalId, UUID foodListingId, UUID receiverOrgId, BigDecimal score, UUID ngoRequestId) {

    public static final String TOPIC = "match.proposed.v1";
}
