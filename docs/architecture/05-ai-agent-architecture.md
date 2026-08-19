# AI & Agent Architecture

## 1. Layering

```
Spring AI (provider abstraction + structured output binding)
   │
   ├── Provider adapters: AzureOpenAIProvider, AzureAIFoundryProvider, MockProvider(dev)
   │
LangGraph (agent graphs: state machine of nodes, not a chat loop)
   │
   ├── Agent graphs: FoodIntelligenceAgent, MatchingAgent, RescueAgent, NgoCoordinationAgent,
   │                  PickupAgent, TrustRiskAgent, SafetyAgent
   │
Tool Layer (typed, authorized, audited — see §4)
   │
Business Service APIs (Food, Matching, Pickup, NGO, Trust&Safety, Notification, Impact)
```

The domain layer never imports an AI type. Agents are consumers of business APIs through the same
authenticated boundary a normal client would use (scoped service-account JWT with an explicit tool
allowlist), which is what makes §15 ("never give an LLM unrestricted database access") structurally
true rather than a convention.

## 2. Standard agent loop (LangGraph state graph, applies to every agent)

```
Observe → Retrieve Context (RAG/DB via tools) → Reason → Select Tool → Execute Tool
   → Validate Result → Continue Workflow → Escalate if Necessary
```

Each node is a LangGraph node; `Validate Result` is a deterministic guard (schema check +
business-rule check) that can force a retry, a different tool, or an escalation — it is not another
LLM call grading itself. `Escalate if Necessary` routes to a `human_review` terminal node that
writes a `trust.risk_case`-style or `ai.agent_run.escalated=true` record and stops — the agent
never takes the risky action itself.

## 3. Per-agent design

### Food Intelligence Agent (§16)
- **Trigger**: `POST /ai/food-listings/{id}/analyze` (donor-initiated, during listing creation) or
  async on `food.listed.v1` for a background quality pass.
- **Input**: free text + optional image(s).
- **Tools**: `getFoodListing`, `classifyFoodImage` (vision call), `updateFoodListingAiMetadata`.
- **Output contract** (structured, Spring AI `@JsonSchema`-bound, validated before persistence):
  `category, dietaryType[], allergens[], estimatedServings, urgency, missingInformation[],
  suggestedDescription, confidence`.
- **Deterministic guardrail**: the listing cannot move DRAFT→PUBLISHED until required fields
  (expiry, pickup window, quantity) are present, regardless of what the agent suggests. The donor
  always confirms before publish (§44) — the agent never auto-publishes.

### Smart Matching Agent (§17)
- **Trigger**: `food.listed.v1`, `ngo.request.created.v1`, or on-demand `POST /ai/matching/suggest`.
- **Tools**: `searchNearbyFood`, `searchNearbyNGOs`, `getNGORequirements`, `checkFoodEligibility`,
  `calculateDistance`, `createMatchProposal`.
- **Split of responsibility**: a deterministic `MatchingEngine` (plain Java, unit-testable) computes
  the candidate set and a base score from distance/expiry/quantity/eligibility (hard filters — see
  §45). The agent's LLM step only re-ranks/explains among *already-eligible* candidates and writes
  `ai_rationale` — it cannot introduce an ineligible candidate because `createMatchProposal` itself
  re-validates eligibility server-side before insert (tool-side validation, not just prompt trust).

### Food Rescue Agent (§18)
- **Trigger**: scheduled job emitting `food.expiring.v1` at configurable thresholds (e.g., T-4h,
  T-1h) per listing.
- **Workflow**: matches §18 exactly — monitor → detect risk → find receivers → rank → notify → on
  no response within a configurable window, expand radius → volunteer escalation → human ops
  escalation. Each expansion step is a graph edge with its own timeout, not a retry loop inside one
  LLM call, so the process is observable and resumable (LangGraph checkpointing) across a service
  restart.

