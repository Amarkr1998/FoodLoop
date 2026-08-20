package com.foodloop.pickup.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Two workflows share this one status field: the direct donor/receiver
 * handoff from Phase 4 (SCHEDULED straight through to COMPLETED/NO_SHOW),
 * and the volunteer-mediated flow from Phase 10
 * (UNASSIGNED/ASSIGNED/EN_ROUTE/ARRIVED, matching the shape already
 * sketched in docs/architecture/02-database-design.md). A task starts in
 * SCHEDULED either way; {@link PickupTask#requestVolunteer()} is what
 * switches it onto the second path.
 */
public enum PickupStatus {
    SCHEDULED,
    UNASSIGNED,
    ASSIGNED,
    EN_ROUTE,
    ARRIVED,
    COMPLETED,
    NO_SHOW,
    CANCELLED;

    private static final Map<PickupStatus, Set<PickupStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(PickupStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(SCHEDULED, EnumSet.of(COMPLETED, NO_SHOW, CANCELLED, UNASSIGNED));
        ALLOWED_TRANSITIONS.put(UNASSIGNED, EnumSet.of(ASSIGNED, CANCELLED));
        ALLOWED_TRANSITIONS.put(ASSIGNED, EnumSet.of(EN_ROUTE, UNASSIGNED, CANCELLED));
        ALLOWED_TRANSITIONS.put(EN_ROUTE, EnumSet.of(ARRIVED, UNASSIGNED, CANCELLED));
        ALLOWED_TRANSITIONS.put(ARRIVED, EnumSet.of(COMPLETED, NO_SHOW));
        ALLOWED_TRANSITIONS.put(COMPLETED, EnumSet.noneOf(PickupStatus.class));
        ALLOWED_TRANSITIONS.put(NO_SHOW, EnumSet.noneOf(PickupStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(PickupStatus.class));
    }

    public boolean canTransitionTo(PickupStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
