"use client";

import { useEffect } from "react";
import { useOrgContextStore } from "@/lib/stores/org-context-store";

/** Activates this org as the current org-context the moment its detail page is viewed (e.g. right after creation). */
export function SetActiveOrg({ orgId }: { orgId: string }) {
  const setActiveOrgId = useOrgContextStore((s) => s.setActiveOrgId);
  useEffect(() => {
    setActiveOrgId(orgId);
  }, [orgId, setActiveOrgId]);
  return null;
}
