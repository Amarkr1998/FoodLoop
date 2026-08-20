package com.foodloop.impact.domain;

import java.math.BigDecimal;

/** An aggregate over some slice of {@link RescueRecord} rows — see RescueRecordRepository's JPQL constructor-expression queries. */
public record ImpactSummary(long rescueCount, BigDecimal totalKgSaved, BigDecimal totalCo2SavedKg) {

    public static ImpactSummary empty() {
        return new ImpactSummary(0, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
