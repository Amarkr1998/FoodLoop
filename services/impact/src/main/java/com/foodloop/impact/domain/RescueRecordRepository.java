package com.foodloop.impact.domain;

import java.util.List;
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

    /**
     * Advanced analytics (spec Phase 12): per-category and per-month slices
     * of the same aggregates above. Native queries (rather than JPQL's
     * portable-but-limited function() escape hatch) for date_trunc and for
     * ordering by a SELECT-list alias, both plain Postgres SQL. RLS already
     * scopes every one of these to the caller's tenant, same as the summarize*
     * queries above.
     */
    @Query(value = """
            SELECT food_category AS foodCategory,
                   COUNT(*) AS rescueCount,
                   COALESCE(SUM(estimated_kg_saved), 0) AS totalKgSaved,
                   COALESCE(SUM(estimated_co2_saved_kg), 0) AS totalCo2SavedKg
            FROM impact.rescue_record
            WHERE donor_org_id = :orgId
            GROUP BY food_category
            ORDER BY totalKgSaved DESC
            """, nativeQuery = true)
    List<CategoryImpactRow> categoryBreakdownByDonorOrg(@Param("orgId") UUID orgId);

    @Query(value = """
            SELECT food_category AS foodCategory,
                   COUNT(*) AS rescueCount,
                   COALESCE(SUM(estimated_kg_saved), 0) AS totalKgSaved,
                   COALESCE(SUM(estimated_co2_saved_kg), 0) AS totalCo2SavedKg
            FROM impact.rescue_record
            GROUP BY food_category
            ORDER BY totalKgSaved DESC
            """, nativeQuery = true)
    List<CategoryImpactRow> categoryBreakdownAll();

    @Query(value = """
            SELECT date_trunc('month', completed_at)::date AS month,
                   COUNT(*) AS rescueCount,
                   COALESCE(SUM(estimated_kg_saved), 0) AS totalKgSaved,
                   COALESCE(SUM(estimated_co2_saved_kg), 0) AS totalCo2SavedKg
            FROM impact.rescue_record
            WHERE donor_org_id = :orgId
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<MonthlyImpactRow> monthlyTrendByDonorOrg(@Param("orgId") UUID orgId);

    @Query(value = """
            SELECT date_trunc('month', completed_at)::date AS month,
                   COUNT(*) AS rescueCount,
                   COALESCE(SUM(estimated_kg_saved), 0) AS totalKgSaved,
                   COALESCE(SUM(estimated_co2_saved_kg), 0) AS totalCo2SavedKg
            FROM impact.rescue_record
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<MonthlyImpactRow> monthlyTrendAll();
}
