package com.foodloop.ngo.application;

import com.foodloop.ngo.domain.NgoRequirement;
import com.foodloop.ngo.domain.NgoRequirementRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NgoRequirementService {

    private final NgoRequirementRepository ngoRequirementRepository;

    public NgoRequirementService(NgoRequirementRepository ngoRequirementRepository) {
        this.ngoRequirementRepository = ngoRequirementRepository;
    }

    @Transactional
    public NgoRequirement upsert(
            UUID tenantId, UUID ngoOrgId, String[] preferredCategories, String[] dietaryRestrictions,
            Integer capacityPerWeek) {
        NgoRequirement requirement = ngoRequirementRepository.findByNgoOrgId(ngoOrgId)
                .orElseGet(() -> new NgoRequirement(tenantId, ngoOrgId));
        requirement.update(preferredCategories, dietaryRestrictions, capacityPerWeek);
        return ngoRequirementRepository.save(requirement);
    }

    @Transactional(readOnly = true)
    public NgoRequirement get(UUID ngoOrgId) {
        return ngoRequirementRepository.findByNgoOrgId(ngoOrgId).orElse(null);
    }
}