### NGO Coordination Agent (§19)
- **Trigger**: `ngo.request.created.v1` or scheduled sweep for open requests nearing `needed_before`.
- **Tools**: `getNGORequirements`, `searchNearbyFood`, `checkFoodEligibility`, `createMatchProposal`,
  `schedulePickup` (only after acceptance).
- **Human approval gate**: allocations above a configurable quantity/value threshold, or any
  allocation the deterministic feasibility check can't fully clear, route to
  `POST /ai/agent-runs/{id}/escalate/resolve` for NGO-ops approval before `schedulePickup` executes.

### Pickup Agent (§20)
- **Tools**: `findAvailableVolunteers`, `calculateRoute` (delegates to deterministic routing, not
  LLM math), `sendNotification`, `updateFoodStatus` (pickup sub-states).
- Detects delay via a deterministic timer against `scheduled_window`; the agent's job is choosing
  *who* to notify and whether to recommend reassignment, not computing ETAs.

### Trust & Risk Agent (§21)
- **Tools**: read-only signal tools (`getUserBehaviorSignals`, `getReportHistory`) +
  `createRiskCase`. It **cannot** suspend or ban — `createRiskCase` only ever writes a proposal row
  with `requiresHumanReview=true` when the deterministic score crosses a threshold; enforcement is
  a separate, human-triggered Identity-context action (§26).
- **Scoring**: a deterministic weighted-signal function (not LLM-generated) produces `riskScore`;
  the LLM's job is only to summarize `riskFactors` into a human-readable rationale for the reviewer.

