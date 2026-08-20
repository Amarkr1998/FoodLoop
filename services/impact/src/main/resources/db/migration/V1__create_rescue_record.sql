-- Also created by infrastructure/docker/postgres/init/01-schemas-and-extensions.sql
-- in the local dev stack; repeated here (idempotently) so this service's own
-- migrations are self-sufficient against any bare Postgres instance.
CREATE SCHEMA IF NOT EXISTS impact;

-- One row per completed pickup (spec Phase 11) — the read model this whole
-- service is built around. food_listing_id/donor_user_id/donor_org_id/
-- receiver_user_id reference other contexts' rows by value only, no
-- cross-schema FK (ADR-003).
CREATE TABLE impact.rescue_record (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL,
    pickup_task_id           UUID NOT NULL,
    food_listing_id          UUID NOT NULL,
    donor_user_id            UUID NOT NULL,
    donor_org_id             UUID NOT NULL,
    receiver_user_id         UUID NOT NULL,
    food_category            TEXT NOT NULL,
    quantity_value           NUMERIC NOT NULL,
    quantity_unit            TEXT NOT NULL,
    estimated_kg_saved       NUMERIC NOT NULL,
    estimated_co2_saved_kg   NUMERIC NOT NULL,
    completed_at             TIMESTAMPTZ NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Idempotent consumption of pickup.completed.v1 (§7): a redelivered
    -- event must not double-count the same pickup's impact.
    CONSTRAINT uq_rescue_record_pickup_task UNIQUE (pickup_task_id)
);

CREATE INDEX idx_rescue_record_tenant ON impact.rescue_record (tenant_id);
CREATE INDEX idx_rescue_record_donor_user ON impact.rescue_record (donor_user_id);
CREATE INDEX idx_rescue_record_donor_org ON impact.rescue_record (donor_org_id);
CREATE INDEX idx_rescue_record_receiver_user ON impact.rescue_record (receiver_user_id);

ALTER TABLE impact.rescue_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE impact.rescue_record FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON impact.rescue_record
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));
