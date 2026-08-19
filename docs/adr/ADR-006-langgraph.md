# ADR-006: LangGraph for agent workflow orchestration

## Status
Accepted

## Context
FoodLoop's agents are not chatbots — they are multi-step workflows (observe → retrieve → reason →
tool-select → execute → validate → continue/escalate, §15) that must be resumable, observable, and
capable of long-running state (e.g., the Rescue Agent's expiry-monitoring/radius-expansion sequence
over hours, §18).

## Decision
Use LangGraph to model each agent as an explicit state graph with typed nodes and edges, not a
single-prompt chat loop. Deterministic validation/guard nodes sit between LLM-reasoning nodes and
tool-execution nodes.

## Consequences
- Each step is independently testable and observable (per-node latency, per-node failure), directly
  supporting the AI Ops Dashboard requirement (§38).
- Long-running/checkpointable workflows (Rescue Agent) survive service restarts without losing
  workflow position.
- Adds a learning-curve/tooling dependency beyond a plain prompt-chaining approach; accepted because
  the alternative (ad hoc orchestration code per agent) would be harder to keep consistent across
  seven distinct agents with shared guardrail/escalation semantics.
