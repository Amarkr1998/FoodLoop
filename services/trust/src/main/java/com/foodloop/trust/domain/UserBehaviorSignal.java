package com.foodloop.trust.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** What the Trust &amp; Risk Agent's getUserBehaviorSignals tool reads — computed live from {@link Report} rows, never stored separately. */
public record UserBehaviorSignal(
        int reportCount, int distinctReporterCount, Instant mostRecentReportAt, Map<ReportReason, Long> reasonCounts) {

    public static UserBehaviorSignal from(List<Report> reports) {
        if (reports.isEmpty()) {
            return new UserBehaviorSignal(0, 0, null, Map.of());
        }
        int distinctReporters = (int) reports.stream().map(Report::getReporterUserId).distinct().count();
        Instant mostRecent = reports.stream().map(Report::getCreatedAt).max(Instant::compareTo).orElse(null);
        Map<ReportReason, Long> reasonCounts = reports.stream()
                .collect(java.util.stream.Collectors.groupingBy(Report::getReason, java.util.stream.Collectors.counting()));
        return new UserBehaviorSignal(reports.size(), distinctReporters, mostRecent, reasonCounts);
    }
}
