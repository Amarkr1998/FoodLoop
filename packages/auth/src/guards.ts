import { isAdminOrTrustOps, type FoodLoopTokenClaims } from "./claims";

export class ForbiddenError extends Error {
  constructor(message = "Requires ROLE_ADMIN or ROLE_TRUST_OPS.") {
    super(message);
    this.name = "ForbiddenError";
  }
}

/**
 * Gate for apps/admin routes (03-api-catalog.md's Admin section, all
 * ROLE_ADMIN/ROLE_TRUST_OPS-gated). This is a UX-layer guard only — the
 * backend re-checks the same roles server-side on every admin/* call
 * (06-security-threat-model.md T5), so this never substitutes for that,
 * it just avoids rendering an admin screen the API will reject anyway.
 */
export function requireAdminOrTrustOps(claims: FoodLoopTokenClaims | null | undefined): void {
  if (!isAdminOrTrustOps(claims)) {
    throw new ForbiddenError();
  }
}
