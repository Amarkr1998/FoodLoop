package com.foodloop.impact.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ImpactCalculatorTest {

    @Test
    void kgUnitPassesThroughOneToOne() {
        assertThat(ImpactCalculator.estimateKgSaved(new BigDecimal("5"), "KG"))
                .isEqualByComparingTo("5.000");
    }

    @Test
    void servingsAreConvertedUsingCoarseFactor() {
        assertThat(ImpactCalculator.estimateKgSaved(new BigDecimal("10"), "SERVINGS"))
                .isEqualByComparingTo("4.000");
    }

    @Test
    void litersAreConvertedOneToOneByMass() {
        assertThat(ImpactCalculator.estimateKgSaved(new BigDecimal("2"), "LITERS"))
                .isEqualByComparingTo("2.000");
    }

    @Test
    void boxesAreConvertedUsingCoarseFactor() {
        assertThat(ImpactCalculator.estimateKgSaved(new BigDecimal("3"), "BOXES"))
                .isEqualByComparingTo("9.000");
    }

    @Test
    void piecesAreConvertedUsingCoarseFactor() {
        assertThat(ImpactCalculator.estimateKgSaved(new BigDecimal("20"), "PIECES"))
                .isEqualByComparingTo("3.000");
    }

    @Test
    void unknownUnitConservativelyEstimatesZero() {
        assertThat(ImpactCalculator.estimateKgSaved(new BigDecimal("100"), "SOMETHING_UNMODELED"))
                .isEqualByComparingTo("0.000");
    }

    @Test
    void co2SavedIsKgSavedTimesBallparkFactor() {
        assertThat(ImpactCalculator.estimateCo2SavedKg(new BigDecimal("4.000")))
                .isEqualByComparingTo("10.000");
    }
}
