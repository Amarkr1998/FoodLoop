package com.foodloop.impact.api;

import com.foodloop.impact.domain.ImpactSummary;
import java.math.BigDecimal;

/** {@code estimatedKgSaved}/{@code estimatedCo2SavedKg} are approximations — see ImpactCalculator's Javadoc. */
public record ImpactSummaryResponse(long rescueCount, BigDecimal estimatedKgSaved, BigDecimal estimatedCo2SavedKg) {

    public static ImpactSummaryResponse from(ImpactSummary summary) {
        return new ImpactSummaryResponse(summary.rescueCount(), summary.totalKgSaved(), summary.totalCo2SavedKg());
    }
}
