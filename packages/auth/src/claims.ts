/**
 * Shape of the access token claims issued by the foodloop Keycloak realm.
 * `tenant_id` is a custom protocol mapper (infrastructure/docker/keycloak/
 * foodloop-realm.json); realm roles match the RBAC baseline in
 * docs/architecture/06-security-threat-model.md §1.
 */
export type FoodLoopRealmRole =
  | "DONOR"
  | "RECEIVER"
  | "VOLUNTEER"
  | "NGO_ADMIN"
  | "NGO_OPS"
  | "ORG_ADMIN"
  | "TRUST_OPS"
  | "ADMIN";

export interface FoodLoopTokenClaims {
  sub: string;
  iss: string;
  aud: string | string[];
  azp: string;
  exp: number;
  iat: number;
  jti: string;
  tenant_id?: string;
  email?: string;
  preferred_username?: string;
  realm_access?: { roles: string[] };
}

export function hasRole(claims: FoodLoopTokenClaims | null | undefined, role: FoodLoopRealmRole): boolean {
  return claims?.realm_access?.roles.includes(role) ?? false;
}

export function isAdminOrTrustOps(claims: FoodLoopTokenClaims | null | undefined): boolean {
  return hasRole(claims, "ADMIN") || hasRole(claims, "TRUST_OPS");
}

const BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

/**
 * Manual base64 -> UTF-8 decode with no runtime dependency: `atob` isn't
 * guaranteed in a React Native/Hermes environment, and `Buffer` isn't
 * guaranteed in a browser, and this package is shared by both web/admin
 * (browser) and mobile (Expo).
 */
function base64ToUtf8(base64: string): string {
  const clean = base64.replace(/=+$/, "");
  const bytes: number[] = [];
  let buffer = 0;
  let bits = 0;
  for (const char of clean) {
    const value = BASE64_CHARS.indexOf(char);
    if (value === -1) continue;
    buffer = (buffer << 6) | value;
    bits += 6;
    if (bits >= 8) {
      bits -= 8;
      bytes.push((buffer >> bits) & 0xff);
    }
  }
  return new TextDecoder("utf-8").decode(new Uint8Array(bytes));
}

/** Decodes the payload segment of a JWT without verifying its signature — display/UX gating only, never a security boundary (the backend always re-validates). */
export function decodeJwtPayload<T = FoodLoopTokenClaims>(token: string): T | null {
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  try {
    const payload = parts[1]!.replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(base64ToUtf8(payload)) as T;
  } catch {
    return null;
  }
}
