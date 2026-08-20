package com.foodloop.trust.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateRiskCaseRequest(@NotNull UUID targetUserId, String riskFactors) {
}
