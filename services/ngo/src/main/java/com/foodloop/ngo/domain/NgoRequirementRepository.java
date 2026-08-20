package com.foodloop.ngo.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NgoRequirementRepository extends JpaRepository<NgoRequirement, UUID> {

    Optional<NgoRequirement> findByNgoOrgId(UUID ngoOrgId);
}
