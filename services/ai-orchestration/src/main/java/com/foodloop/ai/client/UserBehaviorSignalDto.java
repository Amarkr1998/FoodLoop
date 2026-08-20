package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserBehaviorSignalDto(
        int reportCount, int distinctReporterCount, Instant mostRecentReportAt, Map<String, Long> reasonCounts) {
}
