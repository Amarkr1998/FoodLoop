-- Also created by infrastructure/docker/postgres/init/01-schemas-and-extensions.sql
-- in the local dev stack; repeated here (idempotently) so this service's own
-- migrations are self-sufficient against any bare Postgres+PostGIS instance,
-- e.g. a Testcontainers-backed integration test.
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE SCHEMA IF NOT EXISTS food;

CREATE TABLE food.food_listing (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL,
    donor_org_id          UUID NOT NULL,
    donor_user_id         UUID NOT NULL,
    title                 TEXT NOT NULL,
    description           TEXT,
    food_category         TEXT NOT NULL,
    dietary_types         TEXT[] NOT NULL DEFAULT '{}',
    allergens             TEXT[] NOT NULL DEFAULT '{}',
    quantity_value        NUMERIC NOT NULL,
    quantity_unit         TEXT NOT NULL,
    estimated_servings    INTEGER,
    preparation_time      TIMESTAMPTZ,
    expiry_time           TIMESTAMPTZ NOT NULL,
    pickup_start_time     TIMESTAMPTZ NOT NULL,
    pickup_end_time       TIMESTAMPTZ NOT NULL,
    location              GEOGRAPHY(Point, 4326) NOT NULL,
    -- Geo-jittered point shown to unauthenticated/unmatched public search
    -- (docs/architecture/06-security-threat-model.md, T13); exact location
    -- is only meant to be revealed to the matched receiver post-claim. This
    -- phase doesn't yet enforce that split at the read-path level — see the
    -- README note on this service — but the column exists now so the
    -- privacy design doesn't need a later migration to retrofit it.
    approx_location       GEOGRAPHY(Point, 4326) NOT NULL,
    status                TEXT NOT NULL DEFAULT 'DRAFT',
    verification_status   TEXT NOT NULL DEFAULT 'UNVERIFIED',
    ai_metadata           JSONB,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    version               BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_food_category CHECK (food_category IN (
        'COOKED_MEAL', 'PACKAGED', 'PRODUCE', 'BAKERY', 'DAIRY', 'BEVERAGE', 'OTHER'
    )),
    CONSTRAINT chk_quantity_unit CHECK (quantity_unit IN (
        'SERVINGS', 'KG', 'BOXES', 'LITERS', 'PIECES'
    )),
    CONSTRAINT chk_food_status CHECK (status IN (
        'DRAFT', 'PUBLISHED', 'AVAILABLE', 'RESERVED', 'CLAIMED',
        'PICKUP_SCHEDULED', 'PICKED_UP', 'COMPLETED',
        'CANCELLED', 'EXPIRED', 'REJECTED', 'NO_SHOW', 'DISPUTED', 'FLAGGED'
    )),
    CONSTRAINT chk_verification_status CHECK (verification_status IN (
        'UNVERIFIED', 'AI_REVIEWED', 'HUMAN_VERIFIED'
    )),
    CONSTRAINT chk_pickup_window CHECK (pickup_end_time > pickup_start_time)
);

CREATE INDEX idx_food_listing_tenant ON food.food_listing (tenant_id);
CREATE INDEX idx_food_listing_tenant_status ON food.food_listing (tenant_id, status, expiry_time);
CREATE INDEX idx_food_listing_donor_org ON food.food_listing (donor_org_id);
-- GIST index is what makes ST_DWithin radius search (§12) sub-linear
-- instead of a full scan; never filter by distance without it.
CREATE INDEX idx_food_listing_approx_location ON food.food_listing USING GIST (approx_location);

ALTER TABLE food.food_listing ENABLE ROW LEVEL SECURITY;
ALTER TABLE food.food_listing FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON food.food_listing
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));

-- receiver_user_id/receiver_org_id reference identity.app_user.id /
-- tenant.organization.id logically, not via cross-schema FK (ADR-003).
CREATE TABLE food.claim (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    food_listing_id     UUID NOT NULL REFERENCES food.food_listing(id),
    receiver_user_id    UUID NOT NULL,
    receiver_org_id     UUID,
    idempotency_key     TEXT NOT NULL,
    status              TEXT NOT NULL DEFAULT 'CONFIRMED',
    claimed_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_claim_status CHECK (status IN ('PENDING', 'CONFIRMED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT uq_claim_idempotency_key UNIQUE (idempotency_key)
);

-- The actual double-claim guard (§13): at most one non-terminal claim per
-- listing, enforced by the database regardless of what application code
-- does — this is the backstop, optimistic locking on food_listing.version
-- (Claim.java / FoodListingService) is the first line of defense.
CREATE UNIQUE INDEX uq_claim_active_per_listing ON food.claim (food_listing_id)
    WHERE status IN ('PENDING', 'CONFIRMED');

CREATE INDEX idx_claim_tenant ON food.claim (tenant_id);
CREATE INDEX idx_claim_receiver ON food.claim (receiver_user_id);

ALTER TABLE food.claim ENABLE ROW LEVEL SECURITY;
ALTER TABLE food.claim FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON food.claim
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));
