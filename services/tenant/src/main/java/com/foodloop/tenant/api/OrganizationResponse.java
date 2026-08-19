package com.foodloop.tenant.api;

import com.foodloop.tenant.domain.Organization;
import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        UUID tenantId,
        String name,
        String type,
        String verificationStatus,
        Instant createdAt) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getTenantId(),
                organization.getName(),
                organization.getType().name(),
                organization.getVerificationStatus().name(),
                organization.getCreatedAt());
    }
}
