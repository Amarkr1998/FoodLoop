"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApiClient } from "@/lib/use-api-client";
import { ApiError, type TenantSchemas } from "@foodloop/api-client";

export type Tenant = TenantSchemas["schemas"]["TenantResponse"];
export type Organization = TenantSchemas["schemas"]["OrganizationResponse"];
export type PageOrganization = TenantSchemas["schemas"]["PageOrganizationResponse"];
export type OrgMember = TenantSchemas["schemas"]["OrgMemberResponse"];

/** Real data — GET /api/v1/tenants (active tenants, platform-wide). */
export function useTenants() {
  const client = useApiClient();
  return useQuery({
    queryKey: ["admin", "tenants"],
    queryFn: () => client.get<Tenant[]>("/api/v1/tenants"),
  });
}

/** Real data — GET /api/v1/organizations. Geospatial "nearby" search is the only listing this endpoint supports (no admin-wide org list exists), so a wide radius from the viewer's location approximates it. */
export function useOrganizationsNearby(lat: number, lng: number, radiusKm = 200) {
  const client = useApiClient();
  return useQuery({
    queryKey: ["admin", "organizations", lat, lng, radiusKm],
    queryFn: () =>
      client.get<PageOrganization>("/api/v1/organizations", { params: { lat, lng, radiusKm, size: 50 } }),
  });
}

/** Real data — GET /api/v1/organizations/{id}/members. */
export function useOrgMembers(orgId: string) {
  const client = useApiClient();
  return useQuery({
    queryKey: ["admin", "organizations", orgId, "members"],
    queryFn: () => client.get<OrgMember[]>(`/api/v1/organizations/${orgId}/members`),
    enabled: !!orgId,
  });
}

/** Real data — POST /api/v1/organizations/{id}/members. */
export function useAddOrgMember() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ orgId, userId, role }: { orgId: string; userId: string; role: "ORG_ADMIN" | "MEMBER" }) =>
      client.post<OrgMember>(`/api/v1/organizations/${orgId}/members`, { userId, role }),
    onSuccess: (_data, { orgId }) => {
      queryClient.invalidateQueries({ queryKey: ["admin", "organizations", orgId, "members"] });
      toast.success("Member added.");
    },
    onError: (err) => toast.error(err instanceof ApiError ? err.message : "Failed to add member."),
  });
}
