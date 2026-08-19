package com.foodloop.food.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodListingRepository extends JpaRepository<FoodListing, UUID> {

    /**
     * Radius search over approx_location (§12, §33 — public discovery uses
     * the privacy-degraded point, never the exact one) via PostGIS
     * ST_DWithin, which is what makes this sub-linear against the GIST
     * index rather than a full-table distance scan (spec explicitly
     * forbids computing this in Java). category/dietaryType are optional —
     * the "IS NULL OR" pattern keeps this one query instead of building it
     * dynamically. Native SQL, not JPQL: PostGIS functions need no
     * Hibernate Spatial function-registry setup this way, and the raw SQL
     * is easier to verify directly against psql.
     */
    @Query(
            value = """
                SELECT * FROM food.food_listing
                WHERE tenant_id = :tenantId
                  AND status = 'AVAILABLE'
                  AND ST_DWithin(approx_location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
                  AND (:category IS NULL OR food_category = :category)
                  AND (:dietaryType IS NULL OR :dietaryType = ANY(dietary_types))
                ORDER BY ST_Distance(approx_location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)
                """,
            countQuery = """
                SELECT count(*) FROM food.food_listing
                WHERE tenant_id = :tenantId
                  AND status = 'AVAILABLE'
                  AND ST_DWithin(approx_location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
                  AND (:category IS NULL OR food_category = :category)
                  AND (:dietaryType IS NULL OR :dietaryType = ANY(dietary_types))
                """,
            nativeQuery = true)
    Page<FoodListing> searchNearby(
            @Param("tenantId") UUID tenantId,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("category") String category,
            @Param("dietaryType") String dietaryType,
            Pageable pageable);
}
