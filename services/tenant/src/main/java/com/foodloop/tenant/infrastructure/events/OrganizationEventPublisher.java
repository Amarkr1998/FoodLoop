package com.foodloop.tenant.infrastructure.events;

import com.foodloop.tenant.domain.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Best-effort publish, same principle as identity's UserEventPublisher:
 * creating an organization succeeding never depends on Kafka being
 * reachable.
 */
@Component
public class OrganizationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrganizationEventPublisher.class);

    private final KafkaTemplate<String, OrganizationCreatedEvent> kafkaTemplate;

    public OrganizationEventPublisher(KafkaTemplate<String, OrganizationCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrganizationCreated(Organization organization) {
        OrganizationCreatedEvent event = OrganizationCreatedEvent.of(
                organization.getTenantId(), organization.getId(), organization.getName(), organization.getType().name());
        kafkaTemplate.send(OrganizationCreatedEvent.TOPIC, event.tenantId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish ORG_CREATED for organizationId={}", organization.getId(), ex);
                    }
                });
    }
}
