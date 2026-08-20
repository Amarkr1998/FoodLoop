package com.foodloop.impact.infrastructure.events;

import com.foodloop.impact.application.ImpactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** This service's entire trigger (spec Phase 11) — no HTTP endpoint creates a RescueRecord directly. */
@Component
public class PickupCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(PickupCompletedListener.class);

    private final ImpactService impactService;

    public PickupCompletedListener(ImpactService impactService) {
        this.impactService = impactService;
    }

    @KafkaListener(topics = PickupCompletedEvent.TOPIC, groupId = "impact-service")
    public void onPickupCompleted(PickupCompletedEvent event) {
        log.info("Received PICKUP_COMPLETED for pickupTaskId={}", event.pickupTaskId());
        impactService.recordFromPickupCompleted(event);
    }
}
