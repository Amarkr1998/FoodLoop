# Development Phases, MVP Scope, Risks & Trade-offs

## 1. Development phases (§52, as specified)

| Phase | Deliverable |
|---|---|
| 0 | Architecture (this doc set) |
| 1 | Platform foundation: repo, Docker Compose, PostgreSQL/PostGIS, Redis, Kafka, RabbitMQ, Keycloak, API gateway, config, observability foundation, CI |
| 2 | Identity & Tenant: registration, auth, OAuth/OIDC, roles, permissions, orgs, tenant isolation, profiles |
| 3 | Food Core: listing, image upload, geospatial search, filters, expiry, state machine, claiming, race-condition protection — **works with AI fully disabled** |
| 4 | Pickup: scheduling, confirmation, no-show handling, notifications, donor/receiver workflow |
| 5 | AI Foundation: Spring AI, Azure OpenAI provider, AI Foundry integration, LangGraph, agent state, tool framework, guardrails, AI observability, provider abstraction |
| 6 | Food Intelligence Agent (first production agent, validated against eval dataset) |
| 7 | Matching Agent: deterministic eligibility engine + AI-assisted ranking/reasoning, match proposal, human approval where required |
| 8 | Rescue + NGO Coordination Agents: expiry monitoring, NGO requirements, rescue workflow, escalation |
| 9 | Trust & Safety: reports, risk scoring, safety flags, moderation, human review |
| 10 | Volunteer Delivery: registration, pickup tasks, delivery, route optimization, status tracking |
| 11 | Analytics & Impact: personal/NGO/org impact, community dashboard, environmental metrics |
| 12 | Enterprise: billing, subscriptions, corporate accounts, advanced analytics, multi-region, data residency |

Each phase follows the vertical-slice rule (§63): DB → domain → application → API → security →
events → tests → frontend → observability, fully wired before moving on. No empty services stood
up ahead of their phase.

## 2. MVP definition (§53)

Users (register/auth/roles/profile) + Donors (create + AI-assisted listing, manage) + Receivers
(nearby discovery, map, filters, claim) + Pickup (schedule/confirm/complete) + AI (Food
Intelligence, Smart Matching, Food Rescue agents) on Spring Boot / PostgreSQL+PostGIS / Redis /
Kafka / Keycloak / React Native / Docker / CI-CD. Explicitly excluded from MVP: billing, government
integrations, complex delivery logistics, multi-region.

## 3. Key risks & trade-offs

| Risk/Trade-off | Impact | Mitigation / accepted trade-off |
|---|---|---|
| Modular monolith → microservices timing | Splitting too early wastes effort; too late causes coupling | Schema-per-context + hexagonal boundaries from day one make the eventual split mechanical (ADR-003); split triggers defined per-context, not calendar-based |
| Self-managed Kafka/RabbitMQ vs managed | Ops burden vs cost | MVP: self-managed on AKS to control cost; revisit managed (Confluent/Service Bus) once volume or reliability SLOs demand it |
| AI provider dependency (Azure OpenAI) | Outage blocks AI features | Structural fallback: deterministic path is the default, AI decorates it (§05 file §9); core commerce flows never depend on AI availability |
| LLM hallucination in matching/safety | Wrong recommendation surfaces to user or ops | Deterministic hard filters gate everything the LLM ranks/explains; human-in-the-loop for high-risk actions; eval dataset with hallucination-rate tracking (§47) |
| Geospatial query performance at scale | Naive queries degrade with listing volume | PostGIS GIST indexes + radius-bounded queries + pagination from day one, never full-table load into app memory (§12) |
| Multi-tenant data leakage | Severe trust/compliance failure | Postgres RLS as structural enforcement, not just app-layer discipline; dedicated cross-tenant test suite (§31) |
| Cost of maintaining 22 architecture surfaces (this doc set) at "enterprise" fidelity while still being an early-stage build | Over-engineering risk explicitly flagged in spec (§2) | Build only what the current phase needs; defer Phase 9–12 concerns' *implementation* even though they're *designed for* here |
| Race conditions on claim | Double-fulfillment, donor/receiver trust damage | Partial unique index + optimistic locking in one transaction, covered by a dedicated concurrency test (§46) |
| I18n scope (7 languages) | Significant translation/content ops overhead | Architecture supports it (no hardcoded strings/locale logic) from Phase 2; actual translation content delivery is a content/ops task, not an engineering blocker for MVP |
