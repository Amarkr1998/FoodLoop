"use client";

import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApiClient } from "@/lib/use-api-client";
import { ApiError, type AiOrchestrationSchemas } from "@foodloop/api-client";

export type AssessTrustRiskResponse = AiOrchestrationSchemas["schemas"]["AssessTrustRiskResponse"];
export type SuggestMatchResponse = AiOrchestrationSchemas["schemas"]["SuggestMatchResponse"];
export type AnalyzeFoodListingResponse = AiOrchestrationSchemas["schemas"]["AnalyzeFoodListingResponse"];
export type AgentRunResponse = AiOrchestrationSchemas["schemas"]["AgentRunResponse"];

/** Real data — POST /api/v1/ai/trust/assess triggers the Trust & Safety Agent for a given user. */
export function useAssessTrustRisk() {
  const client = useApiClient();
  return useMutation({
    mutationFn: (targetUserId: string) =>
      client.post<AssessTrustRiskResponse>("/api/v1/ai/trust/assess", { targetUserId }),
    onError: (err) => toast.error(err instanceof ApiError ? err.message : "Trust assessment failed."),
  });
}

/** Real data — POST /api/v1/ai/matching/suggest triggers the Matching Agent for a food listing. */
export function useSuggestMatch() {
  const client = useApiClient();
  return useMutation({
    mutationFn: (foodListingId: string) =>
      client.post<SuggestMatchResponse>("/api/v1/ai/matching/suggest", { foodListingId }),
    onError: (err) => toast.error(err instanceof ApiError ? err.message : "Match suggestion failed."),
  });
}

/** Real data — POST /api/v1/ai/food-listings/{id}/analyze triggers the Food Intelligence Agent. */
export function useAnalyzeFoodListing() {
  const client = useApiClient();
  return useMutation({
    mutationFn: (foodListingId: string) =>
      client.post<AnalyzeFoodListingResponse>(`/api/v1/ai/food-listings/${foodListingId}/analyze`),
    onError: (err) => toast.error(err instanceof ApiError ? err.message : "Food listing analysis failed."),
  });
}

/** Real data — POST /api/v1/ai/agent-runs/{id}/escalate/resolve approves or rejects an escalated agent decision. */
export function useResolveEscalation() {
  const client = useApiClient();
  return useMutation({
    mutationFn: ({ agentRunId, approve }: { agentRunId: string; approve: boolean }) =>
      client.post<AgentRunResponse>(`/api/v1/ai/agent-runs/${agentRunId}/escalate/resolve`, { approve }),
    onSuccess: (data) => toast.success(`Escalation ${data.status?.toLowerCase() ?? "resolved"}.`),
    onError: (err) => toast.error(err instanceof ApiError ? err.message : "Failed to resolve escalation."),
  });
}
