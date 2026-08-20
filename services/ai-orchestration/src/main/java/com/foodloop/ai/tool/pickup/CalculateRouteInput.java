package com.foodloop.ai.tool.pickup;

public record CalculateRouteInput(double fromLat, double fromLng, double toLat, double toLng, String vehicleType) {
}
