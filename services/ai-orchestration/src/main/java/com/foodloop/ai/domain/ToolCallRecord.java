package com.foodloop.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * One row per {@link com.foodloop.ai.tool.AgentTool} invocation, written by
 * {@link com.foodloop.ai.tool.ToolExecutor} regardless of outcome — a
 * denied call is audited exactly like a successful one (ADR-007).
 */
@Entity
@Table(name = "tool_call", schema = "ai")
public class ToolCallRecord {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "agent_run_id", nullable = false, updatable = false)
    private UUID agentRunId;

    @Column(name = "tool_name", nullable = false, updatable = false)
    private String toolName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_json")
    private String inputJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_json")
    private String outputJson;

    @Column(name = "authorized_scope")
    private String authorizedScope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ToolCallStatus status;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ToolCallRecord() {
        // JPA
    }

    public ToolCallRecord(
            UUID tenantId, UUID agentRunId, String toolName, String inputJson, String outputJson,
            String authorizedScope, ToolCallStatus status, Integer latencyMs) {
        this.tenantId = tenantId;
        this.agentRunId = agentRunId;
        this.toolName = toolName;
        this.inputJson = inputJson;
        this.outputJson = outputJson;
        this.authorizedScope = authorizedScope;
        this.status = status;
        this.latencyMs = latencyMs;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getAgentRunId() {
        return agentRunId;
    }

    public String getToolName() {
        return toolName;
    }

    public ToolCallStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
