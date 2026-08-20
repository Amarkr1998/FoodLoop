package com.foodloop.ai.tool;

import java.util.UUID;

/**
 * Identifies which agent is calling a tool and for which tenant — the
 * authorization check in {@link ToolExecutor} is keyed on
 * {@code agentName}, per the permission matrix in
 * {@link AgentPermissionRegistry} (docs/architecture/05-ai-agent-architecture.md
 * §5), not on anything the LLM's own output claims about itself.
 */
public record AgentCallerContext(String agentName, UUID tenantId, UUID agentRunId) {
}
