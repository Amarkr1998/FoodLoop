import type { OidcClientConfig } from "./config";

/**
 * Ready-made client configs for the two frontend Keycloak clients
 * (infrastructure/docker/keycloak/foodloop-realm.json) — both public, PKCE
 * (S256) required, no secret. `issuerUri` is deliberately left to the
 * caller (from KEYCLOAK_ISSUER_URI / EXPO_PUBLIC_KEYCLOAK_ISSUER_URI) since
 * it differs between local Docker Compose and any deployed environment.
 */
export function webOidcConfig(issuerUri: string): OidcClientConfig {
  return {
    issuerUri,
    clientId: "foodloop-web",
    redirectUri: "http://localhost:3000/api/auth/callback/keycloak",
    scopes: ["openid", "profile", "email"],
  };
}

export function mobileOidcConfig(issuerUri: string): OidcClientConfig {
  return {
    issuerUri,
    clientId: "foodloop-mobile",
    // Matches the realm's registered redirect URIs (foodloop://*, exp://*):
    // a custom scheme for standalone/dev-client builds, Expo's proxy scheme
    // when running in Expo Go. apps/mobile's actual AuthSession call picks
    // between them via expo-auth-session's makeRedirectUri() at runtime.
    redirectUri: "foodloop://auth/callback",
    scopes: ["openid", "profile", "email"],
  };
}
