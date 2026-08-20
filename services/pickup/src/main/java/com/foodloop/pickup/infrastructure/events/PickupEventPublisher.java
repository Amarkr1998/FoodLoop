package com.foodloop.pickup.infrastructure.events;

import com.foodloop.pickup.domain.PickupTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Best-effort publish with a synchronous-exception guard — see
 * FoodEventPublisher's Javadoc (services/food) for why the try/catch here
 * is load-bearing, not defensive boilerplate.
 */
@Component
public class PickupEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PickupEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PickupEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPickupCompleted(PickupTask task) {
        PickupCompletedEvent event = PickupCompletedEvent.of(
                task.getTenantId(), task.getId(), task.getFoodListingId(), task.getClaimId(),
                task.getDonorUserId(), task.getReceiverUserId());
        send(PickupCompletedEvent.TOPIC, event, task.getId());
    }

    public void publishPickupNoShow(PickupTask task) {
        PickupNoShowEvent event = PickupNoShowEvent.of(
                task.getTenantId(), task.getId(), task.getFoodListingId(), task.getClaimId());
        send(PickupNoShowEvent.TOPIC, event, task.getId());
    }

    private void send(String topic, Object event, java.util.UUID taskId) {
        try {
            kafkaTemplate.send(topic, taskId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish to {} for pickupTaskId={}", topic, taskId, ex);
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Failed to publish to {} for pickupTaskId={}", topic, taskId, e);
        }
    }
}
