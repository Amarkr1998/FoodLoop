package com.foodloop.matching.infrastructure.events;

import com.foodloop.matching.domain.MatchProposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Best-effort publish — see services/food's FoodEventPublisher Javadoc for why the try/catch here is load-bearing, not defensive filler. */
@Component
public class MatchEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MatchEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MatchEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishMatchProposed(MatchProposal proposal) {
        MatchProposedEvent event = MatchProposedEvent.of(
                proposal.getTenantId(), proposal.getId(), proposal.getFoodListingId(),
                proposal.getReceiverOrgId(), proposal.getScore(), proposal.getNgoRequestId());
        try {
            kafkaTemplate.send(MatchProposedEvent.TOPIC, proposal.getId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish to {} for matchProposalId={}",
                                    MatchProposedEvent.TOPIC, proposal.getId(), ex);
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Failed to publish to {} for matchProposalId={}", MatchProposedEvent.TOPIC, proposal.getId(), e);
        }
    }
}
