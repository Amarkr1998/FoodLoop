# FoodLoop — Architecture Overview

## 1. Product framing

FoodLoop is an AI-augmented, event-driven, geospatial, multi-tenant food-rescue platform.
It is not a delivery clone, not a CRUD app, and not a chatbot. The AI layer performs bounded,
tool-mediated operational work (classification, matching support, expiry-risk detection,
NGO coordination, risk scoring) inside a deterministic system of record. Deterministic code
owns every decision where correctness, money, or safety is at stake; the LLM owns reasoning,
extraction, and orchestration over tools that are individually authorized, validated, and audited.

## 2. Guiding constraints (from spec, non-negotiable)

- Deterministic algorithms handle distance, eligibility, state transitions, ranking math, and
  environmental-impact formulas. LLMs reason and orchestrate; they never become the source of truth.
- No agent has standing DB access — only typed, authorized, audited tools.
- Every AI-critical path (claiming, pickup, listing management) must keep working if every AI
  provider is down.
- No fake AI (keyword-matching dressed up as classification) and no fake data path in production
  code — mock providers must be explicit and dev-only.
- Build a modular monolith-of-bounded-contexts first; extract services only where the domain
  boundary, data ownership, and scaling profile actually diverge. Don't create empty services to
  satisfy the diagram.

## 3. C4 — Level 1: System Context

```mermaid
C4Context
  title FoodLoop — System Context

  Person(donor, "Donor", "Restaurant, hotel, grocery, caterer, home cook")
  Person(receiver, "Receiver", "Individual, NGO, food bank")
  Person(volunteer, "Volunteer", "Pickup/delivery")
  Person(ops, "Ops/Admin", "Trust & safety, platform ops")

  System(foodloop, "FoodLoop Platform", "Food-rescue coordination: listings, matching, pickup, AI agents")

  System_Ext(azureOpenAI, "Azure OpenAI / AI Foundry", "LLM inference")
  System_Ext(keycloak, "Keycloak", "OIDC identity provider")
  System_Ext(maps, "Maps/Geocoding Provider", "Geocoding, routing")
  System_Ext(notif, "Push/Email/SMS Gateways", "APNs/FCM, SMTP, SMS")
  System_Ext(blob, "Azure Blob Storage", "Images, documents")

  Rel(donor, foodloop, "Lists surplus food")
  Rel(receiver, foodloop, "Discovers & claims food")
  Rel(volunteer, foodloop, "Accepts pickup tasks")
  Rel(ops, foodloop, "Moderates, reviews escalations")
  Rel(foodloop, azureOpenAI, "Agent inference calls")
  Rel(foodloop, keycloak, "AuthN/AuthZ")
  Rel(foodloop, maps, "Geocode, route")
  Rel(foodloop, notif, "Deliver notifications")
  Rel(foodloop, blob, "Store media")
```

## 4. C4 — Level 2: Containers

```mermaid
C4Container
  title FoodLoop — Containers (initial deployment)

  Person(user, "Mobile/Web User")

  System_Boundary(edge, "Edge") {
    Container(gw, "API Gateway", "Spring Cloud Gateway", "Routing, auth enforcement, rate limiting")
  }

  System_Boundary(core, "Core Services (Spring Boot, modular)") {
    Container(identity, "Identity & Tenant Service", "Spring Boot", "AuthN/Z integration, orgs, tenants")
    Container(food, "Food Service", "Spring Boot + PostGIS", "Listings, state machine, discovery")
    Container(matching, "Matching Service", "Spring Boot", "Deterministic matching + ranking")
    Container(pickup, "Pickup Service", "Spring Boot", "Scheduling, confirmation")
    Container(ngo, "NGO Service", "Spring Boot", "NGO verification, requirements")
    Container(trust, "Trust & Safety Service", "Spring Boot", "Reports, risk, moderation")
    Container(impact, "Impact Service", "Spring Boot", "Metrics, reporting")
    Container(notif, "Notification Service", "Spring Boot", "Multi-channel delivery")
    Container(aiorch, "AI Orchestration Service", "Spring Boot + Spring AI + LangGraph", "Agents, tools, RAG, guardrails")
  }

  ContainerDb(pg, "PostgreSQL + PostGIS", "Postgres 16", "System of record, geospatial")
  ContainerDb(redis, "Redis", "Cache", "Cache, rate limit, short-lived agent state")
  ContainerDb(vector, "pgvector store", "Postgres extension", "RAG embeddings")
  ContainerQueue(kafka, "Kafka", "Event bus", "Domain events")
  ContainerQueue(mq, "RabbitMQ", "Task queue", "Operational async work")

  Rel(user, gw, "HTTPS/JSON, WSS")
  Rel(gw, identity, "REST")
  Rel(gw, food, "REST")
  Rel(gw, matching, "REST")
  Rel(gw, pickup, "REST")
  Rel(gw, ngo, "REST")
  Rel(gw, trust, "REST")
  Rel(gw, impact, "REST")
  Rel(gw, aiorch, "REST/WS")

  Rel(food, pg, "SQL")
  Rel(matching, pg, "SQL")
  Rel(aiorch, vector, "similarity search")
  Rel(food, kafka, "publish FOOD_*")
  Rel(matching, kafka, "consume FOOD_*, publish MATCH_*")
  Rel(aiorch, kafka, "consume/publish agent-relevant events")
  Rel(notif, mq, "consume send-notification jobs")
  Rel(food, redis, "cache listings")
```

## 5. C4 — Level 3: notable component view (Food Service)

```mermaid
C4Component
  title Food Service — Components

  Container_Boundary(food, "Food Service") {
    Component(api, "Food API", "REST controller", "CRUD, search, claim")
    Component(domain, "Food Domain", "Aggregate + state machine", "FoodListing, transitions, invariants")
    Component(geo, "Geo Query", "PostGIS repository", "Radius search, indexing")
    Component(claim, "Claim Handler", "Application service", "Optimistic locking, idempotency")
    Component(outbox, "Event Outbox", "Transactional outbox", "Guarantees at-least-once publish")
  }
  Rel(api, domain, "invokes")
  Rel(domain, geo, "reads/writes via repository")
  Rel(claim, domain, "state transition")
  Rel(domain, outbox, "writes event in same TX")
```

## 6. Architectural style decisions

- **Modular monolith of bounded contexts, deployed as separately buildable Spring Boot modules**
  sharing one deployable initially where practical (Phase 1–4), split into independently deployed
  services once a context shows an independent scaling or failure profile (AI Orchestration and
  Food are the first candidates to split, since they have the most divergent load and latency
  characteristics). See ADR-003.
- **Hexagonal/ports-and-adapters per module**: domain core has no framework dependency; adapters
  for REST, JPA, Kafka, and AI tools sit at the edges. This is what makes the AI-fallback and
  provider-abstraction requirements (§29) tractable — the domain doesn't know an LLM exists.
- **DDD tactical patterns**: aggregates (FoodListing, PickupTask, MatchProposal, NgoRequest, RiskCase)
  own their invariants; value objects (Money-like: Quantity+Unit, GeoPoint, TimeWindow, DietaryType)
  replace primitives; domain events are the seam to the event bus via transactional outbox.
- **API-first**: OpenAPI contracts in `packages/shared-contracts` are the source of truth; both
  backend controllers and generated frontend clients are derived from them.
