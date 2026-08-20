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
        Double latitude,
        Double longitude,
        Instant createdAt) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getTenantId(),
                organization.getName(),
                organization.getType().name(),
                organization.getVerificationStatus().name(),
                organization.getLocation() != null ? organization.getLocation().getY() : null,
                organization.getLocation() != null ? organization.getLocation().getX() : null,
                organization.getCreatedAt());
    }
}
