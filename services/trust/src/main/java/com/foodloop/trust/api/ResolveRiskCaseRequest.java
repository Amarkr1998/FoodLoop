package com.foodloop.trust.api;

import jakarta.validation.constraints.NotBlank;

public record ResolveRiskCaseRequest(@NotBlank String resolutionAction) {
}
