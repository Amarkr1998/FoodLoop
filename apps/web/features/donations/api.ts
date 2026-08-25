"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApiClient } from "@/lib/use-api-client";
import { ApiError, type FoodSchemas, type MatchingSchemas } from "@foodloop/api-client";

export type FoodListing = FoodSchemas["schemas"]["FoodListingResponse"];
export type PageFoodListing = FoodSchemas["schemas"]["PageFoodListingResponse"];
export type CreateFoodListingRequest = FoodSchemas["schemas"]["CreateFoodListingRequest"];

const donationsKey = {
  all: ["donations"] as const,
  search: (params: SearchParams) => ["donations", "search", params] as const,
  detail: (id: string) => ["donations", "detail", id] as const,
};

export interface SearchParams {
  [key: string]: string | number | boolean | undefined;
  lat: number;
  lng: number;
  radiusKm?: number;
  category?: string;
  dietaryType?: string;
  page?: number;
  size?: number;
}

export function useDonations(params: SearchParams) {
  const client = useApiClient();
  return useQuery({
    queryKey: donationsKey.search(params),
    queryFn: () => client.get<PageFoodListing>("/api/v1/food-listings", { params }),
  });
}

export function useDonation(id: string) {
  const client = useApiClient();
  return useQuery({
    queryKey: donationsKey.detail(id),
    queryFn: () => client.get<FoodListing>(`/api/v1/food-listings/${id}`),
    enabled: !!id,
  });
}

export type MatchProposal = MatchingSchemas["schemas"]["MatchProposalResponse"];

export function useDonationMatches(foodListingId: string) {
  const client = useApiClient();
  return useQuery({
    queryKey: ["donations", "matches", foodListingId],
    queryFn: () => client.get<MatchProposal[]>("/api/v1/matches", { params: { foodListingId } }),
    enabled: !!foodListingId,
  });
}

function useMutationErrorToast() {
  return (err: unknown) => {
    toast.error(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
  };
}

export function useCreateDonation() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const onError = useMutationErrorToast();

  return useMutation({
    mutationFn: (body: CreateFoodListingRequest) => client.post<FoodListing>("/api/v1/food-listings", body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: donationsKey.all });
      toast.success("Donation created as a draft.");
    },
    onError,
  });
}

export function usePublishDonation() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const onError = useMutationErrorToast();

  return useMutation({
    mutationFn: (id: string) => client.post<FoodListing>(`/api/v1/food-listings/${id}/publish`),
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: donationsKey.detail(id) });
      queryClient.invalidateQueries({ queryKey: donationsKey.all });
      toast.success("Donation published — now visible to receivers.");
    },
    onError,
  });
}

export function useCancelDonation() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const onError = useMutationErrorToast();

  return useMutation({
    mutationFn: (id: string) => client.post<FoodListing>(`/api/v1/food-listings/${id}/cancel`),
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: donationsKey.detail(id) });
      queryClient.invalidateQueries({ queryKey: donationsKey.all });
      toast.success("Donation cancelled.");
    },
    onError,
  });
}

export function useClaimDonation() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const onError = useMutationErrorToast();

  return useMutation({
    mutationFn: ({ id, receiverOrgId }: { id: string; receiverOrgId: string }) =>
      client.post(`/api/v1/food-listings/${id}/claim`, { receiverOrgId }, { idempotencyKey: crypto.randomUUID() }),
    onSuccess: (_data, { id }) => {
      queryClient.invalidateQueries({ queryKey: donationsKey.detail(id) });
      toast.success("Claim submitted.");
    },
    onError,
  });
}
