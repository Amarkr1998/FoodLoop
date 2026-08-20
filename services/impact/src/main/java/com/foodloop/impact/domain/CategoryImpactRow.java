package com.foodloop.impact.domain;

import java.math.BigDecimal;

/** Spring Data native-query projection backing RescueRecordRepository's category-breakdown queries. */
public interface CategoryImpactRow {

    String getFoodCategory();

    long getRescueCount();

    BigDecimal getTotalKgSaved();

    BigDecimal getTotalCo2SavedKg();
}
