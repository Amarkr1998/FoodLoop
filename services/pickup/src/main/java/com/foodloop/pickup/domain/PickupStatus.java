package com.foodloop.pickup.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Donor/receiver direct-handoff workflow (spec Phase 4) — no volunteer
 * intermediary yet (that's Phase 10), so this is simpler than the full
 * pickup_task lifecycle sketched in docs/architecture/02-database-design.md
 * (UNASSIGNED/ASSIGNED/EN_ROUTE/ARRIVED are volunteer-assignment states).
 */
public enum PickupStatus {
    SCHEDULED,
    COMPLETED,
    NO_SHOW,
    CANCELLED;

    private static final Map<PickupStatus, Set<PickupStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(PickupStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(SCHEDULED, EnumSet.of(COMPLETED, NO_SHOW, CANCELLED));
        ALLOWED_TRANSITIONS.put(COMPLETED, EnumSet.noneOf(PickupStatus.class));
        ALLOWED_TRANSITIONS.put(NO_SHOW, EnumSet.noneOf(PickupStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(PickupStatus.class));
    }

    public boolean canTransitionTo(PickupStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
