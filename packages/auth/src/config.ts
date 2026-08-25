/**
 * Derives the standard Keycloak OIDC endpoints from an issuer URI, matching
 * KEYCLOAK_ISSUER_URI in .env.example (http://localhost:8081/realms/foodloop
 * locally). Framework-specific wiring (NextAuth provider config for
 * web/admin, expo-auth-session config for mobile) consumes this — it isn't
 * itself a NextAuth/expo-auth-session integration.
 */
export interface OidcEndpoints {
  issuer: string;
  authorizationEndpoint: string;
  tokenEndpoint: string;
  userinfoEndpoint: string;
  endSessionEndpoint: string;
  jwksUri: string;
}

export function deriveKeycloakEndpoints(issuerUri: string): OidcEndpoints {
  const issuer = issuerUri.replace(/\/$/, "");
  const base = `${issuer}/protocol/openid-connect`;
  return {
    issuer,
    authorizationEndpoint: `${base}/auth`,
    tokenEndpoint: `${base}/token`,
    userinfoEndpoint: `${base}/userinfo`,
    endSessionEndpoint: `${base}/logout`,
    jwksUri: `${base}/certs`,
  };
}

export interface OidcClientConfig {
  issuerUri: string;
  clientId: string;
  /** Public clients (PKCE) never carry a secret — see ADR on frontend Keycloak clients. */
  redirectUri: string;
  scopes: string[];
}
