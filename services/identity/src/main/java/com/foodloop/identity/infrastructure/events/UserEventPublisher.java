package com.foodloop.identity.infrastructure.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Registration succeeding never depends on Kafka being reachable — the
 * write to Postgres is the durable fact; publishing is best-effort and
 * failures are logged, not propagated to the caller, consistent with
 * critical-path availability (ADR-008's principle applied to messaging,
 * not just AI providers).
 */
@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public UserEventPublisher(KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send(UserRegisteredEvent.TOPIC, event.tenantId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish USER_REGISTERED for userId={}", event.userId(), ex);
                    }
                });
    }
}
