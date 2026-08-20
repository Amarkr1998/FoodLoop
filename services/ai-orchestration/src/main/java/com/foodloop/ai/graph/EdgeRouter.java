package com.foodloop.ai.graph;

/**
 * Decides the next node name after a {@link GraphNode} runs, based on the
 * state it produced — e.g. "Validate Result" routing to a retry, a
 * different tool, {@link AgentGraph#END}, or an escalation node
 * (docs/architecture/05-ai-agent-architecture.md §2). This is what makes a
 * graph a state machine rather than a fixed pipeline: routing is data-driven,
 * never another LLM call grading itself.
 */
@FunctionalInterface
public interface EdgeRouter<S> {

    String route(S state);
}
