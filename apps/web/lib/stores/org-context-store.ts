import { create } from "zustand";
import { persist } from "zustand/middleware";

/**
 * Which organization the signed-in user is currently "acting as" — a user
 * can belong to multiple orgs (see tenant service's OrgMember), so this is
 * genuine client state (which one is active right now), distinct from the
 * list of orgs the user belongs to (server data, fetched via TanStack
 * Query). Persisted so a refresh doesn't silently switch context back to
 * the default org mid-task.
 */
interface OrgContextState {
  activeOrgId: string | null;
  setActiveOrgId: (orgId: string | null) => void;
}

export const useOrgContextStore = create<OrgContextState>()(
  persist(
    (set) => ({
      activeOrgId: null,
      setActiveOrgId: (orgId) => set({ activeOrgId: orgId }),
    }),
    { name: "foodloop-active-org" },
  ),
);
