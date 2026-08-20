package com.foodloop.ai.tool.ngo;

public record SearchNearbyFoodInput(double lat, double lng, double radiusKm, String category) {
}
