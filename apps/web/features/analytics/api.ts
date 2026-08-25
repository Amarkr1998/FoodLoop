"use client";

import { useQuery } from "@tanstack/react-query";
import { useApiClient } from "@/lib/use-api-client";
import type { ImpactSchemas } from "@foodloop/api-client";

type MonthlyImpact = ImpactSchemas["schemas"]["MonthlyImpactResponse"];
type CategoryImpact = ImpactSchemas["schemas"]["CategoryImpactResponse"];
type ImpactSummary = ImpactSchemas["schemas"]["ImpactSummaryResponse"];

/** Real data — GET /api/v1/impact/community, /community/trend, /community/breakdown (Phase 11/12 analytics endpoints). */
export function useCommunitySummary() {
  const client = useApiClient();
  return useQuery({
    queryKey: ["analytics", "community", "summary"],
    queryFn: () => client.get<ImpactSummary>("/api/v1/impact/community"),
  });
}

export function useCommunityTrend() {
  const client = useApiClient();
  return useQuery({
    queryKey: ["analytics", "community", "trend"],
    queryFn: () => client.get<MonthlyImpact[]>("/api/v1/impact/community/trend"),
  });
}

export function useCommunityBreakdown() {
  const client = useApiClient();
  return useQuery({
    queryKey: ["analytics", "community", "breakdown"],
    queryFn: () => client.get<CategoryImpact[]>("/api/v1/impact/community/breakdown"),
  });
}
