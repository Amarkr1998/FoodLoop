package com.foodloop.impact.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Spring Data native-query projection backing RescueRecordRepository's monthly-trend queries. */
public interface MonthlyImpactRow {

    LocalDate getMonth();

    long getRescueCount();

    BigDecimal getTotalKgSaved();

    BigDecimal getTotalCo2SavedKg();
}
