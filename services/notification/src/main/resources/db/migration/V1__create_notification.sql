-- Also created by infrastructure/docker/postgres/init/01-schemas-and-extensions.sql
-- in the local dev stack; repeated here (idempotently) so this service's own
-- migrations are self-sufficient against any bare Postgres instance.
CREATE SCHEMA IF NOT EXISTS notification;

-- recipient_org_id references tenant.organization.id logically, not via
-- cross-schema FK (ADR-003).
CREATE TABLE notification.notification (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    recipient_org_id    UUID NOT NULL,
    channel             TEXT NOT NULL,
    subject             TEXT NOT NULL,
    body                TEXT NOT NULL,
    status              TEXT NOT NULL DEFAULT 'QUEUED',
    source_agent_run_id UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_notification_channel CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')),
    -- QUEUED is the only status this phase ever produces — no delivery
    -- provider exists yet to transition it further (see pom.xml's Javadoc).
    CONSTRAINT chk_notification_status CHECK (status IN ('QUEUED', 'SENT', 'FAILED'))
);

CREATE INDEX idx_notification_tenant ON notification.notification (tenant_id);
CREATE INDEX idx_notification_recipient ON notification.notification (recipient_org_id);

ALTER TABLE notification.notification ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification.notification FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON notification.notification
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));
