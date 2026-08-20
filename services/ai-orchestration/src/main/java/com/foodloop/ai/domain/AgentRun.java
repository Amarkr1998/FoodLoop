package com.foodloop.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * One row per agent invocation (docs/architecture/05-ai-agent-architecture.md
 * §10, AI Ops observability). {@code outcomeSummary} is a redacted decision
 * summary — raw chain-of-thought is never persisted (spec §38).
 */
@Entity
@Table(name = "agent_run", schema = "ai")
public class AgentRun {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "agent_name", nullable = false, updatable = false)
    private String agentName;

    @Column(name = "trigger_event_id")
    private UUID triggerEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentRunStatus status = AgentRunStatus.RUNNING;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "model_provider")
    private String modelProvider;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "outcome_summary")
    private String outcomeSummary;

    @Column(nullable = false)
    private boolean escalated = false;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "total_cost_usd")
    private BigDecimal totalCostUsd;

    protected AgentRun() {
        // JPA
    }

    public AgentRun(UUID tenantId, String agentName, UUID triggerEventId) {
        this.tenantId = tenantId;
        this.agentName = agentName;
        this.triggerEventId = triggerEventId;
    }

    public void recordModel(String provider, String modelName) {
        this.modelProvider = provider;
        this.modelName = modelName;
    }

    public void complete(String outcomeSummary) {
        this.status = AgentRunStatus.COMPLETED;
        this.outcomeSummary = outcomeSummary;
        this.completedAt = Instant.now();
    }

    public void escalate(String outcomeSummary) {
        this.status = AgentRunStatus.ESCALATED;
        this.escalated = true;
        this.outcomeSummary = outcomeSummary;
        this.completedAt = Instant.now();
    }

    public void fail(String outcomeSummary) {
        this.status = AgentRunStatus.FAILED;
        this.outcomeSummary = outcomeSummary;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getAgentName() {
        return agentName;
    }

    public UUID getTriggerEventId() {
        return triggerEventId;
    }

    public AgentRunStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getModelProvider() {
        return modelProvider;
    }

    public String getModelName() {
        return modelName;
    }

    public String getOutcomeSummary() {
        return outcomeSummary;
    }

    public boolean isEscalated() {
        return escalated;
    }
}
