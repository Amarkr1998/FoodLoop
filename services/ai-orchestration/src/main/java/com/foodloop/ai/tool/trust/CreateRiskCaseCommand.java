package com.foodloop.ai.tool.trust;

import java.util.UUID;

public record CreateRiskCaseCommand(UUID targetUserId, String riskFactors) {
}
