-- Also created by infrastructure/docker/postgres/init/01-schemas-and-extensions.sql
-- in the local dev stack; repeated here (idempotently) so this service's own
-- migrations are self-sufficient against any bare Postgres instance, e.g. a
-- Testcontainers-backed integration test or a differently-provisioned
-- environment that hasn't run the infra init script.
CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE identity.app_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    keycloak_id     UUID NOT NULL,
    email           TEXT NOT NULL,
    phone           TEXT,
    display_name    TEXT NOT NULL,
    locale          TEXT NOT NULL DEFAULT 'en',
    status          TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_app_user_keycloak_id UNIQUE (keycloak_id),
    CONSTRAINT uq_app_user_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT chk_app_user_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_app_user_tenant ON identity.app_user (tenant_id);

-- Row-level security is the structural tenant-isolation backstop (ADR-009,
-- threat T1): even a query that forgets a WHERE tenant_id clause cannot see
-- another tenant's rows. FORCE is required, not optional — without it the
-- table owner (the role every service migration and query runs as locally)
-- bypasses RLS entirely, silently defeating the policy.
ALTER TABLE identity.app_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.app_user FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON identity.app_user
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));
