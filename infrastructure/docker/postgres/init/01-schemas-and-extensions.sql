-- One schema per bounded context (docs/architecture/02-database-design.md).
-- Physical separation now is what makes an eventual per-context service
-- split (ADR-003) a deployment change instead of a data migration.

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS tenant;
CREATE SCHEMA IF NOT EXISTS food;
CREATE SCHEMA IF NOT EXISTS matching;
CREATE SCHEMA IF NOT EXISTS pickup;
CREATE SCHEMA IF NOT EXISTS ngo;
CREATE SCHEMA IF NOT EXISTS trust;
CREATE SCHEMA IF NOT EXISTS impact;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS ai;

-- Session GUC used by row-level security policies (ADR-009). Each service
-- connection sets `app.current_tenant` per request; declaring the default
-- here just documents the contract for local dev.
ALTER DATABASE foodloop SET app.current_tenant = '';

-- Postgres superusers (and any role with BYPASSRLS) silently bypass row-
-- level security entirely, regardless of FORCE ROW LEVEL SECURITY on a
-- table — and the bootstrap POSTGRES_USER role this container creates
-- (foodloop) is a superuser by default, exactly like every official
-- postgres image. If every service connected as that role, the RLS
-- policies in every service's migrations would silently do nothing. Every
-- service therefore connects as this separate, deliberately unprivileged
-- role instead; the bootstrap superuser is for cluster administration only
-- (running this init script, ad hoc ops work), never application traffic.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'foodloop_app') THEN
        CREATE ROLE foodloop_app WITH LOGIN PASSWORD 'foodloop_app_dev_only' NOSUPERUSER NOBYPASSRLS;
    END IF;
END
$$;

GRANT CREATE ON DATABASE foodloop TO foodloop_app;
GRANT ALL PRIVILEGES ON SCHEMA identity, tenant, food, matching, pickup, ngo, trust, impact, notification, ai
    TO foodloop_app;
