package com.foodloop.ai.graph;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A minimal, genuinely-executing implementation of the node/edge/state graph
 * pattern described as "LangGraph" in the spec. LangGraph itself is a Python
 * library and cannot run inside this JVM, so every business agent
 * (Food Intelligence, Matching, Rescue, ...) is built on this instead of a
 * fake or unavailable integration — see ADR-006.
 *
 * <p>Execution walks Observe -&gt; Retrieve -&gt; Reason -&gt; Tool -&gt; Validate -&gt;
 * Continue/Escalate (docs/architecture/05-ai-agent-architecture.md §2): each
 * node transforms the state, then its {@link EdgeRouter} picks the next node
 * name from the resulting state. There is no hidden LLM-driven routing —
 * routers are plain Java, so a run is fully deterministic given the same node
 * outputs, and therefore resumable/inspectable.
 */
public final class AgentGraph<S> {

    public static final String END = "__END__";

    private final Map<String, GraphNode<S>> nodes;
    private final Map<String, EdgeRouter<S>> routers;
    private final String startNode;
    private final int maxSteps;

    private AgentGraph(Builder<S> builder) {
        this.nodes = builder.nodes;
        this.routers = builder.routers;
        this.startNode = builder.startNode;
        this.maxSteps = builder.maxSteps;
    }

    public static <S> Builder<S> builder(String startNode) {
        return new Builder<>(startNode);
    }

    /**
     * Runs the graph to completion. Throws {@link IllegalStateException}
     * rather than looping forever if routing never reaches {@link #END}
     * within {@code maxSteps} — a routing bug must fail loudly, not hang an
     * agent run.
     */
    public S run(S initialState) {
        S state = initialState;
        String current = startNode;
        int steps = 0;

        while (!END.equals(current)) {
            if (steps++ >= maxSteps) {
                throw new IllegalStateException(
                        "AgentGraph exceeded maxSteps (" + maxSteps + ") without reaching END; "
                                + "last node was '" + current + "'. Likely a routing cycle.");
            }
            GraphNode<S> node = nodes.get(current);
            if (node == null) {
                throw new IllegalStateException("No node registered for name '" + current + "'.");
            }
            state = node.execute(state);

            EdgeRouter<S> router = routers.get(current);
            current = router != null ? router.route(state) : END;
        }
        return state;
    }

    public static final class Builder<S> {
        private final String startNode;
        private final Map<String, GraphNode<S>> nodes = new LinkedHashMap<>();
        private final Map<String, EdgeRouter<S>> routers = new HashMap<>();
        private int maxSteps = 50;

        private Builder(String startNode) {
            this.startNode = startNode;
        }

        public Builder<S> node(GraphNode<S> node) {
            nodes.put(node.name(), node);
            return this;
        }

        public Builder<S> edge(String fromNode, EdgeRouter<S> router) {
            routers.put(fromNode, router);
            return this;
        }

        public Builder<S> maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public AgentGraph<S> build() {
            if (!nodes.containsKey(startNode)) {
                throw new IllegalStateException("Start node '" + startNode + "' was never registered via node(...).");
            }
            return new AgentGraph<>(this);
        }
    }
}
