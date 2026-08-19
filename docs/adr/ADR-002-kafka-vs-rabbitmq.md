# ADR-002: Kafka for domain events, RabbitMQ for operational queues

## Status
Accepted

## Context
The spec requires both durable, replayable domain-event streaming (matching, rescue, audit) and
simple task-queue semantics (send a notification, process an image) without duplicating the same
workflow across two buses.

## Decision
Kafka carries durable domain facts (`FOOD_LISTED`, `MATCH_CREATED`, `RISK_DETECTED`, etc.) that
multiple consumers (including future analytics/AI consumers) need to read, potentially replay, and
audit. RabbitMQ carries operational work items consumed by exactly one worker to completion
(notification dispatch, media processing, report generation, retrying flaky external calls).

## Consequences
- A Kafka consumer that needs a side-effect action (e.g., "email the donor") enqueues a RabbitMQ
  job rather than re-publishing to Kafka — the one legitimate crossover point, documented in the
  event catalog.
- Consumers on both buses are idempotent (Kafka at-least-once; RabbitMQ redelivery on nack) via
  `eventId` dedup / idempotency keys.
- Two messaging systems to operate is real overhead; accepted because the semantics genuinely
  differ (replay/audit vs. work-queue) and conflating them would force awkward workarounds on one
  side or the other.
