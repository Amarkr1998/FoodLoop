package com.foodloop.matching.domain;

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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.http.HttpStatus;

/**
 * One row per proposed listing-to-receiver match (spec §17). {@code score}
 * and {@code distanceMeters} are always the deterministic
 * {@link MatchingEngine} output computed at persist time by
 * {@code MatchingService} — never a value the agent supplies directly —
 * so a prompt-injected agent can pick which candidate to propose but can't
 * fabricate why it was eligible. {@code aiRationale} is the one field the
 * LLM does write: a human-readable explanation, not a score.
 */
@Entity
@Table(name = "match_proposal", schema = "matching")
public class MatchProposal {

    private static final Map<MatchStatus, Set<MatchStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(MatchStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(MatchStatus.PROPOSED, EnumSet.of(MatchStatus.ACCEPTED, MatchStatus.REJECTED, MatchStatus.EXPIRED));
        ALLOWED_TRANSITIONS.put(MatchStatus.ACCEPTED, EnumSet.noneOf(MatchStatus.class));
        ALLOWED_TRANSITIONS.put(MatchStatus.REJECTED, EnumSet.noneOf(MatchStatus.class));
        ALLOWED_TRANSITIONS.put(MatchStatus.EXPIRED, EnumSet.noneOf(MatchStatus.class));
    }

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "food_listing_id", nullable = false, updatable = false)
    private UUID foodListingId;

    @Column(name = "receiver_org_id", nullable = false, updatable = false)
    private UUID receiverOrgId;

    @Column(name = "distance_meters", nullable = false, updatable = false)
    private BigDecimal distanceMeters;

    @Column(nullable = false, updatable = false)
    private BigDecimal score;

    @Column(name = "ai_rationale")
    private String aiRationale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.PROPOSED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MatchProposal() {
        // JPA
    }

    public MatchProposal(
            UUID tenantId, UUID foodListingId, UUID receiverOrgId,
            BigDecimal distanceMeters, BigDecimal score, String aiRationale) {
        this.tenantId = tenantId;
        this.foodListingId = foodListingId;
        this.receiverOrgId = receiverOrgId;
        this.distanceMeters = distanceMeters;
        this.score = score;
        this.aiRationale = aiRationale;
    }

    public void transitionTo(MatchStatus target) {
        if (!ALLOWED_TRANSITIONS.get(status).contains(target)) {
            throw new ApiException("INVALID_STATE_TRANSITION", HttpStatus.CONFLICT,
                    "Cannot transition match proposal from " + status + " to " + target + ".");
        }
        this.status = target;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getFoodListingId() {
        return foodListingId;
    }

    public UUID getReceiverOrgId() {
        return receiverOrgId;
    }

    public BigDecimal getDistanceMeters() {
        return distanceMeters;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getAiRationale() {
        return aiRationale;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
