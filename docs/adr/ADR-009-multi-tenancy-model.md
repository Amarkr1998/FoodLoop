# ADR-009: Row-level security for tenant isolation

## Status
Accepted

## Context
FoodLoop is multi-tenant across organizations, regions, and countries (§31). Cross-tenant data
leakage is the highest-severity threat in the threat model (T1) — an application-layer-only
`WHERE tenant_id = ?` convention is one missed clause away from a breach.

## Decision
Every table carries `tenant_id`. Postgres Row-Level Security policies are enabled per table, keyed
on a session-scoped GUC (`app.current_tenant`) set by a connection interceptor at the start of each
request from the authenticated JWT's tenant claim (cross-checked against DB membership, not trusted
blindly — see ADR-004). Application-layer filtering is retained as a first line of defense; RLS is
the structural backstop.

## Consequences
- A missing `WHERE tenant_id` clause in application code can no longer leak cross-tenant rows —
  the database itself refuses to return them.
- Requires every DB connection path (including migrations, admin tooling, and batch jobs) to
  correctly set or deliberately bypass the session GUC, which must be part of the module template/
  boilerplate so new services don't accidentally omit it.
- Dedicated cross-tenant leakage integration tests are mandatory per context (§31), run in CI.
