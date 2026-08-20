package com.foodloop.matching.domain;

/**
 * Plain-Java Haversine distance — this schema has no geography column of
 * its own (see pom.xml's Javadoc), so eligibility re-validation recomputes
 * distance from the lat/lng Food and Tenant already return rather than
 * duplicating PostGIS here.
 */
public final class DistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private DistanceCalculator() {
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
}
