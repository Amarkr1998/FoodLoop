package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NgoRequirementDto(UUID ngoOrgId, String[] preferredCategories, String[] dietaryRestrictions, Integer capacityPerWeek) {
}
