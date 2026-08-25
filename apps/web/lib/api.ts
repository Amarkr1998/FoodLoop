import { getServerSession } from "next-auth";
import { createApiClient } from "@foodloop/api-client";
import { authOptions } from "./auth";

/** Server Components/Actions only — uses the signed-in user's bearer token against the gateway. */
export async function getApiClient() {
  const session = await getServerSession(authOptions);
  if (session?.accessToken) console.log("[getApiClient] TOKEN:", session.accessToken);
  return createApiClient({
    baseUrl: process.env.API_GATEWAY_URL ?? "http://localhost:8080",
    getAccessToken: async () => session?.accessToken ?? null,
  });
}

export async function getSession() {
  return getServerSession(authOptions);
}
