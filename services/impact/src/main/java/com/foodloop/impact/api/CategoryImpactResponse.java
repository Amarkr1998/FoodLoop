package com.foodloop.impact.api;

import com.foodloop.impact.domain.CategoryImpactSummary;
import java.math.BigDecimal;

/** {@code estimatedKgSaved}/{@code estimatedCo2SavedKg} are approximations — see ImpactCalculator's Javadoc. */
public record CategoryImpactResponse(String foodCategory, long rescueCount, BigDecimal estimatedKgSaved, BigDecimal estimatedCo2SavedKg) {

    public static CategoryImpactResponse from(CategoryImpactSummary summary) {
        return new CategoryImpactResponse(
                summary.foodCategory(), summary.rescueCount(), summary.totalKgSaved(), summary.totalCo2SavedKg());
    }
}
