package com.foodloop.ai.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssessTrustRiskRequest(@NotNull UUID targetUserId) {
}
