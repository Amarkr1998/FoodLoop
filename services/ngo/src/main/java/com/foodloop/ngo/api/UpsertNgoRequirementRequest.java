package com.foodloop.ngo.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpsertNgoRequirementRequest(
        @NotNull UUID ngoOrgId, String[] preferredCategories, String[] dietaryRestrictions, Integer capacityPerWeek) {
}
