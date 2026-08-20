package com.foodloop.ai.api;

import jakarta.validation.constraints.NotNull;

public record ResolveEscalationRequest(@NotNull Boolean approve) {
}
