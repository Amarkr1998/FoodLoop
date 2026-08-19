CREATE EXTENSION IF NOT EXISTS postgis;
CREATE SCHEMA IF NOT EXISTS pickup;

-- claim_id/food_listing_id/donor_user_id/receiver_user_id reference
-- food.claim / food.food_listing / identity.app_user by value only, no
-- cross-schema FK (ADR-003) — this service is boot-strapped entirely from
-- the food.claimed.v1 event, never a direct call into Food's schema.
CREATE TABLE pickup.pickup_task (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    claim_id                UUID NOT NULL,
    food_listing_id         UUID NOT NULL,
    donor_user_id           UUID NOT NULL,
    receiver_user_id        UUID NOT NULL,
    status                  TEXT NOT NULL DEFAULT 'SCHEDULED',
    scheduled_window_start  TIMESTAMPTZ NOT NULL,
    scheduled_window_end    TIMESTAMPTZ NOT NULL,
    pickup_location         GEOGRAPHY(Point, 4326) NOT NULL,
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                 BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_pickup_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'NO_SHOW', 'CANCELLED')),
    -- Idempotent consumption of food.claimed.v1 (§7): a redelivered event
    -- must not create a second task for the same claim.
    CONSTRAINT uq_pickup_task_claim UNIQUE (claim_id)
);

CREATE INDEX idx_pickup_task_tenant ON pickup.pickup_task (tenant_id);
CREATE INDEX idx_pickup_task_donor ON pickup.pickup_task (donor_user_id);
CREATE INDEX idx_pickup_task_receiver ON pickup.pickup_task (receiver_user_id);

ALTER TABLE pickup.pickup_task ENABLE ROW LEVEL SECURITY;
ALTER TABLE pickup.pickup_task FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON pickup.pickup_task
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));
