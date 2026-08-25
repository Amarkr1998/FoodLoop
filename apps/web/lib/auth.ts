import type { NextAuthOptions } from "next-auth";
import type { JWT } from "next-auth/jwt";
import KeycloakProvider from "next-auth/providers/keycloak";
import { decodeJwtPayload, type FoodLoopTokenClaims } from "@foodloop/auth";

const KEYCLOAK_ISSUER = process.env.KEYCLOAK_ISSUER_URI ?? "http://localhost:8081/realms/foodloop";
const KEYCLOAK_WEB_CLIENT_ID = process.env.KEYCLOAK_WEB_CLIENT_ID ?? "foodloop-web";

function applyClaims(token: JWT, accessToken: string) {
  token.accessToken = accessToken;
  const claims = decodeJwtPayload<FoodLoopTokenClaims>(accessToken);
  token.tenantId = claims?.tenant_id;
  token.roles = claims?.realm_access?.roles ?? [];
}

/**
 * Keycloak access tokens here are short-lived (realm default), and the
 * initial sign-in only fires the jwt callback's `account` branch once —
 * without this, every request after the token's first ~15 minutes would
 * silently keep sending the same now-expired token forever (discovered
 * live: gateway rejected with "Jwt expired", not a wiring bug in the
 * token's presence/shape, which the diagnostic logging had already ruled
 * out). Public client, no secret, per the realm config.
 */
async function refreshAccessToken(token: JWT): Promise<JWT> {
  try {
    const response = await fetch(`${KEYCLOAK_ISSUER}/protocol/openid-connect/token`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        client_id: KEYCLOAK_WEB_CLIENT_ID,
        refresh_token: token.refreshToken as string,
      }),
    });
    const refreshed = await response.json();
    if (!response.ok) throw refreshed;

    applyClaims(token, refreshed.access_token);
    token.accessTokenExpiresAt = Date.now() + refreshed.expires_in * 1000;
    token.refreshToken = refreshed.refresh_token ?? token.refreshToken;
    delete token.error;
    return token;
  } catch (err) {
    console.log("[refreshAccessToken] failed:", err);
    return { ...token, error: "RefreshAccessTokenError" };
  }
}

export const authOptions: NextAuthOptions = {
  providers: [
    KeycloakProvider({
      clientId: KEYCLOAK_WEB_CLIENT_ID,
      // Public PKCE client (infrastructure/docker/keycloak/foodloop-realm.json)
      // — no secret. NextAuth adds the PKCE challenge automatically once no
      // clientSecret is configured.
      clientSecret: "",
      issuer: KEYCLOAK_ISSUER,
      checks: ["pkce", "state"],
    }),
  ],
  session: { strategy: "jwt" },
  callbacks: {
    async jwt({ token, account }) {
      if (account?.access_token) {
        console.log("[jwt] initial sign-in; refresh_token present:", !!account.refresh_token, "expires_in:", account.expires_in);
        applyClaims(token, account.access_token);
        token.accessTokenExpiresAt = Date.now() + (account.expires_in as number) * 1000;
        token.refreshToken = account.refresh_token;
        token.idToken = account.id_token;
        return token;
      }

      const msUntilExpiry = typeof token.accessTokenExpiresAt === "number" ? token.accessTokenExpiresAt - Date.now() : undefined;
      console.log("[jwt] subsequent call; msUntilExpiry:", msUntilExpiry, "hasRefreshToken:", !!token.refreshToken, "hasError:", !!token.error);

      // Refresh a little before actual expiry to avoid a request racing the boundary.
      if (typeof token.accessTokenExpiresAt === "number" && Date.now() < token.accessTokenExpiresAt - 10_000) {
        return token;
      }
      if (!token.refreshToken) {
        console.log("[jwt] no refreshToken stored, cannot refresh");
        return token;
      }
      const refreshed = await refreshAccessToken(token);
      console.log("[jwt] refresh result error:", refreshed.error);
      return refreshed;
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken as string | undefined;
      session.idToken = token.idToken as string | undefined;
      session.tenantId = token.tenantId as string | undefined;
      session.roles = (token.roles as string[] | undefined) ?? [];
      session.error = token.error as string | undefined;
      return session;
    },
  },
  pages: {
    signIn: "/login",
  },
};
