package com.foodloop.ai.client;

import java.util.UUID;

/** Mirrors Trust's CreateRiskCaseRequest — the wire shape of the POST /api/v1/trust/risk-cases request body. */
public record CreateRiskCasePayload(UUID targetUserId, String riskFactors) {
}
