package com.foodloop.pickup.domain;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Pickup only ever needs the exact point (it's shown to the matched
 * receiver, who has already claimed the food) — no jitter helper here
 * unlike Food's GeoUtils, which serves public pre-claim discovery.
 */
public final class GeoUtils {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private GeoUtils() {
    }

    public static Point point(double lat, double lng) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
    }
}