### Safety Agent (§22)
- Runs alongside Food Intelligence on listing creation and on Trust reports. Flags missing info,
  checks against RAG-retrieved platform rules, and can set `safety_flag.requires_human_review=true`
  which puts a listing on hold (deterministic guard reads this flag, not the LLM's raw text). It
  never asserts legal/medical certification — that's a hard prompt/system constraint plus an output
  validator that rejects any generated text matching a certification-claim pattern.

## 4. Tool layer (§24)

Every tool is a Spring bean implementing a common `AgentTool<I,O>` contract:

```
authorize(callerContext, input) -> AuthorizationResult   // scope + tenant + agent-permission check
validateInput(input)                                     // schema + business validation
execute(input) -> output                                 // delegates to the owning context's
                                                           // application service (same code path
                                                           // a human-triggered API call would use)
validateOutput(output)
audit(callerContext, input, output, result)               // -> ai.tool_call row, always, even on deny
```

Rate limiting and idempotency (where the underlying action is a mutation — `reserveFood`,
`schedulePickup`, `createMatchProposal`) are enforced by the *same* idempotency-key/optimistic-lock
mechanism the REST layer uses, because the tool calls the same application service. Tools:
`searchNearbyFood, getFoodListing, checkFoodEligibility, getNGORequirements, searchNearbyNGOs,
calculateDistance, findAvailableVolunteers, calculateRoute, createMatchProposal, reserveFood,
schedulePickup, sendNotification, updateFoodStatus, createSafetyCase, createRiskCase,
calculateImpact, classifyFoodImage, getUserBehaviorSignals, getReportHistory`.

## 5. Agent permission matrix (§25)

| Agent | Read tools | Write tools | Cannot do |
|---|---|---|---|
| Food Intelligence | getFoodListing, classifyFoodImage | updateFoodListingAiMetadata | publish listing, change status |
| Matching | searchNearbyFood, searchNearbyNGOs, getNGORequirements, checkFoodEligibility, calculateDistance | createMatchProposal | reserve food, schedule pickup |
| Rescue | getFoodListing (expiring set), searchNearbyFood/NGOs | sendNotification, createMatchProposal, createSafetyCase(escalation marker) | force-claim, ban, override donor |
| NGO Coordination | getNGORequirements, searchNearbyFood, checkFoodEligibility | createMatchProposal, schedulePickup (post-approval only) | approve its own high-value allocation |
| Pickup | findAvailableVolunteers, calculateRoute | sendNotification, updateFoodStatus (pickup substates only) | modify food listing content |
| Trust & Risk | getUserBehaviorSignals, getReportHistory | createRiskCase | suspend/ban, modify listings |
| Safety | getFoodListing, RAG policy retrieval | createSafetyCase | delete listing, ban user, issue legal guidance |

No agent identity carries `ROLE_ADMIN`. Each agent authenticates as a distinct service principal
with a per-agent scope list enforced at the tool-authorization step, independent of any given
LLM's behavior — a prompt-injected agent can *ask* for `reserveFood` but the Rescue Agent's service
principal simply doesn't have that scope, so the call is denied and audited, not silently allowed.

## 6. Human-in-the-loop (§26)

```
Agent proposes action
  → deterministic risk gate (threshold config per action type, per tenant)
     Low risk  → tool executes immediately (still fully audited)
     High risk → ai.agent_run.escalated = true, action NOT executed,
                 surfaced in AI Ops Dashboard escalation queue
                     → human Approve  → tool executes with human's identity attached as approver
                     → human Reject   → agent_run closed, no side effect
```
Always human-gated regardless of computed risk: user suspension, permanent ban, high-severity
safety cases, fraud enforcement, allocations above the configured value/quantity threshold, and any
action touching more than one tenant.

## 7. RAG architecture (§27)

```
Document → Parse (per source type) → Chunk (semantic, ~400-800 tok, overlap) → Embed (Azure OpenAI
embeddings) → Store (pgvector, ai.document_chunk) → Retrieve (pre-filter by tenant/region/language,
then ANN) → Rerank (cross-encoder or LLM rerank on top-k) → Agent context
```
Documents carry `version, source, region, language, effectiveDate`. Retrieval always pre-filters by
the requesting tenant's `region`/`language` before similarity search — never a global unscoped
search — and the Safety/NGO Coordination agents attach the retrieved `source_doc_id + version` as a
citation in their `outcome_summary`, so a policy-based decision is always traceable to the document
version that justified it.

## 8. Guardrails (§28)

- **Input**: schema-validated tool arguments; prompt-injection heuristics on user-supplied text
  (delimiter fencing, instruction-pattern detection) before it reaches system-prompt context;
  PII pattern scan with redaction option before text is sent to the model.
- **Output**: every agent's terminal output is bound to a JSON schema (Spring AI structured output);
  unparseable or schema-invalid output triggers one bounded retry, then escalation — never a raw
  string write to a domain table.
- **Operational**: per-agent token budget and per-run timeout; circuit breaker per model provider
  (Resilience4j) with automatic fallback (see §9); global and per-tenant rate limits on agent
  invocation via Redis.

## 9. Provider abstraction & fallback (§29, ADR-005/ADR-008)

```
interface ChatModelProvider { ChatResponse complete(PromptSpec spec); }
Implementations: AzureOpenAIProvider (primary), AzureAiFoundryProvider (secondary/specialized),
                 MockChatModelProvider (local dev only, clearly logged as MOCK, never selectable
                 in a prod profile)
```
Fallback chain on provider error/timeout/circuit-open: primary → secondary provider → deterministic
workflow path. Concretely: if Food Intelligence Agent is unavailable, listing creation still works
with a plain form (no AI suggestions) — DRAFT→PUBLISHED never depends on AI availability. If
Matching Agent is unavailable, the deterministic `MatchingEngine` still runs and produces ranked
candidates without the LLM rationale field. This is enforced by having the deterministic path be
the actual default implementation that the agent *decorates*, not a fallback bolted on afterward.

## 10. AI observability (§38)

`ai.agent_run` + `ai.tool_call` feed the AI Ops Dashboard: executions, success/failure, tool-call
counts, latency, token consumption, cost (via provider pricing table), escalation rate, per-agent
and per-prompt-version breakdown, RAG retrieval precision (sampled human-labeled eval set). Stored
fields are redacted decision summaries and tool I/O, never raw chain-of-thought reasoning traces.
