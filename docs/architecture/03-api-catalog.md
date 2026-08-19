# API Catalog (v1)

All routes under `/api/v1`. All list endpoints are paginated (`page`, `size`, cursor for
high-volume feeds), filterable, and sortable. All mutating endpoints accept an `Idempotency-Key`
header where retries are plausible (claim, pickup confirm, agent-triggered writes). Errors use the
consistent envelope from §42.

## Identity & Tenant
```
POST   /auth/register
POST   /auth/otp/request
POST   /auth/otp/verify
GET    /users/me
PATCH  /users/me
POST   /organizations
GET    /organizations/{id}
PATCH  /organizations/{id}
POST   /organizations/{id}/members
GET    /organizations/{id}/members
```

## Food
```
POST   /food-listings                        -- create DRAFT
POST   /food-listings/{id}/publish
PATCH  /food-listings/{id}
GET    /food-listings/{id}
GET    /food-listings                        -- search: lat, lng, radiusKm, category, dietaryType,
                                                  minQty, pickupBefore, verifiedOnly, donorType
POST   /food-listings/{id}/images
POST   /food-listings/{id}/claim              -- idempotent, optimistic-lock guarded
POST   /food-listings/{id}/cancel
GET    /food-listings/{id}/state-history
```

## Matching
```
GET    /matches?foodListingId=...
GET    /matches?ngoRequestId=...
POST   /matches/{id}/accept
POST   /matches/{id}/reject
POST   /matches/proposals                     -- internal/service-to-service: agent-created proposals
```

## Pickup
```
POST   /pickups                                -- created from accepted match
POST   /pickups/{id}/assign-volunteer
POST   /pickups/{id}/confirm-arrival
POST   /pickups/{id}/complete
POST   /pickups/{id}/report-no-show
GET    /pickups?status=&volunteerId=&donorOrgId=
GET    /pickups/{id}/route                     -- deterministic route calc
```

## NGO
```
POST   /ngos/verification-request
GET    /ngos/{id}
POST   /ngo-requests
GET    /ngo-requests?ngoId=&status=
POST   /ngo-requests/{id}/cancel
```

## Notification
```
GET    /notifications
PATCH  /notifications/preferences
POST   /notifications/{id}/mark-read
```

## Impact
```
GET    /impact/me
GET    /impact/organizations/{id}
GET    /impact/community?region=
```

## Trust & Safety
```
POST   /reports
GET    /risk-cases?status=
POST   /risk-cases/{id}/review                 -- human-only, requires ROLE_TRUST_OPS
GET    /safety-flags?foodListingId=
POST   /safety-flags/{id}/resolve
```

## AI Orchestration (mostly internal/service, some user-facing)
```
POST   /ai/food-listings/{id}/analyze          -- triggers Food Intelligence Agent, returns suggestion
POST   /ai/matching/suggest                    -- triggers Matching Agent reasoning pass
GET    /ai/agent-runs/{id}
GET    /ai/agent-runs?agentName=&status=&tenantId=   -- admin/ops only
POST   /ai/agent-runs/{id}/escalate/resolve     -- human-in-the-loop resolution
```

## Admin (separate router prefix, ROLE_ADMIN/ROLE_TRUST_OPS gated)
```
/api/v1/admin/users
/api/v1/admin/organizations
/api/v1/admin/food-listings
/api/v1/admin/agent-runs
/api/v1/admin/config
/api/v1/admin/audit-logs
```

Every endpoint is documented in OpenAPI under `packages/shared-contracts/openapi/<context>.yaml`;
this catalog is the index, the YAML is the contract of record.
