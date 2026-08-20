package com.foodloop.tenant.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * The Matching Agent's nearby-receiver search (Phase 7). Receiver-capable
     * types are hardcoded in the IN clause — never all org types — so this
     * query structurally can't return a donor org no matter what {@code type}
     * filter is passed; same ST_DWithin/GIST-index pattern as
     * services/food's FoodListingRepository#searchNearby.
     */
    @Query(
            value = """
                SELECT * FROM tenant.organization
                WHERE tenant_id = :tenantId
                  AND type IN ('NGO', 'FOOD_BANK', 'CORPORATE', 'INDIVIDUAL')
                  AND location IS NOT NULL
                  AND ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
                  AND (:type IS NULL OR type = :type)
                ORDER BY ST_Distance(location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)
                """,
            countQuery = """
                SELECT count(*) FROM tenant.organization
                WHERE tenant_id = :tenantId
                  AND type IN ('NGO', 'FOOD_BANK', 'CORPORATE', 'INDIVIDUAL')
                  AND location IS NOT NULL
                  AND ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
                  AND (:type IS NULL OR type = :type)
                """,
            nativeQuery = true)
    Page<Organization> searchNearbyReceivers(
            @Param("tenantId") UUID tenantId,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("type") String type,
            Pageable pageable);
}
