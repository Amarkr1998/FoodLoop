package com.foodloop.impact.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Converts a listing's donor-declared quantity into an estimated kg-of-food
 * figure and a rough CO2e-avoided figure. Both are explicitly
 * <strong>approximations</strong>, not certified or precise measurements:
 * per-unit kg conversions are coarse, order-of-magnitude estimates (a "box"
 * or a "serving" varies hugely in reality), and the CO2e factor is one
 * commonly-cited ballpark figure for food waste's greenhouse gas footprint,
 * not a per-food-type life-cycle assessment. Never present these numbers to
 * a user as exact — every response surfacing them is expected to label them
 * as estimates (see ImpactController).
 */
public final class ImpactCalculator {

    /** Coarse quantity-unit -> kg conversion factors; KG needs none (1:1). */
    private static final Map<String, BigDecimal> KG_PER_UNIT = Map.of(
            "SERVINGS", new BigDecimal("0.4"),
            "LITERS", new BigDecimal("1.0"),
            "BOXES", new BigDecimal("3.0"),
            "PIECES", new BigDecimal("0.15"));

    /** A commonly-cited ballpark for avoided-food-waste CO2e per kg — not a precise LCA figure. */
    private static final BigDecimal CO2E_KG_PER_KG_FOOD = new BigDecimal("2.5");

    private ImpactCalculator() {
    }

    public static BigDecimal estimateKgSaved(BigDecimal quantityValue, String quantityUnit) {
        if ("KG".equals(quantityUnit)) {
            return quantityValue.setScale(3, RoundingMode.HALF_UP);
        }
        BigDecimal factor = KG_PER_UNIT.getOrDefault(quantityUnit, BigDecimal.ZERO);
        return quantityValue.multiply(factor).setScale(3, RoundingMode.HALF_UP);
    }

    public static BigDecimal estimateCo2SavedKg(BigDecimal kgSaved) {
        return kgSaved.multiply(CO2E_KG_PER_KG_FOOD).setScale(3, RoundingMode.HALF_UP);
    }
}
