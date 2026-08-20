package com.foodloop.ai.domain;

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
 * The NGO Coordination Agent's human-approval gate (spec §19): created
 * instead of an immediate {@code createMatchProposal} call when an
 * allocation exceeds the configured quantity threshold. Resolved by
 * {@code POST /api/v1/ai/agent-runs/{id}/escalate/resolve}, which is also
 * the point {@code createMatchProposal} actually runs on approval — see
 * NgoCoordinationAgent's Javadoc.
 */
@Entity
@Table(name = "pending_ngo_allocation", schema = "ai")
public class PendingNgoAllocation {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "agent_run_id", nullable = false, updatable = false)
    private UUID agentRunId;

    @Column(name = "ngo_request_id", nullable = false, updatable = false)
    private UUID ngoRequestId;

    @Column(name = "ngo_org_id", nullable = false, updatable = false)
    private UUID ngoOrgId;

    @Column(name = "food_listing_id", nullable = false, updatable = false)
    private UUID foodListingId;

    @Column(name = "quantity_needed", nullable = false, updatable = false)
    private BigDecimal quantityNeeded;

    @Column(name = "quantity_unit", nullable = false, updatable = false)
    private String quantityUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PendingAllocationStatus status = PendingAllocationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by_user_id")
    private UUID resolvedByUserId;

    protected PendingNgoAllocation() {
        // JPA
    }

    public PendingNgoAllocation(
            UUID tenantId, UUID agentRunId, UUID ngoRequestId, UUID ngoOrgId, UUID foodListingId,
            BigDecimal quantityNeeded, String quantityUnit) {
        this.tenantId = tenantId;
        this.agentRunId = agentRunId;
        this.ngoRequestId = ngoRequestId;
        this.ngoOrgId = ngoOrgId;
        this.foodListingId = foodListingId;
        this.quantityNeeded = quantityNeeded;
        this.quantityUnit = quantityUnit;
    }

    public void approve(UUID approverUserId) {
        requirePending();
        this.status = PendingAllocationStatus.APPROVED;
        this.resolvedAt = Instant.now();
        this.resolvedByUserId = approverUserId;
    }

    public void reject(UUID reviewerUserId) {
        requirePending();
        this.status = PendingAllocationStatus.REJECTED;
        this.resolvedAt = Instant.now();
        this.resolvedByUserId = reviewerUserId;
    }

    private void requirePending() {
        if (status != PendingAllocationStatus.PENDING) {
            throw new ApiException("ALLOCATION_ALREADY_RESOLVED", HttpStatus.CONFLICT,
                    "This pending allocation was already " + status + ".");
        }
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

    public UUID getNgoRequestId() {
        return ngoRequestId;
    }

    public UUID getNgoOrgId() {
        return ngoOrgId;
    }

    public UUID getFoodListingId() {
        return foodListingId;
    }

    public BigDecimal getQuantityNeeded() {
        return quantityNeeded;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public PendingAllocationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
