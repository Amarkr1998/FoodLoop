package com.foodloop.pickup.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VolunteerProfileRepository extends JpaRepository<VolunteerProfile, UUID> {

    Optional<VolunteerProfile> findByUserId(UUID userId);

    /** The Pickup Agent's findAvailableVolunteers tool (spec §20) — mirrors PickupTaskRepository#searchNearbyUnassigned's pattern, reversed. */
    @Query(
            value = """
                SELECT * FROM pickup.volunteer_profile
                WHERE tenant_id = :tenantId
                  AND available = true
                  AND current_location IS NOT NULL
                  AND ST_DWithin(current_location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
                ORDER BY ST_Distance(current_location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)
                """,
            countQuery = """
                SELECT count(*) FROM pickup.volunteer_profile
                WHERE tenant_id = :tenantId
                  AND available = true
                  AND current_location IS NOT NULL
                  AND ST_DWithin(current_location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
                """,
            nativeQuery = true)
    Page<VolunteerProfile> searchNearbyAvailable(
            @Param("tenantId") UUID tenantId,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            Pageable pageable);
}
