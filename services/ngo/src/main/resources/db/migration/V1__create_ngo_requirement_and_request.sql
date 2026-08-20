-- Also created by infrastructure/docker/postgres/init/01-schemas-and-extensions.sql
-- in the local dev stack; repeated here (idempotently) so this service's own
-- migrations are self-sufficient against any bare Postgres instance.
CREATE SCHEMA IF NOT EXISTS ngo;

-- ngo_org_id references tenant.organization.id logically, not via
-- cross-schema FK (ADR-003). One row per NGO org — NGO-specific operating
-- data, distinct from Organization & Tenant's generic organization record.
CREATE TABLE ngo.ngo_requirement (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              UUID NOT NULL,
    ngo_org_id             UUID NOT NULL,
    preferred_categories   TEXT[],
    dietary_restrictions   TEXT[],
    capacity_per_week      INTEGER,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_ngo_requirement_org UNIQUE (ngo_org_id)
);

CREATE INDEX idx_ngo_requirement_tenant ON ngo.ngo_requirement (tenant_id);

ALTER TABLE ngo.ngo_requirement ENABLE ROW LEVEL SECURITY;
ALTER TABLE ngo.ngo_requirement FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON ngo.ngo_requirement
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));

-- matched_proposal_id/matched_food_listing_id reference
-- matching.match_proposal.id / food.food_listing.id logically, not via
-- cross-schema FK (ADR-003).
CREATE TABLE ngo.ngo_request (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL,
    ngo_org_id               UUID NOT NULL,
    food_category            TEXT NOT NULL,
    quantity_needed          NUMERIC NOT NULL,
    quantity_unit            TEXT NOT NULL,
    needed_before            TIMESTAMPTZ NOT NULL,
    notes                    TEXT,
    status                   TEXT NOT NULL DEFAULT 'OPEN',
    matched_proposal_id      UUID,
    matched_food_listing_id  UUID,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                  BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_ngo_request_status CHECK (status IN ('OPEN', 'MATCHED', 'FULFILLED', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX idx_ngo_request_tenant ON ngo.ngo_request (tenant_id);
CREATE INDEX idx_ngo_request_org ON ngo.ngo_request (ngo_org_id);
CREATE INDEX idx_ngo_request_status_deadline ON ngo.ngo_request (status, needed_before);

ALTER TABLE ngo.ngo_request ENABLE ROW LEVEL SECURITY;
ALTER TABLE ngo.ngo_request FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON ngo.ngo_request
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));
