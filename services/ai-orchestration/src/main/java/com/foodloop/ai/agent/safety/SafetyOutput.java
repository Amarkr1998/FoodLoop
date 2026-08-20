package com.foodloop.ai.agent.safety;

import java.util.List;

/**
 * The Safety Agent's structured output (spec §22). {@code reason} is what
 * gets scanned by {@link com.foodloop.ai.guardrail.CertificationClaimGuard}
 * before it's trusted — never persisted if it asserts a certification the
 * agent has no authority to make.
 */
public record SafetyOutput(boolean requiresHumanReview, String reason, List<String> missingInformation) {
}
