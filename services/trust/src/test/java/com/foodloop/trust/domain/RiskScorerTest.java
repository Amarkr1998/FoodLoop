package com.foodloop.trust.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RiskScorerTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID TARGET = UUID.randomUUID();

    @Test
    void noReportsScoresZero() {
        assertThat(RiskScorer.score(List.of())).isEqualByComparingTo("0");
    }

    @Test
    void singleLowSeverityReportFromOneReporterScoresBaseWeightOnly() {
        Report report = new Report(TENANT, UUID.randomUUID(), TARGET, ReportReason.SPAM, "spam");
        assertThat(RiskScorer.score(List.of(report))).isEqualByComparingTo("5.00");
    }

    @Test
    void safetyReportsWeighMoreThanSpam() {
        Report safety = new Report(TENANT, UUID.randomUUID(), TARGET, ReportReason.SAFETY, "unsafe");
        Report spam = new Report(TENANT, UUID.randomUUID(), TARGET, ReportReason.SPAM, "spam");
        assertThat(RiskScorer.score(List.of(safety))).isGreaterThan(RiskScorer.score(List.of(spam)));
    }

    @Test
    void multipleDistinctReportersScoreHigherThanRepeatedReportsFromOne() {
        UUID sameReporter = UUID.randomUUID();
        List<Report> fromOneReporter = List.of(
                new Report(TENANT, sameReporter, TARGET, ReportReason.SPAM, "1"),
                new Report(TENANT, sameReporter, TARGET, ReportReason.SPAM, "2"));
        List<Report> fromTwoReporters = List.of(
                new Report(TENANT, UUID.randomUUID(), TARGET, ReportReason.SPAM, "1"),
                new Report(TENANT, UUID.randomUUID(), TARGET, ReportReason.SPAM, "2"));

        assertThat(RiskScorer.score(fromTwoReporters)).isGreaterThan(RiskScorer.score(fromOneReporter));
    }

    @Test
    void scoreNeverExceedsOneHundred() {
        List<Report> manySafetyReports = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> new Report(TENANT, UUID.randomUUID(), TARGET, ReportReason.SAFETY, "report " + i))
                .toList();
        assertThat(RiskScorer.score(manySafetyReports)).isLessThanOrEqualTo(new java.math.BigDecimal("100"));
    }
}
