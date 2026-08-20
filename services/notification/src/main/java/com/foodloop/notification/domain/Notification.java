package com.foodloop.notification.domain;

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
 * One row per notification an agent or service asked to be sent — see the
 * module's pom.xml for why {@link #status} never leaves {@link NotificationStatus#QUEUED}
 * in this phase. Tenant-isolated via RLS (V1__create_notification.sql).
 * Exactly one of {@link #recipientOrgId}/{@link #recipientUserId} is set
 * (DB CHECK constraint, V2__add_recipient_user_id.sql) — the Pickup Agent
 * (spec §20) needs to notify a specific volunteer user, not an org, which
 * this table couldn't represent when Rescue (Phase 8) was its only writer.
 */
@Entity
@Table(name = "notification", schema = "notification")
public class Notification {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "recipient_org_id", updatable = false)
    private UUID recipientOrgId;

    @Column(name = "recipient_user_id", updatable = false)
    private UUID recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationChannel channel;

    @Column(nullable = false, updatable = false)
    private String subject;

    @Column(nullable = false, updatable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.QUEUED;

    /** Which AI agent run asked for this, if any — null for a human/system-triggered notification. */
    @Column(name = "source_agent_run_id")
    private UUID sourceAgentRunId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
        // JPA
    }

    private Notification(
            UUID tenantId, UUID recipientOrgId, UUID recipientUserId, NotificationChannel channel, String subject,
            String body, UUID sourceAgentRunId) {
        this.tenantId = tenantId;
        this.recipientOrgId = recipientOrgId;
        this.recipientUserId = recipientUserId;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.sourceAgentRunId = sourceAgentRunId;
    }

    public static Notification forOrg(
            UUID tenantId, UUID recipientOrgId, NotificationChannel channel, String subject, String body,
            UUID sourceAgentRunId) {
        return new Notification(tenantId, recipientOrgId, null, channel, subject, body, sourceAgentRunId);
    }

    public static Notification forUser(
            UUID tenantId, UUID recipientUserId, NotificationChannel channel, String subject, String body,
            UUID sourceAgentRunId) {
        return new Notification(tenantId, null, recipientUserId, channel, subject, body, sourceAgentRunId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getRecipientOrgId() {
        return recipientOrgId;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public UUID getSourceAgentRunId() {
        return sourceAgentRunId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
