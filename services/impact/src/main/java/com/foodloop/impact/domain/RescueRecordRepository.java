package com.foodloop.impact.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RescueRecordRepository extends JpaRepository<RescueRecord, UUID> {

    boolean existsByPickupTaskId(UUID pickupTaskId);

    /** RLS already scopes this to the caller's tenant — no explicit tenant_id predicate needed beyond that. */
    @Query("""
            SELECT new com.foodloop.impact.domain.ImpactSummary(
                COUNT(r), COALESCE(SUM(r.estimatedKgSaved), 0), COALESCE(SUM(r.estimatedCo2SavedKg), 0))
            FROM RescueRecord r
            WHERE r.donorUserId = :userId
            """)
    ImpactSummary summarizeAsDonor(@Param("userId") UUID userId);

    @Query("""
            SELECT new com.foodloop.impact.domain.ImpactSummary(
                COUNT(r), COALESCE(SUM(r.estimatedKgSaved), 0), COALESCE(SUM(r.estimatedCo2SavedKg), 0))
            FROM RescueRecord r
            WHERE r.receiverUserId = :userId
            """)
    ImpactSummary summarizeAsReceiver(@Param("userId") UUID userId);

    @Query("""
            SELECT new com.foodloop.impact.domain.ImpactSummary(
                COUNT(r), COALESCE(SUM(r.estimatedKgSaved), 0), COALESCE(SUM(r.estimatedCo2SavedKg), 0))
            FROM RescueRecord r
            WHERE r.donorOrgId = :orgId
            """)
    ImpactSummary summarizeByDonorOrg(@Param("orgId") UUID orgId);

    /** The "community dashboard" (spec Phase 11) — every completed rescue in the caller's tenant. */
    @Query("""
            SELECT new com.foodloop.impact.domain.ImpactSummary(
                COUNT(r), COALESCE(SUM(r.estimatedKgSaved), 0), COALESCE(SUM(r.estimatedCo2SavedKg), 0))
            FROM RescueRecord r
            """)
    ImpactSummary summarizeAll();
}
