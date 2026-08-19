package com.foodloop.food.domain;

import java.security.SecureRandom;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Public discovery shows approx_location, not the exact point (§33, threat
 * T13) — jitters a coordinate by a small random offset so a receiver
 * browsing nearby listings can't pinpoint a donor's precise address before
 * claiming.
 */
public final class GeoUtils {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;
    private static final double MAX_JITTER_METERS = 150.0;
    private static final SecureRandom RANDOM = new SecureRandom();

    private GeoUtils() {
    }

    public static Point point(double lat, double lng) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
    }

    public static Point jitter(double lat, double lng) {
        double bearingRadians = RANDOM.nextDouble() * 2 * Math.PI;
        double offsetMeters = RANDOM.nextDouble() * MAX_JITTER_METERS;

        double deltaLat = (offsetMeters * Math.cos(bearingRadians)) / METERS_PER_DEGREE_LAT;
        double metersPerDegreeLng = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat));
        double deltaLng = (offsetMeters * Math.sin(bearingRadians)) / metersPerDegreeLng;

        return point(lat + deltaLat, lng + deltaLng);
    }
}
