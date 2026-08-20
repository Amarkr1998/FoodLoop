package com.foodloop.ai.agent.trust;

/**
 * The model's only contribution to a risk case (spec §21: "the LLM's job is
 * only to summarize riskFactors into a human-readable rationale") — never a
 * score. {@code riskFactors} is written to {@code RiskCase.riskFactors} as-is
 * after validation; it never influences {@code riskScore}, which
 * {@code RiskCaseService} always re-derives itself.
 */
public record TrustRiskLlmOutput(String riskFactors) {
}
