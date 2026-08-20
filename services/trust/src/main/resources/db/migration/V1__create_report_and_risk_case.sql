-- Also created by infrastructure/docker/postgres/init/01-schemas-and-extensions.sql
-- in the local dev stack; repeated here (idempotently) so this service's own
-- migrations are self-sufficient against any bare Postgres instance.
CREATE SCHEMA IF NOT EXISTS trust;

-- reporter_user_id/target_user_id reference identity.app_user.id logically,
-- not via cross-schema FK (ADR-003).
CREATE TABLE trust.report (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID NOT NULL,
    reporter_user_id   UUID NOT NULL,
    target_user_id     UUID NOT NULL,
    reason             TEXT NOT NULL,
    description        TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_report_reason CHECK (reason IN ('SAFETY', 'FRAUD', 'HARASSMENT', 'NO_SHOW', 'SPAM', 'OTHER'))
);

CREATE INDEX idx_report_tenant ON trust.report (tenant_id);
CREATE INDEX idx_report_target_user ON trust.report (target_user_id);

ALTER TABLE trust.report ENABLE ROW LEVEL SECURITY;
ALTER TABLE trust.report FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON trust.report
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));

CREATE TABLE trust.risk_case (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL,
    target_user_id        UUID NOT NULL,
    risk_score            NUMERIC NOT NULL,
    risk_factors          TEXT,
    requires_human_review BOOLEAN NOT NULL,
    status                TEXT NOT NULL DEFAULT 'OPEN',
    resolution_action     TEXT,
    resolved_by_user_id   UUID,
    resolved_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_risk_case_status CHECK (status IN ('OPEN', 'RESOLVED'))
);

CREATE INDEX idx_risk_case_tenant ON trust.risk_case (tenant_id);
CREATE INDEX idx_risk_case_target_user ON trust.risk_case (target_user_id);

ALTER TABLE trust.risk_case ENABLE ROW LEVEL SECURITY;
ALTER TABLE trust.risk_case FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON trust.risk_case
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));
