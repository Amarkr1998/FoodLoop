package com.foodloop.trust.api;

import com.foodloop.trust.domain.Report;
import java.time.Instant;
import java.util.UUID;

public record ReportResponse(UUID id, UUID reporterUserId, UUID targetUserId, String reason, String description, Instant createdAt) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(), report.getReporterUserId(), report.getTargetUserId(), report.getReason().name(),
                report.getDescription(), report.getCreatedAt());
    }
}
