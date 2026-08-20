package com.foodloop.tenant.application;

import com.foodloop.tenant.domain.Tenant;
import com.foodloop.tenant.domain.TenantRepository;
import com.foodloop.tenant.domain.TenantStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<Tenant> listActiveTenants() {
        return tenantRepository.findByStatus(TenantStatus.ACTIVE);
    }
}
