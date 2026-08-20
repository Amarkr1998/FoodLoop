package com.foodloop.tenant.domain;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/** Same SRID-4326 point construction as services/food's GeoUtils — no jitter here, see Organization's Javadoc. */
public final class GeoUtils {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private GeoUtils() {
    }

    public static Point point(double lat, double lng) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
    }
}
