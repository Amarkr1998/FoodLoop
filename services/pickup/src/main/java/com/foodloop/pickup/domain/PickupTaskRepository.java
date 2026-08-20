package com.foodloop.pickup.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PickupTaskRepository extends JpaRepository<PickupTask, UUID> {

    Optional<PickupTask> findByClaimId(UUID claimId);

    /** The Pickup Agent's scheduled sweep (spec §20) — assigned tasks whose scheduled window has already passed without completing. */
    List<PickupTask> findByTenantIdAndStatusInAndScheduledWindowEndBefore(
            UUID tenantId, List<PickupStatus> statuses, Instant cutoff);

    /**
     * What a volunteer browses to find a task to claim (spec Phase 10) — no
     * automatic/AI assignment in this phase, same self-service pattern as
     * Food's own nearby search (FoodListingRepository#searchNearby).
     */
    @Query(
            value = """
                SELECT * FROM pickup.pickup_task
                WHERE tenant_id = :tenantId
                  AND status = 'UNASSIGNED'
                  AND ST_DWithin(pickup_location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
                ORDER BY ST_Distance(pickup_location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)
                """,
            countQuery = """
                SELECT count(*) FROM pickup.pickup_task
                WHERE tenant_id = :tenantId
                  AND status = 'UNASSIGNED'
                  AND ST_DWithin(pickup_location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
                """,
            nativeQuery = true)
    Page<PickupTask> searchNearbyUnassigned(
            @Param("tenantId") UUID tenantId,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            Pageable pageable);
}
