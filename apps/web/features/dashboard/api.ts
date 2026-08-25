"use client";

import { useQuery } from "@tanstack/react-query";
import { useApiClient } from "@/lib/use-api-client";
import type { ImpactSchemas, FoodSchemas } from "@foodloop/api-client";

type ImpactSummary = ImpactSchemas["schemas"]["ImpactSummaryResponse"];
type MonthlyImpact = ImpactSchemas["schemas"]["MonthlyImpactResponse"];
type FoodListing = FoodSchemas["schemas"]["FoodListingResponse"];

/** Real data — GET /api/v1/impact/community (community-wide rescue/kg/CO2 totals). */
export function useCommunityImpact() {
  const client = useApiClient();
  return useQuery({
    queryKey: ["dashboard", "impact", "community"],
    queryFn: () => client.get<ImpactSummary>("/api/v1/impact/community"),
  });
}

/** Real data — GET /api/v1/impact/community/trend (monthly rescue/kg/CO2 series, powers the trend chart). */
export function useCommunityImpactTrend() {
  const client = useApiClient();
  return useQuery({
    queryKey: ["dashboard", "impact", "community", "trend"],
    queryFn: () => client.get<MonthlyImpact[]>("/api/v1/impact/community/trend"),
  });
}

/** Real data — GET /api/v1/food-listings/expiring?withinMinutes=1440 (next 24h). */
export function useExpiringFood(withinMinutes = 1440) {
  const client = useApiClient();
  return useQuery({
    queryKey: ["dashboard", "food", "expiring", withinMinutes],
    queryFn: () => client.get<FoodListing[]>("/api/v1/food-listings/expiring", { params: { withinMinutes } }),
  });
}
