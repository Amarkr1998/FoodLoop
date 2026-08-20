package com.foodloop.trust.domain;

import com.foodloop.commons.web.ApiException;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.http.HttpStatus;

/**
 * A proposal, never an enforcement (spec §21: "It cannot suspend or ban").
 * {@code riskScore}/{@code requiresHumanReview} are always computed
 * server-side by {@link RiskScorer} at creation time — see RiskCaseService
 * — never trusted from the caller. Resolution records a human decision
 * ({@code resolutionAction}), but this service never mutates an Identity
 * account itself; that enforcement step is a separate, not-yet-built,
 * human-triggered Identity-context action (§26) — this is the anti-corruption
 * boundary docs/architecture/01-bounded-contexts.md describes.
 */
@Entity
@Table(name = "risk_case", schema = "trust")
public class RiskCase {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "target_user_id", nullable = false, updatable = false)
    private UUID targetUserId;

    @Column(name = "risk_score", nullable = false, updatable = false)
    private BigDecimal riskScore;

    @Column(name = "risk_factors", updatable = false)
    private String riskFactors;

    @Column(name = "requires_human_review", nullable = false, updatable = false)
    private boolean requiresHumanReview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskCaseStatus status = RiskCaseStatus.OPEN;

    @Column(name = "resolution_action")
    private String resolutionAction;

    @Column(name = "resolved_by_user_id")
    private UUID resolvedByUserId;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RiskCase() {
        // JPA
    }

    public RiskCase(
            UUID tenantId, UUID targetUserId, BigDecimal riskScore, String riskFactors, boolean requiresHumanReview) {
        this.tenantId = tenantId;
        this.targetUserId = targetUserId;
        this.riskScore = riskScore;
        this.riskFactors = riskFactors;
        this.requiresHumanReview = requiresHumanReview;
    }

    public void resolve(String resolutionAction, UUID resolvedByUserId) {
        if (status != RiskCaseStatus.OPEN) {
            throw new ApiException("RISK_CASE_ALREADY_RESOLVED", HttpStatus.CONFLICT,
                    "Risk case " + id + " was already resolved.");
        }
        this.status = RiskCaseStatus.RESOLVED;
        this.resolutionAction = resolutionAction;
        this.resolvedByUserId = resolvedByUserId;
        this.resolvedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public String getRiskFactors() {
        return riskFactors;
    }

    public boolean isRequiresHumanReview() {
        return requiresHumanReview;
    }

    public RiskCaseStatus getStatus() {
        return status;
    }

    public String getResolutionAction() {
        return resolutionAction;
    }

    public UUID getResolvedByUserId() {
        return resolvedByUserId;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
