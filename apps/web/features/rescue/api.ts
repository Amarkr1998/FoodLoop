"use client";

import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApiClient } from "@/lib/use-api-client";
import { ApiError, type AiOrchestrationSchemas } from "@foodloop/api-client";

export type RescueCheckResponse = AiOrchestrationSchemas["schemas"]["RescueCheckResponse"];

/** Real data — POST /api/v1/ai/rescue/check triggers the Food Rescue Agent's own logic for this listing. */
export function useRunRescueCheck() {
  const client = useApiClient();
  return useMutation({
    mutationFn: ({ foodListingId, threshold }: { foodListingId: string; threshold: "T_MINUS_4H" | "T_MINUS_1H" }) =>
      client.post<RescueCheckResponse>("/api/v1/ai/rescue/check", { foodListingId, threshold }),
    onError: (err) => toast.error(err instanceof ApiError ? err.message : "Rescue check failed."),
  });
}
