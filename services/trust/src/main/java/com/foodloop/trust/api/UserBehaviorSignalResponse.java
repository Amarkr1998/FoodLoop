package com.foodloop.trust.api;

import com.foodloop.trust.domain.UserBehaviorSignal;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

public record UserBehaviorSignalResponse(
        int reportCount, int distinctReporterCount, Instant mostRecentReportAt, Map<String, Long> reasonCounts) {

    public static UserBehaviorSignalResponse from(UserBehaviorSignal signal) {
        return new UserBehaviorSignalResponse(
                signal.reportCount(), signal.distinctReporterCount(), signal.mostRecentReportAt(),
                signal.reasonCounts().entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue)));
    }
}
