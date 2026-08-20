package com.foodloop.impact.domain;

import java.math.BigDecimal;

/** One slice of an {@link ImpactSummary} broken down by food category, ordered by kg saved descending. */
public record CategoryImpactSummary(String foodCategory, long rescueCount, BigDecimal totalKgSaved, BigDecimal totalCo2SavedKg) {
}
