# Security Architecture & Threat Model

## 1. Security architecture (§30)

- **Identity provider**: Keycloak (OIDC), issuing JWTs consumed by every service via a shared
  resource-server config. MFA enforced for donor-org admins, NGO verifiers, and all `ROLE_ADMIN`/
  `ROLE_TRUST_OPS` accounts.
- **AuthZ**: RBAC baseline (`DONOR, RECEIVER, VOLUNTEER, NGO_ADMIN, ORG_ADMIN, TRUST_OPS, ADMIN`)
  plus fine-grained resource checks (e.g., only the donor org can edit its own listing) enforced in
  the application layer via method-security, never inferred from client-supplied claims beyond the
  signed JWT's roles/tenant claim.
- **Service-to-service**: mTLS within the cluster + service-account JWTs (client-credentials grant)
  scoped per caller, including AI agent service principals (§05 file, section 5).
- **Edge**: API Gateway terminates TLS, enforces authn, coarse rate limiting, and request size
  limits before traffic reaches any service.
- **Secrets**: Azure Key Vault; no secret in source control, env files, or logs. Local dev uses a
  `.env` populated from `.env.example` with dev-only dummy values, never committed.
- **Logging discipline**: structured logs redact tokens, passwords, and full PII fields (email/phone
  masked) by a logging interceptor, not by developer discipline alone.

## 2. Threat model (STRIDE-oriented, condensed)

| # | Threat | Vector | Mitigation |
|---|---|---|---|
| T1 | Cross-tenant data leakage | Missing `tenant_id` filter in a query | Postgres RLS keyed on session GUC (defense in depth beyond app-layer filters); cross-tenant integration tests required for every context (§31) |
| T2 | Double-claim / claim race | Two receivers claim same listing concurrently | Partial unique index + optimistic-lock version check in one transaction (§13, §02 file) |
| T3 | Prompt injection via listing text/images | Malicious donor text instructs agent to call unauthorized tool | Tool authorization is capability-based per agent service principal, independent of prompt content; input sanitization/delimiter fencing; output schema validation |
| T4 | Tool abuse / scope escalation | Compromised or manipulated agent attempts an out-of-scope tool call | Per-agent allowlist enforced at `AgentTool.authorize()`, denial audited, alerted on repeated denial pattern |
| T5 | Broken access control on admin endpoints | Missing method-level check | Centralized method-security annotations + contract tests asserting 403 for non-admin roles on every `/admin/*` route |
| T6 | SSRF via image/URL ingestion | Donor-supplied image URL used server-side | Fetch through an allowlisted proxy with private-IP-range blocking; no raw server-side fetch of arbitrary user URLs |
| T7 | Injection (SQL/NoSQL) | Unparameterized query | JPA/parameterized queries only; PostGIS raw queries reviewed and parameterized; static analysis in CI (§48) |
| T8 | Fraudulent listings / fake donations | Bad actor lists non-existent food to game impact metrics | Trust & Risk Agent signal ingestion, human-reviewed risk cases, no metric counted until `PICKUP_COMPLETED` |
| T9 | Rate-limit evasion / scraping receiver locations | Repeated unauthenticated or scripted discovery calls | Redis-backed per-user/IP rate limits at gateway + service layer; approximate location for public discovery (§33) |
| T10 | Secrets exposure | Committed credentials, verbose stack traces to client | Key Vault only; global exception handler strips internal detail before client response (§42) |
| T11 | Event replay / duplicate side effects | Kafka at-least-once redelivery | Idempotent consumers keyed by `eventId`; idempotency keys on all mutating tool/API calls |
| T12 | Agent over-reach on enforcement | Agent-recommended ban executed automatically | Structural human-in-the-loop gate — enforcement tools don't exist for risk/safety agents, only "create case" tools (§21, §22, §26) |
| T13 | PII over-exposure in discovery | Exact address shown to unauthenticated/unmatched users | `approx_location` (geo-jittered) used for public search; exact `location` only revealed post-claim to the matched party |
| T14 | Supply-chain / dependency compromise | Malicious transitive dependency | Dependency scanning + container scanning in CI pipeline (§48), pinned versions |

## 3. Data classification & privacy (§33)

- **PII tiers**: Tier 1 (name, email, phone, exact address) — encrypted at rest, access-logged,
  never in logs. Tier 2 (approximate location, org affiliation) — used for discovery. Tier 3
  (aggregate/derived, e.g. impact stats) — freely displayable.
- **Retention**: configurable per-tenant/region retention windows; deletion requests cascade via a
  documented job, not manual SQL.
- **Consent**: recorded at registration and re-confirmed for any new processing purpose (e.g.,
  enabling location sharing for volunteers).

This threat model is a living document — extend the table as new attack surfaces are added
(payment/billing in Phase 12 will need its own PCI-adjacent pass).
