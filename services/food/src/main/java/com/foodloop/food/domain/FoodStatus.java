package com.foodloop.food.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The food listing state machine (spec §11). Deterministic and exhaustive —
 * an invalid transition must fail safely, never silently no-op or fall
 * through to an unintended state. PICKUP_SCHEDULED onward is driven by the
 * Pickup context (Phase 4); the edges into/out of those states are declared
 * here now since this enum is the one canonical status field, but nothing
 * in Food itself triggers them yet.
 */
public enum FoodStatus {
    DRAFT,
    PUBLISHED,
    AVAILABLE,
    RESERVED,
    CLAIMED,
    PICKUP_SCHEDULED,
    PICKED_UP,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    REJECTED,
    NO_SHOW,
    DISPUTED,
    FLAGGED;

    private static final Map<FoodStatus, Set<FoodStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(FoodStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(DRAFT, EnumSet.of(PUBLISHED, CANCELLED, REJECTED));
        ALLOWED_TRANSITIONS.put(PUBLISHED, EnumSet.of(AVAILABLE, CANCELLED, REJECTED));
        ALLOWED_TRANSITIONS.put(AVAILABLE, EnumSet.of(RESERVED, CLAIMED, CANCELLED, EXPIRED, FLAGGED));
        ALLOWED_TRANSITIONS.put(RESERVED, EnumSet.of(CLAIMED, AVAILABLE, EXPIRED, FLAGGED));
        ALLOWED_TRANSITIONS.put(CLAIMED, EnumSet.of(PICKUP_SCHEDULED, CANCELLED, DISPUTED));
        ALLOWED_TRANSITIONS.put(PICKUP_SCHEDULED, EnumSet.of(PICKED_UP, NO_SHOW));
        ALLOWED_TRANSITIONS.put(PICKED_UP, EnumSet.of(COMPLETED, DISPUTED));
        ALLOWED_TRANSITIONS.put(COMPLETED, EnumSet.noneOf(FoodStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(FoodStatus.class));
        ALLOWED_TRANSITIONS.put(EXPIRED, EnumSet.noneOf(FoodStatus.class));
        ALLOWED_TRANSITIONS.put(REJECTED, EnumSet.noneOf(FoodStatus.class));
        ALLOWED_TRANSITIONS.put(NO_SHOW, EnumSet.of(AVAILABLE, CANCELLED));
        ALLOWED_TRANSITIONS.put(DISPUTED, EnumSet.noneOf(FoodStatus.class));
        ALLOWED_TRANSITIONS.put(FLAGGED, EnumSet.noneOf(FoodStatus.class));
    }

    public boolean canTransitionTo(FoodStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
