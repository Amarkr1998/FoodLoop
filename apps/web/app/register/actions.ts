"use server";

import { redirect } from "next/navigation";
import { ApiError, createApiClient } from "@foodloop/api-client";

// Single-region MVP default tenant, seeded by the tenant service
// (services/tenant/.../V2__seed_default_tenant.sql) — matches
// docs/architecture/08-phases-mvp-risks.md's single-tenant scope.
const DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000001";

export interface RegisterState {
  error?: string;
}

export async function registerUser(_prev: RegisterState, formData: FormData): Promise<RegisterState> {
  const email = String(formData.get("email") ?? "");
  const password = String(formData.get("password") ?? "");
  const displayName = String(formData.get("displayName") ?? "");

  const client = createApiClient({
    baseUrl: process.env.API_GATEWAY_URL ?? "http://localhost:8080",
    // /api/v1/auth/register is unauthenticated by design (no account exists yet).
    getAccessToken: () => null,
  });

  try {
    await client.post("/api/v1/auth/register", {
      tenantId: DEFAULT_TENANT_ID,
      email,
      password,
      displayName,
      locale: "en",
    });
  } catch (err) {
    if (err instanceof ApiError) {
      return { error: err.message };
    }
    return { error: "Registration failed. Please try again." };
  }

  redirect("/login?registered=1");
}
