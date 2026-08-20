package com.foodloop.pickup.api;

import com.foodloop.pickup.domain.VolunteerProfile;
import java.time.Instant;
import java.util.UUID;

public record VolunteerProfileResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        String vehicleType,
        Integer capacityServings,
        boolean available,
        Double latitude,
        Double longitude,
        Instant createdAt) {

    public static VolunteerProfileResponse from(VolunteerProfile profile) {
        return new VolunteerProfileResponse(
                profile.getId(),
                profile.getTenantId(),
                profile.getUserId(),
                profile.getVehicleType().name(),
                profile.getCapacityServings(),
                profile.isAvailable(),
                profile.getCurrentLocation() != null ? profile.getCurrentLocation().getY() : null,
                profile.getCurrentLocation() != null ? profile.getCurrentLocation().getX() : null,
                profile.getCreatedAt());
    }
}
