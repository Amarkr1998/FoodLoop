-- Phase 10: volunteer-mediated pickup, alongside the existing direct
-- donor/receiver handoff from Phase 4 (see PickupStatus's own Javadoc,
-- which already anticipated these states).
ALTER TABLE pickup.pickup_task ADD COLUMN assigned_volunteer_id UUID;

ALTER TABLE pickup.pickup_task DROP CONSTRAINT chk_pickup_status;
ALTER TABLE pickup.pickup_task ADD CONSTRAINT chk_pickup_status CHECK (status IN (
    'SCHEDULED', 'UNASSIGNED', 'ASSIGNED', 'EN_ROUTE', 'ARRIVED',
    'COMPLETED', 'NO_SHOW', 'CANCELLED'
));

CREATE INDEX idx_pickup_task_volunteer ON pickup.pickup_task (assigned_volunteer_id);

-- user_id references identity.app_user.id logically, not via cross-schema
-- FK (ADR-003) — same reasoning as pickup_task's donor/receiver ids.
CREATE TABLE pickup.volunteer_profile (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL,
    user_id              UUID NOT NULL,
    vehicle_type         TEXT NOT NULL,
    capacity_servings    INTEGER,
    available            BOOLEAN NOT NULL DEFAULT true,
    current_location     GEOGRAPHY(Point, 4326),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    version              BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_vehicle_type CHECK (vehicle_type IN ('ON_FOOT', 'BICYCLE', 'SCOOTER', 'CAR')),
    -- One profile per person: re-registering means updating it, not a second row.
    CONSTRAINT uq_volunteer_profile_user UNIQUE (user_id)
);

CREATE INDEX idx_volunteer_profile_tenant ON pickup.volunteer_profile (tenant_id);
CREATE INDEX idx_volunteer_profile_location ON pickup.volunteer_profile USING GIST (current_location);

ALTER TABLE pickup.volunteer_profile ENABLE ROW LEVEL SECURITY;
ALTER TABLE pickup.volunteer_profile FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON pickup.volunteer_profile
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));
