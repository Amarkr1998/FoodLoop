# Event Catalog

## Bus assignment rule (ADR-002)

**Kafka** = durable domain facts other contexts (and analytics/AI) need to react to, replay, or
audit. **RabbitMQ** = operational work items with queue semantics (one worker consumes and
finishes it, retried/DLQ'd on failure, no replay need). A domain fact is never pushed to both buses
for the same workflow; if a Kafka consumer needs "send an email," it publishes/enqueues a RabbitMQ
job as its side effect — that's the one legitimate crossover point.

## Kafka topics (domain events)

| Topic | Producer | Key consumers | Payload essentials |
|---|---|---|---|
| `food.listed.v1` | Food | Matching, AI Orchestration, Impact(none yet) | listingId, tenantId, donorOrgId, category, expiryTime, location |
| `food.updated.v1` | Food | Matching | listingId, changedFields |
| `food.claimed.v1` | Food | Pickup, Notification | listingId, claimId, receiverUserId |
| `food.reserved.v1` | Food | Pickup, Matching | listingId, claimId |
| `food.expiring.v1` | Food (scheduled check) | Matching (Rescue Agent trigger), Notification | listingId, minutesToExpiry |
| `food.expired.v1` | Food | Impact, Matching | listingId |
| `food.cancelled.v1` | Food | Matching, Notification | listingId, reason |
| `pickup.created.v1` | Pickup | Notification, AI Orchestration | pickupId, claimId |
| `pickup.completed.v1` | Pickup | Impact, Notification | pickupId, actualServings |
| `pickup.no_show.v1` | Pickup | Trust & Safety, Notification | pickupId, partyAtFault |
| `donation.completed.v1` | Pickup(on completion) | Impact | listingId, servings, weightKg |
| `ngo.request.created.v1` | NGO | Matching, AI Orchestration | requestId, ngoOrgId, neededServings, needsBefore |
| `match.created.v1` | Matching | Pickup(pending accept), Notification | matchId, listingId, targetType, score |
| `match.accepted.v1` | Matching | Pickup, Notification | matchId |
| `match.rejected.v1` | Matching | AI Orchestration (retry/expand radius) | matchId, reason |
| `risk.detected.v1` | Trust & Safety | Identity(review queue), AI Orchestration | subjectUserId, riskScore, factors |
| `safety.flagged.v1` | Trust & Safety / AI Orchestration | Food(auto-hold), Notification(ops) | listingId, severity, requiresHumanReview |
| `agent.run.completed.v1` | AI Orchestration | Analytics, Audit | agentRunId, outcome, escalated |

All Kafka messages carry an envelope: `eventId (uuid), eventType, eventVersion, tenantId,
occurredAt, correlationId, traceId, producer, payload`. Consumers persist processed `eventId`s
(or use a dedup table keyed by `eventId`) to stay idempotent under Kafka's at-least-once delivery.
Schema evolution: Avro/JSON-Schema in a registry, additive-only within a major `eventVersion`;
breaking changes ship as `food.listed.v2` with dual-publish during migration.

## RabbitMQ queues (operational work)

| Queue | Enqueued by | Purpose | Retry/DLQ |
|---|---|---|---|
| `notification.send` | Notification service (fan-in consumer of many Kafka topics) | push/email/SMS dispatch | exponential backoff x5, DLQ → ops alert |
| `media.process` | Food (on image upload) | thumbnailing, moderation pre-scan | retry x3, DLQ → manual review |
| `report.generate` | Impact | async report generation | retry x3, DLQ → ops |
| `external.retry` | any context | generic wrapper for flaky external calls (maps, SMS gateway) | backoff, capped, DLQ |
| `ai.tool.async` | AI Orchestration | long-running tool executions (image classification batch) | retry x2, DLQ → human escalation queue |

Every queue has a matching `<queue>.dlq`; DLQ depth is an alerted metric (§40).
