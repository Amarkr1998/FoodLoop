package com.foodloop.trust.infrastructure.events;

import com.foodloop.trust.domain.RiskCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Best-effort publish — see services/food's FoodEventPublisher Javadoc for why the try/catch here is load-bearing, not defensive filler. */
@Component
public class TrustEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TrustEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TrustEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRiskDetected(RiskCase riskCase) {
        RiskDetectedEvent event = RiskDetectedEvent.of(
                riskCase.getTenantId(), riskCase.getId(), riskCase.getTargetUserId(), riskCase.getRiskScore(),
                riskCase.isRequiresHumanReview());
        send(RiskDetectedEvent.TOPIC, event, riskCase.getId());
    }

    public void publishModerationActionTaken(RiskCase riskCase) {
        ModerationActionTakenEvent event = ModerationActionTakenEvent.of(
                riskCase.getTenantId(), riskCase.getId(), riskCase.getTargetUserId(), riskCase.getResolutionAction(),
                riskCase.getResolvedByUserId());
        send(ModerationActionTakenEvent.TOPIC, event, riskCase.getId());
    }

    private void send(String topic, Object event, java.util.UUID key) {
        try {
            kafkaTemplate.send(topic, key.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish to {} for riskCaseId={}", topic, key, ex);
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Failed to publish to {} for riskCaseId={}", topic, key, e);
        }
    }
}
