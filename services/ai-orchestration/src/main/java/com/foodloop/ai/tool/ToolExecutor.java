package com.foodloop.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodloop.ai.domain.ToolCallRecord;
import com.foodloop.ai.domain.ToolCallRepository;
import com.foodloop.ai.domain.ToolCallStatus;
import com.foodloop.commons.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * The single path through which every {@link AgentTool} is invoked (spec
 * §24 / docs/architecture/05-ai-agent-architecture.md §4). No agent code
 * calls a tool directly — everything goes through
 * {@link #run(AgentTool, AgentCallerContext, Object)}, which enforces the
 * {@link AgentPermissionRegistry} matrix, then runs
 * authorize -&gt; validateInput -&gt; execute -&gt; validateOutput, and persists a
 * {@link ToolCallRecord} for every outcome — permission-matrix denial,
 * tool-level denial, failure, or success alike (ADR-007: a denied call is
 * audited exactly like a successful one).
 */
@Component
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final AgentPermissionRegistry permissionRegistry;
    private final ToolCallRepository toolCallRepository;
    private final ObjectMapper objectMapper;

    public ToolExecutor(
            AgentPermissionRegistry permissionRegistry,
            ToolCallRepository toolCallRepository,
            ObjectMapper objectMapper) {
        this.permissionRegistry = permissionRegistry;
        this.toolCallRepository = toolCallRepository;
        this.objectMapper = objectMapper;
    }

    public <I, O> O run(AgentTool<I, O> tool, AgentCallerContext caller, I input) {
        String toolName = tool.name();

        if (!permissionRegistry.isPermitted(caller.agentName(), toolName)) {
            String reason = "Agent '" + caller.agentName() + "' has no permission-matrix entry for tool '"
                    + toolName + "'.";
            audit(caller, toolName, input, null, reason, ToolCallStatus.DENIED, 0);
            throw new ApiException("TOOL_NOT_PERMITTED", HttpStatus.FORBIDDEN, reason);
        }

        AuthorizationResult authorization = tool.authorize(caller, input);
        if (!authorization.allowed()) {
            audit(caller, toolName, input, null, authorization.reason(), ToolCallStatus.DENIED, 0);
            throw new ApiException("TOOL_CALL_DENIED", HttpStatus.FORBIDDEN, authorization.reason());
        }

        long startedAt = System.currentTimeMillis();
        try {
            tool.validateInput(input);
            O output = tool.execute(input);
            tool.validateOutput(output);
            int latencyMs = (int) (System.currentTimeMillis() - startedAt);
            audit(caller, toolName, input, output, authorization.reason(), ToolCallStatus.SUCCESS, latencyMs);
            return output;
        } catch (RuntimeException ex) {
            int latencyMs = (int) (System.currentTimeMillis() - startedAt);
            audit(caller, toolName, input, null, ex.getMessage(), ToolCallStatus.FAILED, latencyMs);
            throw ex;
        }
    }

    private <I, O> void audit(
            AgentCallerContext caller, String toolName, I input, O output,
            String note, ToolCallStatus status, int latencyMs) {
        ToolCallRecord record = new ToolCallRecord(
                caller.tenantId(),
                caller.agentRunId(),
                toolName,
                writeJson(input),
                output != null ? writeJson(output) : null,
                note,
                status,
                latencyMs);
        toolCallRepository.save(record);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize tool call payload for audit; recording placeholder.", ex);
            return "{\"error\":\"serialization-failed\"}";
        }
    }
}
