package com.foodloop.ngo.infrastructure.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** The NGO Coordination Agent's trigger (spec §19, docs/architecture/05-ai-agent-architecture.md). */
public record NgoRequestCreatedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID ngoRequestId, UUID ngoOrgId, String foodCategory, BigDecimal quantityNeeded, String quantityUnit,
        Instant neededBefore) {

    public static final String TOPIC = "ngo.request.created.v1";

    public static NgoRequestCreatedEvent of(
            UUID tenantId, UUID ngoRequestId, UUID ngoOrgId, String foodCategory, BigDecimal quantityNeeded,
            String quantityUnit, Instant neededBefore) {
        return new NgoRequestCreatedEvent(
                UUID.randomUUID(), "NGO_REQUEST_CREATED", 1, tenantId, Instant.now(), "ngo-service",
                ngoRequestId, ngoOrgId, foodCategory, quantityNeeded, quantityUnit, neededBefore);
    }
}
