-- Also created by infrastructure/docker/postgres/init/01-schemas-and-extensions.sql
-- in the local dev stack; repeated here (idempotently) so this service's own
-- migrations are self-sufficient against any bare Postgres+PostGIS instance
-- (see services/food's V1 migration for the same pattern).
CREATE EXTENSION IF NOT EXISTS postgis;

-- Nullable: only receiver-capable orgs (NGO/FOOD_BANK/CORPORATE/INDIVIDUAL)
-- that have set a location are matchable by the Matching Agent (Phase 7);
-- donor orgs have no reason to set one. No approx/jittered counterpart like
-- food_listing has (§33) — an org's location isn't shown to the public via
-- unauthenticated search the way a listing's is.
ALTER TABLE tenant.organization ADD COLUMN location GEOGRAPHY(Point, 4326);

CREATE INDEX idx_organization_location ON tenant.organization USING GIST (location);
