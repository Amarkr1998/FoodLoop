-- Also created by infrastructure/docker/postgres/init/01-schemas-and-extensions.sql
-- in the local dev stack; repeated here (idempotently) so this service's own
-- migrations are self-sufficient against any bare Postgres instance.
CREATE SCHEMA IF NOT EXISTS matching;

-- food_listing_id/receiver_org_id reference food.food_listing.id /
-- tenant.organization.id logically, not via cross-schema FK (ADR-003).
CREATE TABLE matching.match_proposal (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    food_listing_id     UUID NOT NULL,
    receiver_org_id     UUID NOT NULL,
    distance_meters     NUMERIC NOT NULL,
    score               NUMERIC NOT NULL,
    ai_rationale        TEXT,
    status              TEXT NOT NULL DEFAULT 'PROPOSED',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_match_status CHECK (status IN ('PROPOSED', 'ACCEPTED', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX idx_match_proposal_tenant ON matching.match_proposal (tenant_id);
CREATE INDEX idx_match_proposal_listing ON matching.match_proposal (food_listing_id);
CREATE INDEX idx_match_proposal_receiver ON matching.match_proposal (receiver_org_id);

-- At most one active (PROPOSED) proposal per (listing, org) pair — a
-- duplicate PROPOSED row would only ever be redundant, never meaningfully
-- different, since MatchingService re-derives score/distance every call.
-- A pair can still re-propose after rejection/expiry: this only blocks two
-- simultaneously-open proposals for the same pair, not the full history.
CREATE UNIQUE INDEX uq_match_active_pair ON matching.match_proposal (food_listing_id, receiver_org_id)
    WHERE status = 'PROPOSED';

ALTER TABLE matching.match_proposal ENABLE ROW LEVEL SECURITY;
ALTER TABLE matching.match_proposal FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON matching.match_proposal
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));
