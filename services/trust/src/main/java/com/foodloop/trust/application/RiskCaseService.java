package com.foodloop.trust.application;

import com.foodloop.commons.web.ApiException;
import com.foodloop.trust.domain.Report;
import com.foodloop.trust.domain.RiskCase;
import com.foodloop.trust.domain.RiskCaseRepository;
import com.foodloop.trust.domain.RiskScorer;
import com.foodloop.trust.infrastructure.events.TrustEventPublisher;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code riskScore}/{@code requiresHumanReview} are always re-derived here
 * from this service's own {@link Report} rows via {@link RiskScorer} — the
 * Trust &amp; Risk Agent's {@code createRiskCase} call supplies only the
 * human-readable {@code riskFactors} rationale, never a score, so a
 * prompt-injected agent could choose *when* to open a case but can never
 * fabricate *how risky* it is (same "tool-side re-validation" precedent as
 * MatchingService#createProposal). This class never suspends or bans a
 * user — see RiskCase's own Javadoc for the anti-corruption boundary.
 */
@Service
public class RiskCaseService {

    private final RiskCaseRepository riskCaseRepository;
    private final ReportService reportService;
    private final TrustEventPublisher eventPublisher;
    private final BigDecimal humanReviewThreshold;

    public RiskCaseService(
            RiskCaseRepository riskCaseRepository,
            ReportService reportService,
            TrustEventPublisher eventPublisher,
            @Value("${foodloop.trust.human-review-threshold:50}") BigDecimal humanReviewThreshold) {
        this.riskCaseRepository = riskCaseRepository;
        this.reportService = reportService;
        this.eventPublisher = eventPublisher;
        this.humanReviewThreshold = humanReviewThreshold;
    }

    @Transactional
    public RiskCase create(UUID tenantId, UUID targetUserId, String riskFactors) {
        List<Report> reports = reportService.listForUser(targetUserId);
        if (reports.isEmpty()) {
            throw new ApiException("NO_SIGNALS_FOR_USER", HttpStatus.CONFLICT,
                    "User " + targetUserId + " has no reports on file; nothing to open a risk case for.");
        }
        BigDecimal riskScore = RiskScorer.score(reports);
        boolean requiresHumanReview = riskScore.compareTo(humanReviewThreshold) >= 0;

        RiskCase riskCase = riskCaseRepository.save(
                new RiskCase(tenantId, targetUserId, riskScore, riskFactors, requiresHumanReview));
        eventPublisher.publishRiskDetected(riskCase);
        return riskCase;
    }

    @Transactional(readOnly = true)
    public RiskCase get(UUID id) {
        return riskCaseRepository.findById(id)
                .orElseThrow(() -> new ApiException("RISK_CASE_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Risk case " + id + " was not found."));
    }

    @Transactional(readOnly = true)
    public List<RiskCase> listForUser(UUID targetUserId) {
        return riskCaseRepository.findByTargetUserIdOrderByCreatedAtDesc(targetUserId);
    }

    /** The TRUST_OPS review decision — records what a human decided, never enforces it itself. See RiskCase's Javadoc. */
    @Transactional
    public RiskCase resolve(UUID id, String resolutionAction, UUID resolvedByUserId) {
        RiskCase riskCase = get(id);
        riskCase.resolve(resolutionAction, resolvedByUserId);
        eventPublisher.publishModerationActionTaken(riskCase);
        return riskCase;
    }
}
