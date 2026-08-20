package com.foodloop.matching.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * The deterministic scoring policy (spec §17, §45's hard filters) — plain
 * Java, no model call, unit-testable in isolation. An agent's LLM step may
 * re-rank/explain among candidates this produces, but it never computes or
 * overrides a score itself (docs/architecture/05-ai-agent-architecture.md
 * §9's "deterministic path is the default, AI decorates it").
 *
 * <p>Two weighted factors for now — proximity and urgency (time-to-expiry).
 * Quantity/dietary fit isn't scored yet because neither Organization nor any
 * receiver-need record captures a quantity or dietary preference to compare
 * against (see AskUserQuestion decision for Phase 7's scope); adding that
 * factor is a natural extension once that data exists, not a redesign.
 */
public final class MatchingEngine {

    private static final double DISTANCE_WEIGHT = 0.7;
    private static final double URGENCY_WEIGHT = 0.3;
    private static final double URGENCY_HORIZON_MINUTES = 24 * 60.0;

    private MatchingEngine() {
    }

    /**
     * @param distanceMeters distance from the listing to the candidate receiver
     * @param radiusMeters   the search radius the candidate was found within — the normalization basis for the distance factor
     * @param expiryTime     the listing's expiry
     * @param now            evaluation time (passed explicitly, not read from the clock, so this stays pure and testable)
     * @return a score in [0, 1], rounded to 4 decimal places
     */
    public static BigDecimal score(double distanceMeters, double radiusMeters, Instant expiryTime, Instant now) {
        double distanceFactor = clamp(1 - (distanceMeters / radiusMeters));
        long minutesToExpiry = Duration.between(now, expiryTime).toMinutes();
        double urgencyFactor = clamp(1 - (minutesToExpiry / URGENCY_HORIZON_MINUTES));

        double raw = DISTANCE_WEIGHT * distanceFactor + URGENCY_WEIGHT * urgencyFactor;
        return BigDecimal.valueOf(raw).setScale(4, RoundingMode.HALF_UP);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
