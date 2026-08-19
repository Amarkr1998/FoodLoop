package com.foodloop.food.infrastructure.events;

import com.foodloop.food.application.FoodListingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Closes the loop with Pickup: a completed or no-show pickup advances the
 * food listing's own state machine (§11) without Pickup needing a
 * synchronous call back into Food's API.
 */
@Component
public class PickupOutcomeListener {

    private static final Logger log = LoggerFactory.getLogger(PickupOutcomeListener.class);

    private final FoodListingService foodListingService;

    public PickupOutcomeListener(FoodListingService foodListingService) {
        this.foodListingService = foodListingService;
    }

    @KafkaListener(topics = PickupCompletedEvent.TOPIC, groupId = "food-service")
    public void onPickupCompleted(PickupCompletedEvent event) {
        log.info("Received PICKUP_COMPLETED for foodListingId={}", event.foodListingId());
        foodListingService.applyPickupCompleted(event.tenantId(), event.foodListingId());
    }

    @KafkaListener(topics = PickupNoShowEvent.TOPIC, groupId = "food-service")
    public void onPickupNoShow(PickupNoShowEvent event) {
        log.info("Received PICKUP_NO_SHOW for foodListingId={}", event.foodListingId());
        foodListingService.applyPickupNoShow(event.tenantId(), event.foodListingId());
    }
}
