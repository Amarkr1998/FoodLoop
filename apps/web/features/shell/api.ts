"use client";

import { useQuery } from "@tanstack/react-query";
import { useApiClient } from "@/lib/use-api-client";
import { useOrgContextStore } from "@/lib/stores/org-context-store";
import type { TenantSchemas } from "@foodloop/api-client";

type OrganizationResponse = TenantSchemas["schemas"]["OrganizationResponse"];

/**
 * There is no "list organizations I belong to" endpoint in tenant service
 * yet (GET /api/v1/organizations is the geospatial nearby-search used by
 * the Matching Agent, not a per-user membership list — see
 * packages/shared-contracts/openapi/tenant.yaml). Per the brief: don't
 * invent an endpoint that doesn't exist. Until that's built, this fetches
 * the ONE org already known via org-context-store (set when the user
 * creates/joins an org) as a real, single-item result — the org switcher
 * UI is ready for a real multi-org list the moment that endpoint exists,
 * it just can't offer more than one org to switch to today.
 */
export function useMyOrganizations() {
  const client = useApiClient();
  const activeOrgId = useOrgContextStore((s) => s.activeOrgId);

  return useQuery({
    queryKey: ["shell", "my-organizations", activeOrgId],
    queryFn: async (): Promise<OrganizationResponse[]> => {
      if (!activeOrgId) return [];
      const org = await client.get<OrganizationResponse>(`/api/v1/organizations/${activeOrgId}`);
      return [org];
    },
    enabled: !!activeOrgId,
  });
}
