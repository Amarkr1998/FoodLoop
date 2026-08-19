package com.foodloop.food.infrastructure.events;

import com.foodloop.food.domain.Claim;
import com.foodloop.food.domain.FoodListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Best-effort publish, consistent with identity/tenant: a listing being
 * published or claimed never depends on Kafka being reachable (ADR-008's
 * principle applied to messaging generally, not just AI providers).
 *
 * <p>{@code KafkaTemplate.send()} is only asynchronous once partition
 * metadata for the target topic is cached — the first send to a topic
 * blocks the calling thread (up to {@code max.block.ms}, capped short via
 * application.yml) fetching it, and throws <em>synchronously</em>, not via
 * the returned future, if that fails. Without the try/catch here, a Kafka
 * outage would fail the donor's publish/claim request outright — exactly
 * what "never depends on Kafka being reachable" is supposed to prevent.
 */
@Component
public class FoodEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FoodEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FoodEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishFoodListed(FoodListing listing) {
        FoodListedEvent event = FoodListedEvent.of(
                listing.getTenantId(), listing.getId(), listing.getDonorOrgId(),
                listing.getFoodCategory().name(), listing.getExpiryTime());
        send(FoodListedEvent.TOPIC, event, listing.getId());
    }

    public void publishFoodClaimed(FoodListing listing, Claim claim) {
        FoodClaimedEvent event = FoodClaimedEvent.of(
                listing.getTenantId(), listing.getId(), claim.getId(), claim.getReceiverUserId());
        send(FoodClaimedEvent.TOPIC, event, listing.getId());
    }

    private void send(String topic, Object event, java.util.UUID listingId) {
        try {
            kafkaTemplate.send(topic, listingId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish to {} for listingId={}", topic, listingId, ex);
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Failed to publish to {} for listingId={}", topic, listingId, e);
        }
    }
}
