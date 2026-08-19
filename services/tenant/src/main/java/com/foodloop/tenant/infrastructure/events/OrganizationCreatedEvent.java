package com.foodloop.tenant.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope matches docs/architecture/04-event-catalog.md's standard shape.
 * Published to Kafka topic {@code org.created.v1}; the Matching context
 * (once it exists) and others can react to a new organization joining a
 * tenant without a direct dependency on this service.
 */
public record OrganizationCreatedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID tenantId,
        Instant occurredAt,
        String producer,
        UUID organizationId,
        String name,
        String type) {

    public static final String TOPIC = "org.created.v1";

    public static OrganizationCreatedEvent of(UUID tenantId, UUID organizationId, String name, String type) {
        return new OrganizationCreatedEvent(
                UUID.randomUUID(), "ORG_CREATED", 1, tenantId, Instant.now(), "tenant-service",
                organizationId, name, type);
    }
}
