package com.foodloop.tenant.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    List<Tenant> findByStatus(TenantStatus status);
}
