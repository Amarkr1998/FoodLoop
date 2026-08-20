package com.foodloop.trust.api;

import com.foodloop.trust.domain.ReportReason;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateReportRequest(@NotNull UUID targetUserId, @NotNull ReportReason reason, String description) {
}
