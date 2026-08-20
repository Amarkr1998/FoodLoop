package com.foodloop.ngo.api;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.ngo.application.NgoRequirementService;
import com.foodloop.ngo.domain.NgoRequirement;

@RestController
public class NgoRequirementController {

    private final NgoRequirementService ngoRequirementService;

    public NgoRequirementController(NgoRequirementService ngoRequirementService) {
        this.ngoRequirementService = ngoRequirementService;
    }

    @PutMapping("/api/v1/ngo/requirements")
    public NgoRequirementResponse upsert(@Valid @RequestBody UpsertNgoRequirementRequest request) {
        NgoRequirement requirement = ngoRequirementService.upsert(
                TenantContext.get(), request.ngoOrgId(), request.preferredCategories(),
                request.dietaryRestrictions(), request.capacityPerWeek());
        return NgoRequirementResponse.from(requirement);
    }

    /** The NGO Coordination Agent's getNGORequirements tool reads this (spec §19). */
    @GetMapping("/api/v1/ngo/requirements/{ngoOrgId}")
    public NgoRequirementResponse get(@PathVariable UUID ngoOrgId) {
        NgoRequirement requirement = ngoRequirementService.get(ngoOrgId);
        return requirement != null ? NgoRequirementResponse.from(requirement) : null;
    }
}
