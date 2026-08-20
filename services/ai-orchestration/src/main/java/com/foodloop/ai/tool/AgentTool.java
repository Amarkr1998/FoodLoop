package com.foodloop.ai.tool;

/**
 * Every capability an agent can invoke implements this — never a direct
 * repository or business-service call from agent code (spec §15: "never
 * give an LLM unrestricted database access"). {@link ToolExecutor} runs
 * the full authorize -&gt; validateInput -&gt; execute -&gt; validateOutput -&gt;
 * audit lifecycle (docs/architecture/05-ai-agent-architecture.md §4); a
 * tool implementation only supplies the four typed steps, never audits
 * itself, so every tool call is recorded the same way regardless of which
 * tool it was.
 */
public interface AgentTool<I, O> {

    String name();

    AuthorizationResult authorize(AgentCallerContext caller, I input);

    /** Throws (an {@code ApiException} or similar) on invalid input; never silently coerces. */
    void validateInput(I input);

    O execute(I input);

    /** Throws if the output doesn't match the tool's structural contract — output is never trusted blindly. */
    void validateOutput(O output);
}
