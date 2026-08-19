package com.foodloop.pickup.infrastructure.events;

import com.foodloop.pickup.application.PickupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The FCFS-claim equivalent of consuming MATCH_ACCEPTED (Matching is Phase
 * 7 and doesn't exist yet) — this is what actually creates a pickup task;
 * nothing calls PickupService.createFromClaim directly over HTTP.
 * Idempotency is handled inside the service (unique index on claim_id), so
 * this listener doesn't need its own dedup tracking — a redelivered event
 * just no-ops.
 */
@Component
public class FoodClaimedListener {

    private static final Logger log = LoggerFactory.getLogger(FoodClaimedListener.class);

    private final PickupService pickupService;

    public FoodClaimedListener(PickupService pickupService) {
        this.pickupService = pickupService;
    }

    @KafkaListener(topics = FoodClaimedEvent.TOPIC, groupId = "pickup-service")
    public void onFoodClaimed(FoodClaimedEvent event) {
        log.info("Received FOOD_CLAIMED for claimId={}, foodListingId={}", event.claimId(), event.foodListingId());
        pickupService.createFromClaim(event);
    }
}
