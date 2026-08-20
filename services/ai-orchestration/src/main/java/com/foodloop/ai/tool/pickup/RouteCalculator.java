package com.foodloop.ai.tool.pickup;

import java.util.Map;

/**
 * Deterministic routing (spec §20: "delegates to deterministic routing, not
 * LLM math") — plain Haversine distance (same formula as Matching's
 * DistanceCalculator; this agent has no PostGIS column of its own to query)
 * plus a coarse ETA from a per-vehicle-type average urban speed. Not a real
 * routing engine (no roads, traffic, or turn-by-turn) — an order-of-magnitude
 * estimate for "should we notify/reassign", not turn-by-turn navigation.
 */
public final class RouteCalculator {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /** Coarse average urban speed in meters/minute, keyed to Pickup's VehicleType enum values. */
    private static final Map<String, Double> SPEED_METERS_PER_MINUTE = Map.of(
            "ON_FOOT", 75.0,
            "BICYCLE", 250.0,
            "SCOOTER", 417.0,
            "CAR", 500.0);
    private static final double DEFAULT_SPEED_METERS_PER_MINUTE = 250.0;

    private RouteCalculator() {
    }

    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    public static int estimatedEtaMinutes(double distanceMeters, String vehicleType) {
        double speed = SPEED_METERS_PER_MINUTE.getOrDefault(vehicleType, DEFAULT_SPEED_METERS_PER_MINUTE);
        return (int) Math.ceil(distanceMeters / speed);
    }
}
