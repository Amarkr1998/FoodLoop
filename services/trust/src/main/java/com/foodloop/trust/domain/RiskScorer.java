package com.foodloop.trust.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * A deterministic weighted-signal function (spec §21: "not LLM-generated")
 * producing a 0-100 {@code riskScore} from a target user's report history.
 * Per-reason weights reflect severity, not frequency alone; a
 * {@code distinctReporterBonus} rewards corroboration from multiple
 * independent reporters over repeated reports from the same one (which
 * could just be a single grudge, not a real signal).
 */
public final class RiskScorer {

    private static final Map<ReportReason, BigDecimal> REASON_WEIGHT = Map.of(
            ReportReason.SAFETY, new BigDecimal("25"),
            ReportReason.FRAUD, new BigDecimal("20"),
            ReportReason.HARASSMENT, new BigDecimal("15"),
            ReportReason.NO_SHOW, new BigDecimal("10"),
            ReportReason.SPAM, new BigDecimal("5"),
            ReportReason.OTHER, new BigDecimal("5"));

    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private RiskScorer() {
    }

    public static BigDecimal score(List<Report> reports) {
        if (reports.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = reports.stream()
                .map(r -> REASON_WEIGHT.getOrDefault(r.getReason(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int distinctReporters = (int) reports.stream().map(Report::getReporterUserId).distinct().count();
        // 1.0x for a single reporter, +0.1x per additional distinct reporter, capped at 1.5x.
        BigDecimal bonus = BigDecimal.ONE.add(
                new BigDecimal("0.1").multiply(BigDecimal.valueOf(Math.max(0, distinctReporters - 1))));
        bonus = bonus.min(new BigDecimal("1.5"));

        BigDecimal score = base.multiply(bonus).setScale(2, RoundingMode.HALF_UP);
        return score.min(MAX_SCORE);
    }
}
