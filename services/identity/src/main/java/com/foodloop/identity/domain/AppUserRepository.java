package com.foodloop.identity.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByKeycloakId(UUID keycloakId);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}
