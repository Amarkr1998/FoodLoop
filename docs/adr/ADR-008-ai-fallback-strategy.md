# ADR-008: Deterministic-default AI fallback strategy

## Status
Accepted

## Context
§29 and §55 require that critical functions (listing, claiming, pickup) keep working if every AI
provider is unavailable, and forbid disguising deterministic logic as AI or vice versa.

## Decision
Every AI-touched workflow is implemented so the deterministic path is the actual default
implementation, and the AI agent *decorates* it (adds suggestions, ranking rationale, proactive
detection) rather than being a required step in the critical path. Provider failure triggers, in
order: primary provider (Azure OpenAI) → secondary provider (Azure AI Foundry) → deterministic-only
path with AI features visibly disabled (e.g., "AI suggestions unavailable" in the listing form,
Matching Agent's rationale field simply absent while the deterministic `MatchingEngine` still
ranks). Circuit breakers (Resilience4j) trigger the fallback automatically on sustained provider
errors/timeouts.

## Consequences
- Core commerce flows (publish, claim, pickup) have zero hard dependency on LLM availability.
- Requires discipline to keep the deterministic implementation genuinely complete (not a stub) since
  it's the real fallback path, not a decorative one — directly enforces §55's "no fake
  implementation" rule by making the non-AI path load-bearing.
- Slightly more implementation work up front (two working paths per AI-touched feature) in exchange
  for real resilience.
