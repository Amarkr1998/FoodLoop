# ADR-004: Keycloak for identity, OAuth2/OIDC, MFA

## Status
Accepted

## Context
FoodLoop needs multi-role (donor, receiver, volunteer, NGO admin, org admin, trust ops, admin),
multi-tenant authentication with MFA and standards-based tokens consumable by many services,
without building an identity provider from scratch.

## Decision
Use Keycloak as the OIDC provider. Services validate JWTs via standard resource-server config;
tenant/org/role claims are embedded in the token and cross-checked against the database's
authoritative membership record on sensitive operations (never trusted from the token alone for
tenant-isolation-critical paths — see threat model T1/T5).

## Consequences
- Standards-based, avoids custom auth code and its associated security risk surface.
- MFA, social login, and future federation (corporate SSO for enterprise tenants, Phase 12) come
  largely "for free" via Keycloak's realm configuration.
- Adds an operational dependency (Keycloak availability, realm config as code) — mitigated by
  Terraform/Helm-managed realm provisioning and Docker Compose realm import for local dev.
