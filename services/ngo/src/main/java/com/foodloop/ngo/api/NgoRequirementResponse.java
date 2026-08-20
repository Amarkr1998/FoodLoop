package com.foodloop.ngo.api;

import com.foodloop.ngo.domain.NgoRequirement;
import java.time.Instant;
import java.util.UUID;

public record NgoRequirementResponse(
        UUID id, UUID ngoOrgId, String[] preferredCategories, String[] dietaryRestrictions,
        Integer capacityPerWeek, Instant updatedAt) {

    public static NgoRequirementResponse from(NgoRequirement requirement) {
        return new NgoRequirementResponse(
                requirement.getId(), requirement.getNgoOrgId(), requirement.getPreferredCategories(),
                requirement.getDietaryRestrictions(), requirement.getCapacityPerWeek(), requirement.getUpdatedAt());
    }
}
