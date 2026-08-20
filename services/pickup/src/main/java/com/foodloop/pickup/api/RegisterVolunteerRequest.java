package com.foodloop.pickup.api;

import com.foodloop.pickup.domain.VehicleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterVolunteerRequest(@NotNull VehicleType vehicleType, @Positive Integer capacityServings) {
}
