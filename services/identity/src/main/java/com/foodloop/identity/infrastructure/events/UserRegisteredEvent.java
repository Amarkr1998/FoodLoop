package com.foodloop.identity.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope matches docs/architecture/04-event-catalog.md's standard shape
 * (eventId, eventType, eventVersion, tenantId, occurredAt, correlationId,
 * payload fields). Published to Kafka topic {@code user.registered.v1}.
 */
public record UserRegisteredEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID tenantId,
        Instant occurredAt,
        String correlationId,
        String producer,
        UUID userId,
        String email,
        String displayName) {

    public static final String TOPIC = "user.registered.v1";

    public static UserRegisteredEvent of(UUID tenantId, UUID userId, String email, String displayName, String correlationId) {
        return new UserRegisteredEvent(
                UUID.randomUUID(),
                "USER_REGISTERED",
                1,
                tenantId,
                Instant.now(),
                correlationId,
                "identity-service",
                userId,
                email,
                displayName);
    }
}
