# ADR-007: Capability-based, per-agent tool authorization

## Status
Accepted

## Context
§15 and §25 require that no LLM ever gets unrestricted database access and that each agent has
explicit, non-admin capabilities. Prompt injection (threat T3) means the *content* an LLM produces
cannot be trusted as an authorization signal.

## Decision
Every tool call is authorized structurally, independent of prompt content: each agent runs under a
distinct service-account identity with a fixed, per-agent scope allowlist (the permission matrix in
`05-ai-agent-architecture.md`), enforced inside `AgentTool.authorize()` before input validation or
execution. A tool call outside an agent's scope is denied and audited, regardless of how the LLM
justified requesting it.

## Consequences
- Prompt injection can at most cause an agent to *attempt* an out-of-scope call, which fails closed
  and is auditable/alertable — it cannot succeed regardless of prompt content.
- Enforcement/ban-type actions structurally don't exist as callable tools for Risk/Safety agents;
  only "create case" tools exist, making human-in-the-loop (§26) a property of the tool surface, not
  just a workflow convention.
- Adds bookkeeping overhead (maintaining per-agent scope lists as new tools are added) — accepted as
  the cost of a genuinely enforced boundary rather than a documented-but-optional one.
