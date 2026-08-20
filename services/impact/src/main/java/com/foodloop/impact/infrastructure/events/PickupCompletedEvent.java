package com.foodloop.impact.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

/**
 * This service's own view of the event contract published by Pickup
 * (services/pickup/.../PickupCompletedEvent) — see that class's Javadoc for
 * why a consumer keeps its own copy rather than sharing the producer's Java
 * class, and for why donorUserId/receiverUserId exist on it at all (added
 * specifically for this consumer).
 */
public record PickupCompletedEvent(
        UUID eventId, String eventType, int eventVersion, UUID tenantId, Instant occurredAt, String producer,
        UUID pickupTaskId, UUID foodListingId, UUID claimId, UUID donorUserId, UUID receiverUserId) {

    public static final String TOPIC = "pickup.completed.v1";
}
