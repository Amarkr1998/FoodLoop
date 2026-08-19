# FoodLoop

Enterprise-grade, AI-powered, hyperlocal surplus-food redistribution platform. Autonomous but
controlled AI agents coordinate surplus-food discovery, intelligent donor–receiver matching,
expiry rescue, NGO coordination, and pickup operations across a secure, event-driven, geospatial,
multi-tenant architecture.

## Status

**Phase 1 — Platform Foundation.** See [docs/architecture/08-phases-mvp-risks.md](docs/architecture/08-phases-mvp-risks.md)
for the full phase plan. Phase 0 architecture (approved) lives in [docs/architecture/](docs/architecture/)
and [docs/adr/](docs/adr/) — read those first for the why behind everything here.

## Repository structure

```
apps/               mobile (React Native/Expo), web (Next.js), admin
services/           one Spring Boot module per bounded context, added as each phase builds it
packages/           shared-contracts: OpenAPI specs + event schemas, source of truth for both
                     backend controllers and generated frontend clients
ai/                 agents, tools, workflows, prompts, rag, evaluators, guardrails
infrastructure/     docker (local dev), terraform, kubernetes, helm
docs/               architecture, adr, api, security, ai, operations
```

## Local development

Prerequisites: Docker, Java 21, Maven.

```bash
cp .env.example .env        # fill in real values if you have them; dev defaults work as-is
cd infrastructure/docker
docker compose up -d        # Postgres+PostGIS+pgvector, Redis, Kafka, RabbitMQ, Keycloak, api-gateway
docker compose --profile observability up -d   # optional: OTel collector, Prometheus, Grafana
```

Build and test the backend from the repo root:

```bash
mvn compile
mvn test
```

Services as they land:

| Service | Port (local) |
|---|---|
| API Gateway | http://localhost:8080 |
| Keycloak | http://localhost:8081 (admin/admin_dev_only) |
| RabbitMQ management | http://localhost:15672 |
| Prometheus (observability profile) | http://localhost:9090 |
| Grafana (observability profile) | http://localhost:3001 |

## AI provider configuration

`AI_PROVIDER_MODE=mock` in `.env.example` runs the platform with the deterministic-only fallback
path active and no calls to Azure OpenAI/AI Foundry (ADR-008) — every core commerce flow (listing,
claiming, pickup) works this way. Set `AZURE_OPENAI_*` values and flip the mode to enable live
agent behavior once Phase 5+ AI modules exist.

## Documentation

- [Architecture overview & C4 diagrams](docs/architecture/00-overview.md)
- [Bounded contexts](docs/architecture/01-bounded-contexts.md)
- [Database design](docs/architecture/02-database-design.md)
- [API catalog](docs/architecture/03-api-catalog.md)
- [Event catalog](docs/architecture/04-event-catalog.md)
- [AI & agent architecture](docs/architecture/05-ai-agent-architecture.md)
- [Security & threat model](docs/architecture/06-security-threat-model.md)
- [Infrastructure & deployment](docs/architecture/07-infrastructure-deployment.md)
- [Phases, MVP, risks](docs/architecture/08-phases-mvp-risks.md)
- [Architectural Decision Records](docs/adr/)
