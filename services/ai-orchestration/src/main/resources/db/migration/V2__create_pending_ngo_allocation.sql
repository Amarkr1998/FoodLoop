-- NGO Coordination Agent's human-approval gate (spec §19): an allocation
-- above the configured quantity threshold is persisted here instead of
-- executed immediately, so POST /api/v1/ai/agent-runs/{id}/escalate/resolve
-- has enough to act on later. agent_run_id/ngo_request_id/food_listing_id
-- reference ai.agent_run.id / ngo.ngo_request.id / food.food_listing.id
-- logically, not via cross-schema FK (ADR-003).
CREATE TABLE ai.pending_ngo_allocation (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    agent_run_id        UUID NOT NULL,
    ngo_request_id      UUID NOT NULL,
    ngo_org_id          UUID NOT NULL,
    food_listing_id     UUID NOT NULL,
    quantity_needed     NUMERIC NOT NULL,
    quantity_unit       TEXT NOT NULL,
    status              TEXT NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at         TIMESTAMPTZ,
    resolved_by_user_id UUID,

    CONSTRAINT uq_pending_ngo_allocation_agent_run UNIQUE (agent_run_id),
    CONSTRAINT chk_pending_ngo_allocation_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_pending_ngo_allocation_tenant ON ai.pending_ngo_allocation (tenant_id);

ALTER TABLE ai.pending_ngo_allocation ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai.pending_ngo_allocation FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON ai.pending_ngo_allocation
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));
