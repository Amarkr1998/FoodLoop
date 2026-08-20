package com.foodloop.impact.api;

import com.foodloop.impact.domain.MonthlyImpactSummary;
import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code estimatedKgSaved}/{@code estimatedCo2SavedKg} are approximations — see ImpactCalculator's Javadoc. */
public record MonthlyImpactResponse(LocalDate month, long rescueCount, BigDecimal estimatedKgSaved, BigDecimal estimatedCo2SavedKg) {

    public static MonthlyImpactResponse from(MonthlyImpactSummary summary) {
        return new MonthlyImpactResponse(
                summary.month(), summary.rescueCount(), summary.totalKgSaved(), summary.totalCo2SavedKg());
    }
}
