"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApiClient } from "@/lib/use-api-client";
import { ApiError, type PickupSchemas } from "@foodloop/api-client";

export type PickupTask = PickupSchemas["schemas"]["PickupTaskResponse"];
type PagePickupTask = PickupSchemas["schemas"]["PagePickupTaskResponse"];

/** Real data — GET /api/v1/pickups/available?lat=&lng=&radiusKm= (unassigned tasks awaiting a volunteer, geospatially scoped). */
export function useAvailablePickups(lat: number, lng: number, radiusKm = 25) {
  const client = useApiClient();
  return useQuery({
    queryKey: ["pickups", "available", lat, lng, radiusKm],
    queryFn: () => client.get<PagePickupTask>("/api/v1/pickups/available", { params: { lat, lng, radiusKm } }),
  });
}

/** Real data — GET /api/v1/pickups/delayed?asOf= (tasks past their scheduled window, still not completed). */
export function useDelayedPickups() {
  const client = useApiClient();
  return useQuery({
    queryKey: ["pickups", "delayed"],
    queryFn: () => client.get<PickupTask[]>("/api/v1/pickups/delayed", { params: { asOf: new Date().toISOString() } }),
    refetchInterval: 60_000,
  });
}

export function usePickup(id: string) {
  const client = useApiClient();
  return useQuery({
    queryKey: ["pickups", "detail", id],
    queryFn: () => client.get<PickupTask>(`/api/v1/pickups/${id}`),
    enabled: !!id,
  });
}

function useMutationErrorToast() {
  return (err: unknown) => toast.error(err instanceof ApiError ? err.message : "Something went wrong.");
}

export function useClaimPickup() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const onError = useMutationErrorToast();
  return useMutation({
    mutationFn: (id: string) => client.post<PickupTask>(`/api/v1/pickups/${id}/claim`, undefined, { idempotencyKey: crypto.randomUUID() }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["pickups"] });
      toast.success("Pickup claimed.");
    },
    onError,
  });
}

export function useMarkEnRoute() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const onError = useMutationErrorToast();
  return useMutation({
    mutationFn: (id: string) => client.post<PickupTask>(`/api/v1/pickups/${id}/en-route`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pickups"] }),
    onError,
  });
}

export function useMarkArrived() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const onError = useMutationErrorToast();
  return useMutation({
    mutationFn: (id: string) => client.post<PickupTask>(`/api/v1/pickups/${id}/arrived`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pickups"] }),
    onError,
  });
}

export function useCompletePickup() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const onError = useMutationErrorToast();
  return useMutation({
    mutationFn: (id: string) => client.post<PickupTask>(`/api/v1/pickups/${id}/complete`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["pickups"] });
      toast.success("Pickup completed.");
    },
    onError,
  });
}
