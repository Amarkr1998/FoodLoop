"use client";

import { useMemo } from "react";
import { useSession } from "next-auth/react";
import { createApiClient } from "@foodloop/api-client";

/** Client Components/hooks only — server-side data fetching should use lib/api.ts's getApiClient() instead. */
export function useApiClient() {
  const { data: session } = useSession();

  return useMemo(
    () =>
      createApiClient({
        baseUrl: process.env.NEXT_PUBLIC_API_GATEWAY_URL ?? "http://localhost:8080",
        getAccessToken: () => session?.accessToken ?? null,
      }),
    [session?.accessToken],
  );
}
