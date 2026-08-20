package com.foodloop.impact.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One slice of an {@link ImpactSummary} broken down by calendar month (first-of-month), oldest first. */
public record MonthlyImpactSummary(LocalDate month, long rescueCount, BigDecimal totalKgSaved, BigDecimal totalCo2SavedKg) {
}
