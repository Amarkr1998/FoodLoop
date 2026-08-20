package com.foodloop.ngo.infrastructure.events;

import com.foodloop.ngo.domain.NgoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Best-effort publish — see services/food's FoodEventPublisher Javadoc for why the try/catch here is load-bearing, not defensive filler. */
@Component
public class NgoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NgoEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NgoEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRequestCreated(NgoRequest request) {
        NgoRequestCreatedEvent event = NgoRequestCreatedEvent.of(
                request.getTenantId(), request.getId(), request.getNgoOrgId(), request.getFoodCategory(),
                request.getQuantityNeeded(), request.getQuantityUnit(), request.getNeededBefore());
        try {
            kafkaTemplate.send(NgoRequestCreatedEvent.TOPIC, request.getId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish to {} for ngoRequestId={}",
                                    NgoRequestCreatedEvent.TOPIC, request.getId(), ex);
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Failed to publish to {} for ngoRequestId={}", NgoRequestCreatedEvent.TOPIC, request.getId(), e);
        }
    }
}
