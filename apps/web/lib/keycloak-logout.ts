/**
 * next-auth's own signOut() only clears our session cookie — Keycloak keeps
 * its separate SSO session alive, so a subsequent "Sign in" silently
 * re-authenticates the same account without ever showing Keycloak's login
 * form. RP-initiated logout (OIDC's standard end_session_endpoint) is the
 * only way to actually end that SSO session too.
 */
export function buildKeycloakLogoutUrl(idToken: string | undefined): string {
  const issuer = process.env.NEXT_PUBLIC_KEYCLOAK_ISSUER_URI ?? "http://localhost:8081/realms/foodloop";
  const clientId = process.env.NEXT_PUBLIC_KEYCLOAK_WEB_CLIENT_ID ?? "foodloop-web";
  const url = new URL(`${issuer}/protocol/openid-connect/logout`);
  // Keycloak requires at least one of id_token_hint/client_id alongside
  // post_logout_redirect_uri — a session from before idToken was captured
  // (or one that simply never got one) would otherwise 400 on sign-out.
  if (idToken) url.searchParams.set("id_token_hint", idToken);
  url.searchParams.set("client_id", clientId);
  url.searchParams.set("post_logout_redirect_uri", `${window.location.origin}/login`);
  return url.toString();
}
