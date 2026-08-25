"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApiClient } from "@/lib/use-api-client";
import { ApiError, type MatchingSchemas } from "@foodloop/api-client";

export type MatchCandidate = MatchingSchemas["schemas"]["MatchCandidateResponse"];
export type CreateMatchProposalRequest = MatchingSchemas["schemas"]["CreateMatchProposalRequest"];

/** Real data — GET /api/v1/matches/candidates?foodListingId=&radiusKm= (distance + score are computed server-side, not invented). */
export function useMatchCandidates(foodListingId: string, radiusKm = 15) {
  const client = useApiClient();
  return useQuery({
    queryKey: ["matching", "candidates", foodListingId, radiusKm],
    queryFn: () => client.get<MatchCandidate[]>("/api/v1/matches/candidates", { params: { foodListingId, radiusKm } }),
    enabled: !!foodListingId,
  });
}

/**
 * "Accepting" a candidate, in terms of what the backend actually supports
 * today, is creating a match proposal for that receiver — there is no
 * separate /matches/{id}/accept endpoint (packages/shared-contracts/openapi/matching.yaml
 * only has GET /matches, POST /matches, GET /matches/candidates). Rejecting
 * a candidate has no backend counterpart either since nothing was
 * committed yet — the UI simply doesn't select that candidate.
 */
export function useAcceptMatchCandidate() {
  const client = useApiClient();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: CreateMatchProposalRequest) => client.post("/api/v1/matches", body),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["donations", "matches", variables.foodListingId] });
      toast.success("Match proposal created.");
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : "Couldn't create the match proposal."),
  });
}
