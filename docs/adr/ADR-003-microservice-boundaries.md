# ADR-003: Modular monolith first, split by proven divergence

## Status
Accepted

## Context
The spec explicitly warns against creating microservices merely because "microservices" appears in
the tech list, and against standing up empty services ahead of need (§4, §63).

## Decision
Build each bounded context (§01) as an independently buildable Maven module with its own schema,
hexagonal domain core, and API/event contract, deployed together initially where practical. Extract
a module into its own deployable only when it shows a genuinely divergent scaling profile, failure
boundary, or team ownership need. Food and AI Orchestration are the first likely candidates
(Food needs strict low-latency transactional consistency; AI Orchestration needs a different node
profile for LLM egress and has an entirely different failure mode).

## Consequences
- Fast initial development velocity; no premature network-boundary overhead between contexts that
  don't need it yet.
- Because schemas and domain boundaries are already separate, extraction is a deployment/packaging
  change, not a data-model rewrite.
- Risk: discipline is required to avoid contexts silently coupling through shared code or direct
  schema access; enforced by module visibility rules and code review, not just convention.
