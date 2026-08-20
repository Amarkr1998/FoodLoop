package com.foodloop.tenant.api;

import com.foodloop.tenant.domain.Tenant;
import java.util.UUID;

public record TenantResponse(UUID id, String name, String regionId, String countryCode, String status) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(), tenant.getName(), tenant.getRegionId(), tenant.getCountryCode(), tenant.getStatus().name());
    }
}
