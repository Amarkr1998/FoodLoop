package com.foodloop.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class MatchingEngineTest {

    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void closerAndMoreUrgentScoresHigherThanFartherAndLessUrgent() {
        BigDecimal closeUrgent = MatchingEngine.score(500, 10_000, now.plus(30, ChronoUnit.MINUTES), now);
        BigDecimal farRelaxed = MatchingEngine.score(9_500, 10_000, now.plus(23, ChronoUnit.HOURS), now);

        assertThat(closeUrgent).isGreaterThan(farRelaxed);
    }

    @Test
    void distanceAtRadiusEdgeAndExpiryFarAwayScoresNearZero() {
        BigDecimal score = MatchingEngine.score(10_000, 10_000, now.plus(48, ChronoUnit.HOURS), now);

        assertThat(score.doubleValue()).isEqualTo(0.0);
    }

    @Test
    void zeroDistanceAndAlreadyExpiredScoresMaximum() {
        BigDecimal score = MatchingEngine.score(0, 10_000, now.minus(1, ChronoUnit.HOURS), now);

        assertThat(score.doubleValue()).isEqualTo(1.0);
    }

    @Test
    void scoreIsAlwaysWithinZeroToOne() {
        // Distance beyond the radius and expiry far in the future both push their factors below 0 before clamping.
        BigDecimal score = MatchingEngine.score(50_000, 10_000, now.plus(10, ChronoUnit.DAYS), now);

        assertThat(score.doubleValue()).isBetween(0.0, 1.0);
    }
}
