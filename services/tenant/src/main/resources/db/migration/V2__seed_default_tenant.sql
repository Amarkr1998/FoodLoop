-- Single-region MVP deployment target (docs/architecture/08-phases-mvp-risks.md)
-- needs exactly one tenant to exist; this is real bootstrap data a single-region
-- production deployment also needs, not throwaway dev fixture data (spec §56).
-- New tenants (additional regions/countries, Phase 12) are created by platform
-- admins, not self-service, so no API creates them yet.
INSERT INTO tenant.tenant (id, name, region_id, country_code, status)
VALUES ('00000000-0000-0000-0000-000000000001', 'FoodLoop Default (India)', 'IN-DEFAULT', 'IN', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;
