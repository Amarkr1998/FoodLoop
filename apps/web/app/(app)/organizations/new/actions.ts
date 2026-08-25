"use server";

import { redirect } from "next/navigation";
import { ApiError } from "@foodloop/api-client";
import { getApiClient } from "@/lib/api";

export interface CreateOrgState {
  error?: string;
}

export async function createOrganization(_prev: CreateOrgState, formData: FormData): Promise<CreateOrgState> {
  const client = await getApiClient();
  const name = String(formData.get("name") ?? "");
  const type = String(formData.get("type") ?? "DONOR_RESTAURANT");

  try {
    const org = await client.post<{ id: string }>("/api/v1/organizations", { name, type });
    redirect(`/organizations/${org.id}`);
  } catch (err) {
    if (err instanceof ApiError) {
      return { error: `${err.code}: ${err.message}` };
    }
    throw err;
  }
}
