package com.foodloop.identity.api;

import com.foodloop.identity.domain.AppUser;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        UUID tenantId,
        String email,
        String phone,
        String displayName,
        String locale,
        String status,
        Instant createdAt) {

    public static UserProfileResponse from(AppUser user) {
        return new UserProfileResponse(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getPhone(),
                user.getDisplayName(),
                user.getLocale(),
                user.getStatus().name(),
                user.getCreatedAt());
    }
}
