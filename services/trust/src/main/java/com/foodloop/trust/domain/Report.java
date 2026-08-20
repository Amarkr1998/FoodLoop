package com.foodloop.trust.domain;

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
import org.hibernate.annotations.UuidGenerator;

/**
 * One user's complaint about another (spec's Trust &amp; Safety context:
 * "reports"). Immutable once filed — a correction means filing a new
 * report, not editing history, same precedent as Impact's RescueRecord.
 */
@Entity
@Table(name = "report", schema = "trust")
public class Report {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "reporter_user_id", nullable = false, updatable = false)
    private UUID reporterUserId;

    @Column(name = "target_user_id", nullable = false, updatable = false)
    private UUID targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ReportReason reason;

    @Column(updatable = false)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Report() {
        // JPA
    }

    public Report(UUID tenantId, UUID reporterUserId, UUID targetUserId, ReportReason reason, String description) {
        this.tenantId = tenantId;
        this.reporterUserId = reporterUserId;
        this.targetUserId = targetUserId;
        this.reason = reason;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getReporterUserId() {
        return reporterUserId;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public ReportReason getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
