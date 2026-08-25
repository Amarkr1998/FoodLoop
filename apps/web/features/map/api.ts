"use client";

import { useQuery } from "@tanstack/react-query";
import { useApiClient } from "@/lib/use-api-client";
import type { FoodSchemas, PickupSchemas, TenantSchemas } from "@foodloop/api-client";

type FoodListing = FoodSchemas["schemas"]["FoodListingResponse"];
type PageFoodListing = FoodSchemas["schemas"]["PageFoodListingResponse"];
type PickupTask = PickupSchemas["schemas"]["PickupTaskResponse"];
type PagePickupTask = PickupSchemas["schemas"]["PagePickupTaskResponse"];
type Organization = TenantSchemas["schemas"]["OrganizationResponse"];
type PageOrganization = TenantSchemas["schemas"]["PageOrganizationResponse"];

/**
 * No WebSocket/SSE push channel exists in any backend service yet — every
 * layer here polls on an interval instead, which is an honest, working
 * approximation of "real-time" rather than a fabricated push connection.
 */
const REALTIME_POLL_MS = 20_000;

export function useMapDonors(lat: number, lng: number, radiusKm: number) {
  const client = useApiClient();
  return useQuery({
    queryKey: ["map", "donors", lat, lng, radiusKm],
    queryFn: () => client.get<PageFoodListing>("/api/v1/food-listings", { params: { lat, lng, radiusKm } }),
    refetchInterval: REALTIME_POLL_MS,
  });
}

export function useMapPickups(lat: number, lng: number, radiusKm: number) {
  const client = useApiClient();
  return useQuery({
    queryKey: ["map", "pickups", lat, lng, radiusKm],
    queryFn: () => client.get<PagePickupTask>("/api/v1/pickups/available", { params: { lat, lng, radiusKm } }),
    refetchInterval: REALTIME_POLL_MS,
  });
}

export function useMapOrganizations(lat: number, lng: number, radiusKm: number, type?: string) {
  const client = useApiClient();
  return useQuery({
    queryKey: ["map", "organizations", lat, lng, radiusKm, type],
    queryFn: () => client.get<PageOrganization>("/api/v1/organizations", { params: { lat, lng, radiusKm, type } }),
    refetchInterval: REALTIME_POLL_MS,
  });
}

export type { FoodListing, PickupTask, Organization };
