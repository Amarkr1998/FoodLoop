package com.foodloop.ai.tool.pickup;

import java.util.UUID;

public record FindAvailableVolunteersInput(UUID pickupTaskId, double radiusKm) {
}
