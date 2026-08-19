# ADR-001: PostgreSQL + PostGIS as primary database

## Status
Accepted

## Context
FoodLoop's core value proposition is proximity-based matching (nearby food, nearby NGOs, nearby
volunteers) at potentially large scale. Radius search, distance ranking, and geographic indexing
must not be done by loading rows into Java memory (§12 explicitly forbids this).

## Decision
Use PostgreSQL with the PostGIS extension as the single system-of-record database, with geography
columns and GIST indexes for all location data. Each bounded context gets its own schema in the
same cluster initially.

## Consequences
- Geospatial queries are pushed to the database via `ST_DWithin`/`ST_Distance`, indexed and
  paginated.
- Strong transactional guarantees are available for claim/state-machine consistency (avoided by
  eventually-consistent document stores).
- Avoids introducing a second database technology (e.g., MongoDB) without a document-shaped
  requirement that justifies it (§6).
- Trade-off: horizontal write scaling is harder than a sharded NoSQL store; mitigated by read
  replicas, schema-per-context (enabling later physical split), and eventual partitioning of
  high-volume tables (`food_listing` by month, per §02 doc).
