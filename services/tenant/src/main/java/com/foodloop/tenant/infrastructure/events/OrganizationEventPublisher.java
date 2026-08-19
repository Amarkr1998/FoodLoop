package com.foodloop.tenant.infrastructure.events;

import com.foodloop.tenant.domain.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Best-effort publish, same principle as identity's UserEventPublisher:
 * creating an organization succeeding never depends on Kafka being
 * reachable. See UserEventPublisher's Javadoc for why the try/catch here
 * is load-bearing, not defensive boilerplate: KafkaTemplate.send() throws
 * synchronously (not just via the returned future) on the first send to a
 * topic when metadata can't be fetched.
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
        try {
            kafkaTemplate.send(OrganizationCreatedEvent.TOPIC, event.tenantId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish ORG_CREATED for organizationId={}", organization.getId(), ex);
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Failed to publish ORG_CREATED for organizationId={}", organization.getId(), e);
        }
    }
}
