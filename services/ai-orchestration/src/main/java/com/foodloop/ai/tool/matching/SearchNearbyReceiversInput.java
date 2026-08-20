package com.foodloop.ai.tool.matching;

import java.util.UUID;

/** {@code radiusKm} is nullable — pass {@code null} to use {@link SearchNearbyReceiversTool}'s default. */
public record SearchNearbyReceiversInput(UUID foodListingId, Double radiusKm) {

    public static SearchNearbyReceiversInput defaultRadius(UUID foodListingId) {
        return new SearchNearbyReceiversInput(foodListingId, null);
    }
}
