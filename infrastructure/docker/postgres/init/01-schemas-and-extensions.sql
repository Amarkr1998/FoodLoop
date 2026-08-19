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
