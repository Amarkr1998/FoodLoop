# Repository, Docker, CI/CD & Deployment Architecture

## 1. Repository structure

```
foodloop/
├── apps/{mobile,web,admin}/
├── services/{identity,tenant,food,matching,ngo,pickup,notification,trust-safety,impact,ai-orchestration}/
├── packages/shared-contracts/        # OpenAPI specs, generated clients, shared DTOs/events
├── ai/{agents,tools,workflows,prompts,rag,evaluators,guardrails}/
├── infrastructure/{docker,terraform,kubernetes,helm}/
├── docs/{architecture,adr,api,security,ai,operations}/
└── .github/workflows/
```
Each `services/*` module is an independent Maven module with its own `pom.xml`, Dockerfile, and DB
migration set (Flyway, one baseline per schema), so a module can be pulled into its own deployable
without restructuring later (ADR-003).

## 2. Local development (Docker Compose)

`infrastructure/docker/docker-compose.yml` brings up: PostgreSQL+PostGIS, Redis, Kafka (+ Zookeeper
or KRaft), RabbitMQ, Keycloak (with a pre-provisioned FoodLoop realm import), a pgvector-enabled
Postgres (can be the same instance as the primary DB, separate schema), and the backend services in
dev profile. `docker compose up -d` is the full bring-up command. `.env.example` documents every
required variable (DB creds, Keycloak realm/client, Azure OpenAI endpoint/key placeholders, Kafka
brokers) with dev-safe dummy values; `.env` is gitignored. Observability (OTel collector + local
Grafana/Prometheus) is included but optional via a compose profile flag, to keep the default
bring-up light.

## 3. CI/CD pipeline (§48)

```
Commit → Build → Unit Tests → Static Analysis (SpotBugs/ESLint/etc.) → Dependency Scan
  → Security Scan (SAST) → Integration Tests (Testcontainers) → AI Evaluation Tests (ai/evaluators)
  → Docker Build → Container Scan → Push Registry (ACR) → Deploy Staging → Smoke Tests
  → Manual Approval Gate → Production Deploy
```
Implemented on GitHub Actions, one workflow per `services/*` module (path-filtered, so a change to
`food` doesn't rebuild `pickup`) plus shared workflows for `apps/*`. AI Evaluation Tests run the
versioned eval dataset (§47) against agent graphs with the mock provider for determinism, and
against the real provider on a nightly/pre-release schedule to catch provider drift.

## 4. Deployment architecture (Azure, §49)

```
Azure Front Door / App Gateway (WAF)
   → AKS cluster
       - API Gateway pods
       - Service pods per context (HPA on CPU + custom metric e.g. Kafka consumer lag for Matching/AI)
       - AI Orchestration pods (separate node pool: higher memory, network egress to Azure OpenAI)
   → Azure Database for PostgreSQL (Flexible Server, PostGIS enabled), primary + read replica
   → Azure Cache for Redis
   → Managed Kafka (Confluent Cloud on Azure or self-managed on AKS, per ADR — self-managed
     acceptable for MVP given cost, revisit at scale) + Azure Service Bus or self-managed RabbitMQ
   → Azure Blob Storage (media)
   → Azure Key Vault (secrets, CSI driver into AKS)
   → Azure Container Registry
   → Azure OpenAI + Azure AI Foundry (private endpoint)
   → Azure Monitor + Application Insights (+ optional self-hosted Grafana for OTel/Prometheus detail)
```
Terraform provisions all of the above; Helm charts per service parametrize environment (staging/
prod) and region. The design stays portable — no Azure-only SDK calls inside domain code, only in
the adapter layer (blob storage, secrets, AI provider), so AWS/GCP equivalents are a new adapter,
not a rewrite.

## 5. Multi-region posture (later phase, §31/§32)

`tenantId` → `regionId` → `countryCode` is modeled from day one even though single-region deploy is
the MVP target, so data-residency-driven multi-region (Phase 12) is a deployment/routing change,
not a schema migration.
