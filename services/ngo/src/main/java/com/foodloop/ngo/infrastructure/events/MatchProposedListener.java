package com.foodloop.ngo.infrastructure.events;

import com.foodloop.ngo.application.NgoRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Ignores every proposal not initiated by the NGO Coordination Agent (ngoRequestId null) — see MatchProposedEvent's Javadoc. */
@Component
public class MatchProposedListener {

    private static final Logger log = LoggerFactory.getLogger(MatchProposedListener.class);

    private final NgoRequestService ngoRequestService;

    public MatchProposedListener(NgoRequestService ngoRequestService) {
        this.ngoRequestService = ngoRequestService;
    }

    @KafkaListener(topics = MatchProposedEvent.TOPIC, groupId = "ngo-service")
    public void onMatchProposed(MatchProposedEvent event) {
        if (event.ngoRequestId() == null) {
            return;
        }
        log.info("Received MATCH_PROPOSED for ngoRequestId={}", event.ngoRequestId());
        ngoRequestService.markMatchedFromProposal(
                event.tenantId(), event.ngoRequestId(), event.matchProposalId(), event.foodListingId());
    }
}
