package com.foodloop.pickup.api;

import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(@NotNull Boolean available) {
}
