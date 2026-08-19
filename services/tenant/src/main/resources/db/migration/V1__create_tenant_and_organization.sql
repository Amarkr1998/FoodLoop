CREATE SCHEMA IF NOT EXISTS tenant;

-- The tenant row IS the isolation boundary (region/country deployment
-- scope, ADR-009) rather than something scoped BY a tenant, so unlike
-- every other table in this platform it deliberately has no tenant_id
-- column and no RLS policy. It's small, admin-managed reference data;
-- write access is locked down at the API layer (ADMIN role only), not RLS.
CREATE TABLE tenant.tenant (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          TEXT NOT NULL,
    region_id     TEXT NOT NULL,
    country_code  TEXT NOT NULL,
    status        TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_tenant_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE tenant.organization (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenant.tenant(id),
    name                  TEXT NOT NULL,
    type                  TEXT NOT NULL,
    verification_status   TEXT NOT NULL DEFAULT 'UNVERIFIED',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    version               BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_org_type CHECK (type IN (
        'DONOR_RESTAURANT', 'DONOR_HOTEL', 'DONOR_GROCERY', 'DONOR_CATERER',
        'DONOR_HOME_COOK', 'NGO', 'FOOD_BANK', 'CORPORATE', 'INDIVIDUAL'
    )),
    CONSTRAINT chk_org_verification CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED'))
);

CREATE INDEX idx_organization_tenant ON tenant.organization (tenant_id);

ALTER TABLE tenant.organization ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant.organization FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON tenant.organization
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));

-- user_id references identity.app_user.id logically, not via a DB foreign
-- key: the two contexts have independent schemas/lifecycles by design
-- (ADR-003) so a cross-schema FK would violate that boundary. tenant_id is
-- denormalized here (rather than joined from organization) because RLS
-- policies need it directly on this table too.
CREATE TABLE tenant.org_member (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    organization_id   UUID NOT NULL REFERENCES tenant.organization(id),
    user_id           UUID NOT NULL,
    role              TEXT NOT NULL DEFAULT 'MEMBER',
    joined_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_org_member UNIQUE (organization_id, user_id),
    CONSTRAINT chk_org_member_role CHECK (role IN ('ORG_ADMIN', 'MEMBER'))
);

CREATE INDEX idx_org_member_tenant ON tenant.org_member (tenant_id);
CREATE INDEX idx_org_member_org ON tenant.org_member (organization_id);
CREATE INDEX idx_org_member_user ON tenant.org_member (user_id);

ALTER TABLE tenant.org_member ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant.org_member FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON tenant.org_member
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));
