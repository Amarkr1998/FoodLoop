package com.foodloop.ai.graph;

/**
 * One step of an {@link AgentGraph} — e.g. Observe, Retrieve Context, Reason,
 * Execute Tool, Validate Result (docs/architecture/05-ai-agent-architecture.md
 * §2). Deterministic nodes (schema/business-rule validation) and LLM-backed
 * nodes implement the same contract, so the graph engine has no notion of
 * which nodes call a model and which don't.
 */
public interface GraphNode<S> {

    String name();

    S execute(S state);
}
