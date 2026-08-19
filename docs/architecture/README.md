# FoodLoop Architecture — Phase 0

Index of the Phase 0 architecture deliverables (spec §62). No application code has been written —
this is the analysis to review and approve before Phase 1 implementation begins.

1. [Overview & C4 diagrams](00-overview.md) — product framing, system context, containers, style decisions
2. [Bounded contexts](01-bounded-contexts.md) — module boundaries, data ownership, context map
3. [Database design](02-database-design.md) — schema-per-context, ER model, key tables, RLS
4. [API catalog](03-api-catalog.md) — v1 REST surface per context
5. [Event catalog](04-event-catalog.md) — Kafka topics, RabbitMQ queues, envelope, versioning
6. [AI & agent architecture](05-ai-agent-architecture.md) — agent designs, tools, permissions, RAG, guardrails, fallback
7. [Security & threat model](06-security-threat-model.md) — auth architecture, STRIDE table, data classification
8. [Repository, Docker, CI/CD, deployment](07-infrastructure-deployment.md)
9. [Phases, MVP scope, risks](08-phases-mvp-risks.md)

ADRs live in [`../adr/`](../adr/) (ADR-001 through ADR-010).

## Open items requiring your decision before Phase 1

- Confirm the MVP scope in §08 (§53 of the source spec) as the actual first build target, or adjust.
- Confirm self-managed Kafka/RabbitMQ on AKS for MVP vs. managed services (cost vs. ops trade-off,
  see risks table).
- Confirm module-per-context Maven layout with co-deployment (ADR-003) vs. starting with even fewer,
  more-merged modules for MVP velocity.
