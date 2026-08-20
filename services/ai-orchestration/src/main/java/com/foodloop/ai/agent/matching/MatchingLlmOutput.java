package com.foodloop.ai.agent.matching;

import java.util.UUID;

/**
 * The model's re-ranking output — it may only choose among the candidates
 * {@link MatchingAgent} already gave it (docs/architecture/05 §17: "cannot
 * introduce an ineligible candidate"); {@code receiverOrgId} not being in
 * that set is caught as a validation failure, not trusted.
 */
public record MatchingLlmOutput(UUID receiverOrgId, String rationale) {
}
