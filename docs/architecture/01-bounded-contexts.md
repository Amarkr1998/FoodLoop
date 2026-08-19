# Bounded Contexts & Module Boundaries

Each context below is a Maven module today; a subset becomes an independently deployed service in
later phases per ADR-003. "Owns data" = only this context writes that data; others read via API/event,
never via direct DB access, even pre-split (enforced by separate schemas per context from day one).

| Context | Owns | Does NOT own | Publishes events | Consumes events |
|---|---|---|---|---|
| **Identity & Access** | users, credentials(via Keycloak), roles, permissions, MFA state | org membership semantics | `USER_REGISTERED`, `USER_VERIFIED` | — |
| **Organization & Tenant** | organizations, tenants, regions, subscription tier | users themselves | `ORG_CREATED`, `ORG_VERIFIED` | `USER_REGISTERED` |
| **Food** | food listings, state machine, images metadata, geo index | claims' receiver identity beyond FK | `FOOD_LISTED`, `FOOD_UPDATED`, `FOOD_CLAIMED`, `FOOD_RESERVED`, `FOOD_EXPIRING`, `FOOD_EXPIRED`, `FOOD_CANCELLED` | `RISK_DETECTED` (auto-flag) |
| **Matching** | match proposals, ranking runs | food/NGO source data | `MATCH_CREATED`, `MATCH_ACCEPTED`, `MATCH_REJECTED` | `FOOD_LISTED`, `FOOD_UPDATED`, `NGO_REQUEST_CREATED`, `FOOD_EXPIRING` |
| **Pickup** | pickup tasks, scheduling, confirmation, no-show | volunteer identity/profile | `PICKUP_CREATED`, `PICKUP_SCHEDULED`, `PICKUP_COMPLETED`, `PICKUP_NO_SHOW` | `MATCH_ACCEPTED`, `FOOD_RESERVED` |
| **NGO** | NGO profile, verification, requirements, bulk requests | matching logic itself | `NGO_REQUEST_CREATED`, `NGO_VERIFIED` | `MATCH_CREATED` |
| **Notification** | delivery records, preferences, quiet hours | the triggering business fact | `NOTIFICATION_SENT`, `NOTIFICATION_FAILED` | almost all domain events (fan-in) |
| **Impact** | computed metrics, reports | raw operational data | `IMPACT_COMPUTED` | `PICKUP_COMPLETED`, `DONATION_COMPLETED` |
| **Trust & Safety** | reports, risk scores, safety flags, moderation actions | enforcement outside its own scope (bans need human + Identity) | `RISK_DETECTED`, `SAFETY_FLAGGED`, `MODERATION_ACTION_TAKEN` | `FOOD_LISTED`, `PICKUP_NO_SHOW`, user behavior signals |
| **AI Orchestration** | agent runs, tool-call logs, RAG index, guardrail decisions | any business record it acts on (writes only via tools that call back into owning context APIs) | `AGENT_RUN_STARTED`, `AGENT_RUN_COMPLETED`, `AGENT_ESCALATED` | most domain events (trigger conditions) |

## Context map relationships

- **Food → Matching**: Customer/Supplier. Food is upstream; Matching consumes Food's published
  contract and events, never reaches into Food's schema.
- **Matching → Pickup**: Customer/Supplier via `MATCH_ACCEPTED`.
- **AI Orchestration ↔ everything**: Conformist on each context's public API/tool contract — the
  AI layer never gets its own privileged data path. Every "AI writes something" is actually
  "AI Orchestration calls Food's/Pickup's authenticated API as a scoped service identity."
- **Trust & Safety → Identity**: Anti-corruption layer. Trust & Safety proposes actions
  (`recommendedAction`); Identity is the only context that mutates account state, and only after
  human approval for suspensions/bans (§26).
- **Organization & Tenant** is a shared-kernel-adjacent context: `tenantId`/`orgId` are referenced
  everywhere, but the module itself is the single owner of tenant lifecycle and isolation rules.

## Why these boundaries (not smaller, not bigger)

- Matching is split from Food because its scaling driver (fan-out ranking computation, potential
  future ML ranking) and failure profile (best-effort, retry-heavy) differ sharply from Food's
  (strict transactional consistency for claims).
- Notification is split out because it's the highest-fan-in, highest-volume, most retry-heavy
  context, and a notification outage must never block a claim or pickup.
- Trust & Safety is separate from Food/Identity because it needs an independent audit trail and
  because its write path (recommend, don't enforce) is architecturally different from every other
  context's write path.
- AI Orchestration is separate because it has a distinct dependency (LLM providers), distinct
  failure mode (provider outage, hallucination), and distinct observability needs (token cost,
  prompt versions) that would pollute business-service dashboards if co-located.
