# Emme Durable Conversational Workflows Design

| Field | Detail |
|---|---|
| **Date** | 2026-09-04 |
| **Status** | Ready for user review |
| **Scope** | Durable customer conversations, appointment lifecycle, payment-dependent booking, and enterprise Spring AI/LangGraph4j boundaries |
| **Primary module** | `modules/assistant` |
| **Authoritative modules** | `appointments`, `payment`, `services`, `clients`, `notification`, `calendar`, `tenancy`, `identity` |

## 1. Decision summary

Emme will use a tiered hybrid architecture:

```text
Spring AI capabilities
  + deterministic application router
  + typed LangGraph4j workflow subgraphs
  + authoritative Emme application services
```

Spring AI owns model transport, structured extraction, bounded tool calling, RAG,
embeddings, moderation, and native observations. LangGraph4j owns durable state,
conditional workflow transitions, interruptions, checkpointed resumption, and
workflow-level recovery. Emme application services remain the only authority for
tenant policy, authorization, availability, appointments, payments, refunds, and
notifications.

The system will not use a free-form agent swarm for business mutations. Models may
classify, extract, retrieve, and compose. They may not decide that an appointment,
payment, refund, or cancellation is authorized or successful.

The default payment-dependent booking policy is hold-first:

```text
available slot -> appointment hold -> payment link -> verified payment
  -> revalidate hold -> confirmed appointment
```

The hold is a separate domain/application concept. The existing confirmed
appointment aggregate is not used as an unconfirmed payment placeholder.

## 2. Goals

- Support service discovery, price and policy questions, availability, booking,
  rescheduling, cancellation, status, payment links, payment callbacks, refunds,
  calendar synchronization, notification, design quotes, and staff escalation.
- Keep simple requests on a low-latency direct Spring AI path.
- Make consequential conversations durable across application restarts and delayed
  customer or provider responses.
- Control every graph node's model, tools, memory, timeout, and interruption policy.
- Guard every untrusted input, retrieved context, tool invocation, model response, and
  customer-visible output with deterministic, observable policies.
- Reuse one versioned embedding capability for RAG, semantic tool routing, semantic
  cache lookup, and other vector decisions; do not create one embedding flow per feature.
- Keep tenant identity and authorization backend-derived and fail closed.
- Use typed contracts between the router, workflows, tools, and business modules.
- Preserve an implementation seam for a future workflow engine without changing
  assistant business contracts.

## 3. Non-goals

- Allowing an LLM to directly mutate appointments or payment state.
- Replacing application services with graph nodes containing business rules.
- Making LangGraph4j the source of truth for appointments, payments, or refunds.
- Persisting payment credentials, provider secrets, image bytes, or unrestricted
  conversation payloads in graph checkpoints.
- Adding Temporal, Camunda, or another external workflow engine in this phase.
- Adding `langgraph4j-spring-ai` without a concrete adapter use case and compatibility
  evidence.

## 4. Architecture alternatives

| Alternative | Benefits | Costs and risks | Decision |
|---|---|---|---|
| Spring AI direct `ChatClient` with tools | Minimal code, low latency, native RAG and tool calling | Does not provide durable confirmation, payment waiting, or workflow recovery without a second custom state machine | Use for direct and read-only paths |
| One LangGraph4j ReAct agent | Checkpoints and flexible tool selection | Model controls too much, mutation auditing is difficult, graph becomes a large opaque state machine | Use only for bounded read-only exploration, if needed |
| LLM supervisor with peer agents | Flexible delegation and natural-language specialization | Extra model calls, cost, latency, nondeterministic routes, shared-state and authorization risks | Do not use for core business flows |
| Code router with typed workflow subgraphs | Explicit routing, durable pauses, isolated testing, incremental evolution, strong security boundary | More contracts, graph-state mapping, and versioning work | **Recommended** |
| External durable workflow engine plus LangGraph4j AI nodes | Strong long-running timers, operations UI, cross-service orchestration, language-independent workflows | New infrastructure, operational cost, duplicate state and workflow concepts | Future option behind `WorkflowRuntime`; not current scope |

The recommended option keeps the direct path fast while applying durable orchestration
only where a customer interaction crosses turns, waits for staff or a payment provider,
or performs a consequential mutation.

## 5. Runtime topology

```mermaid
flowchart TD
    Inbound[Web / WhatsApp / internal event] --> Context[Verify channel and bind AI context]
    Context --> Router[ConversationRouter]

    Router --> Direct[DirectAnswerHandler]
    Router --> Knowledge[KnowledgeWorkflow]
    Router --> Catalog[ServiceDiscoveryWorkflow]
    Router --> Appointment[AppointmentWorkflowRouter]
    Router --> Quote[QuoteWorkflow]
    Router --> Staff[StaffEscalationWorkflow]

    Direct --> SpringAI[Spring AI ChatClient]
    Knowledge --> RAG[RetrievalAugmentationAdvisor]
    Catalog --> CatalogAPI[Catalog application use cases]

    Appointment --> Book[AppointmentBookingWorkflow]
    Appointment --> Move[AppointmentRescheduleWorkflow]
    Appointment --> Cancel[AppointmentCancellationWorkflow]
    Appointment --> Status[AppointmentStatusQuery]

    Book --> Payment[AppointmentPaymentWorkflow]
    Payment --> Webhook[Verified payment webhook]
    Webhook --> Resume[WorkflowRuntime resume]

    Book --> Appointments[Appointments authority]
    Move --> Appointments
    Cancel --> Appointments
    Payment --> Payments[Payment authority]

    Appointments --> Events[Modulith events]
    Payments --> Events
    Events --> Notify[Notification and calendar adapters]
```

`ConversationRouter` is an application service, not an LLM supervisor. It returns a
typed route and confidence decision. A route that requires durability is then given to
the selected compiled LangGraph4j subgraph through `WorkflowRuntime`.

## 6. Routing policy

Routing follows a fail-closed sequence:

1. Explicit deterministic commands and pending-action responses.
2. Structured Spring AI intent and slot extraction.
3. Authorized pgvector semantic classification.
4. Bounded model fallback only for provider-unavailable semantic classification.
5. Clarification when confidence, authorization, or required slots are insufficient.

Candidate routes include:

```text
DIRECT_ANSWER
SERVICE_DISCOVERY
AVAILABILITY_LOOKUP
BOOK_APPOINTMENT
RESCHEDULE_APPOINTMENT
CANCEL_APPOINTMENT
APPOINTMENT_STATUS
PAYMENT_STATUS
QUOTE_DESIGN
MULTI_INTENT
STAFF_ESCALATION
UNSUPPORTED
```

Confirmation, rejection, clarification, payment callbacks, and staff decisions are
resolved from trusted workflow context before any model fallback.

### 6.1 Shared semantic path and fallback boundaries

RAG, semantic tool selection, semantic intent routing, and semantic response-cache lookup
use the same application embedding capability and model-version contract. The provider
selector may try configured embedding providers in order, but it must preserve the
embedding model version and dimension expected by the index. This prevents one feature
from silently writing or searching vectors that another feature cannot interpret.

The fallback meanings are deliberately separate:

| Situation | Allowed fallback | Not allowed |
|---|---|---|
| Embedding provider is unavailable | Try the next configured embedding provider; then use the compatibility embedding capability or disable the semantic shortcut according to route policy | Treating a chat completion as an embedding, fabricating a zero vector, or bypassing tenant filters |
| Embedding search returns low confidence | Run the bounded query-improvement ladder, then clarify or fail closed | Calling the answer model and presenting an ungrounded tenant-knowledge answer as factual |
| Semantic tool/cache decision abstains | Continue to the normal router or chat path; semantic cache miss proceeds to model completion | Executing a tool or returning a cache hit below its score and margin policy |
| Semantic classifier is unavailable | Use the explicitly configured structured/model route fallback only for non-authorizing classification | Allowing a model fallback to authorize a mutation |

An LLM fallback therefore means a bounded model call for classification, query
transformation, or response composition. It does not mean that a chat model replaces an
embedding model or overrides a retrieval-quality gate. The current compatibility path
uses `AiModelProvider.embed` only when no application `EmbeddingModelPort` is present;
that is an embedding-capability fallback, not a chat fallback.

### 6.2 Embedding-first semantic fast path

Embedding-first is the recommended default for known semantic decisions because it avoids
an unnecessary chat-model call for tool selection, cache lookup, intent routing, and the
first RAG retrieval. The vector result is a candidate signal, not an authority:

```text
input guard
  → normalize once and reuse the turn embedding when compatible
  → filter by backend authorization and route policy
  → embed and retrieve candidates
  → apply top-score, margin, freshness, and eligibility gates
  → take a deterministic shortcut only when the gate accepts
  → otherwise continue to the bounded model or clarification path
```

| Capability | Accepted embedding decision | Abstention behavior |
|---|---|---|
| Tool routing | Select an authorized, read-only tool whose score and margin pass policy | Continue to normal conversation handling; mutation tools require the durable workflow and confirmation regardless of score |
| Semantic cache | Return only an eligible, fresh, version-compatible, principal-scoped response whose high threshold and margin pass policy | Miss the cache and call the normal answer model; cache failure never makes chat unavailable |
| Intent routing | Select a route only when its score and margin pass the route policy | Use structured/model classification only where allowed, otherwise return `GENERAL` or ask for clarification |
| RAG | Accept context only after the retrieval-quality gate passes | Run the bounded query-improvement ladder, then clarify or produce a grounded no-answer |

The embedding is usually cheaper and more predictable than a chat completion, but it is
still an AI provider call with latency, capacity, and failure modes. The implementation
must therefore apply admission limits, provider failover, metrics, and request-scoped
deduplication. A compatible embedding computed for one normalized turn may be reused by
tool and cache checks; a rewritten or expanded RAG query must receive a new embedding.

This fast path must not become a hidden semantic agent. It cannot invent tool arguments,
grant authorization, approve a payment, or return a cache entry without checking current
policy and identity. A model call is the controlled fallback for ambiguity or query
transformation, not the default selector for every request.

### 6.3 Current repository behavior and migration gap

The repository already follows this pattern in several paths, with one important
qualification:

| Path | Current behavior | Target change |
|---|---|---|
| Intent | `SemanticIntentRouter` embeds the message and `SemanticIntentClassifier` applies top-one similarity and margin; `DetectIntentService` returns `GENERAL` or `unavailable` when the semantic path abstains or is transiently unavailable | Add the explicit structured/model fallback policy at the workflow boundary without allowing it to authorize mutations |
| Tools | `SemanticProactiveToolRouter` embeds first, filters to backend-authorized keys, and invokes only an accepted semantic tool decision | Reuse the turn embedding and make low-confidence behavior an explicit route to normal handling; keep mutation tools outside this shortcut |
| Semantic cache | `SemanticChatCache` embeds before hot/durable lookup and `ChatService` calls normal chat completion after a miss or semantic failure | Preserve the existing cache safety, identity, freshness, and version gates while making the shared embedding and fallback contract explicit |
| RAG | `DocumentKnowledgeRetrievalAdapter` uses `EmbeddingModelPort` when available and the legacy provider's `embed` capability otherwise; generation then uses the answer model. The optional Spring AI path wraps this with `RetrievalAugmentationAdvisor` | Preserve search scores, add `RetrievalQualityGate`, and run bounded query improvement before grounded generation |
| Embedding failover | `EmbeddingModelSelector` tries configured providers in order only for provider-unavailable failures | Keep this provider failover separate from low-relevance query improvement and from chat-model fallback |

Therefore, the answer to “embeddings first, then LLM” is yes for the semantic shortcuts,
but the LLM is currently the next step for cache-miss response generation and a future
explicit fallback for route classification. It is not currently a universal fallback for
every failed embedding operation. The migration must preserve that distinction.

## 7. Spring AI responsibilities

### 7.1 Model profiles

The logical model roles are:

| Role | Required | Responsibility |
|---|---:|---|
| `routeModel` | Yes | Intent, route, and multi-intent classification at low temperature |
| `answerModel` | Yes | Grounded response composition, proposal wording, and complex reasoning |
| `embeddingModel` | Yes | Semantic route, RAG, semantic cache, and optional tool index |
| `visionModel` | Optional | Nail-design image feature extraction |
| `transcriptionModel` | Optional | Voice-note transcription |
| `moderationModel` | Optional | Input/output safety classification |

The baseline deployment uses two chat profiles and one embedding model. The optional
vision and transcription roles may reuse a provider's multimodal services, but they do
not create additional workflow agents. A query transformer reuses `routeModel` or
`answerModel`; it does not require a third general-purpose chat model.

Spring AI's `ChatClient` supports multiple model clients and recommends preserving the
configured builder/customizer path when composing clients. The implementation will use
named model profiles behind `ChatModelSelector`, not provider-specific branching in
assistant use cases.

### 7.2 Tool control

Spring AI owns the bounded tool-call loop. Emme owns eligibility and authorization.

Tools are classified as:

```text
READ_ONLY       safe lookup; may be model-visible
CONFIRMATION    proposes a consequential operation; never mutates
MUTATION        changes business state; invoked by an authorized workflow node
STAFF_ONLY      requires staff role and durable approval
EXTERNAL        remote/MCP capability; explicit allow-list required
```

Each model-facing node receives a `NodeProfile` containing:

```text
nodeId
modelRole
allowedToolKeys
memoryPolicy
maxToolCalls
timeout
mayInterrupt
requiredApproval
```

The node receives a filtered `AuthorizedAiToolGateway`, never the complete registry.
The gateway applies tenant, principal, role, operation, and confirmation policy before
executing a tool. Mutation nodes call application use-case ports directly or use the
existing authorized mutation gateway after the graph has recorded confirmation.

`ToolSearchToolCallingAdvisor` is allowed only for read-only tool discovery when the
tool catalog becomes large. Its session key must be server-derived from tenant and
conversation context. Resolution fallback remains disabled for mutation tools.

Tool-call limits, timeout behavior, exception handling, and partial tool results are
recorded in the workflow trace. Spring AI provides configurable per-tool and total
tool-call limits that will be set explicitly rather than relying on unreviewed defaults.

### 7.3 RAG composition

Spring AI's modular RAG building blocks are used instead of custom generic names:

| Need | Component |
|---|---|
| Follow-up query compression | `CompressionQueryTransformer` |
| Query rewrite | `RewriteQueryTransformer` |
| Language normalization | `TranslationQueryTransformer` when required |
| Query expansion | `MultiQueryExpander` for complex knowledge requests |
| Retrieval | `DocumentRetriever` / tenant-scoped vector retriever |
| Aggregation | `DocumentJoiner` |
| Reranking and deduplication | `DocumentPostProcessor` |
| Context injection | `ContextualQueryAugmenter` |
| Complete flow | `RetrievalAugmentationAdvisor` |

`KnowledgeRoute` selects whether the request needs policy, FAQ, design, or other
tenant knowledge. It is application policy and may be implemented as a conditional
workflow edge; Spring AI does not become the owner of business routing.

Prices, appointment availability, payment state, refund eligibility, and permissions
are never sourced from RAG.

The knowledge path uses a bounded quality-and-improvement loop:

```text
validate input and trusted context
  → embed original query
  → tenant-filtered hybrid/vector retrieval
  → evaluate score, margin, support count, freshness, and route policy
  → if sufficient: rerank, deduplicate, aggregate, and answer with context
  → if insufficient: compress/rewrite/translate/expand the query within a fixed budget
  → re-embed and retrieve
  → if still insufficient: ask a clarifying question or return a grounded no-answer
```

`RetrievalQualityGate` is deterministic and route-specific. It evaluates the top result
and score margin, the number of independently supporting chunks, lexical/semantic
agreement when hybrid search is used, document freshness, and whether the source type is
allowed for the route. Thresholds are calibrated from representative evaluation data;
there is no universal similarity threshold that is safe for every tenant, language, or
knowledge collection.

`QueryImprovementPolicy` limits the number of attempts, transformed-query length, query
variants, token budget, and total latency. It records which transformation improved or
failed retrieval. It may use a low-temperature `routeModel` or `answerModel` for
rewriting, but it cannot alter the tenant filter, source policy, answer policy, or
business route. Live conversations produce evaluation candidates and telemetry; they do
not automatically rewrite production prompts, thresholds, or tenant knowledge without a
controlled promotion process.

The retrieval contract must preserve the authoritative search score and source metadata
from the Documents module into `RetrievedDocument`. The current adapter returns a score of
`0.0` after `DocumentChunkDetails` loses the search-hit score, so score-aware gating is a
planned implementation task rather than an existing guarantee.

### 7.4 Guardrails

Guardrails are layered and fail closed. A model cannot disable or weaken a guardrail by
returning a different instruction, tool argument, or output format. Each decision is
typed, bounded, and traced with redacted evidence:

| Boundary | Required controls | Safe outcomes |
|---|---|---|
| Input admission | Authentication, tenant binding, content type, size and token limits, Unicode normalization, attachment limits, rate/cost budget, and idempotency validation | `ALLOW`, `REJECT`, `CLARIFY` |
| Input safety | Prompt-injection and instruction-boundary detection, abuse/safety classification, PII/payment-secret handling, and channel-specific policy | `ALLOW`, `REDACT`, `BLOCK`, `ESCALATE` |
| Context assembly | Tenant/principal filter, source allow-list, document freshness, injection scanning on retrieved text, context-size budget, and provenance preservation | `ALLOW`, `DROP_CONTEXT`, `CLARIFY`, `NO_ANSWER` |
| Tool invocation | Typed schema validation, tool allow-list, role/tenant authorization, confirmation, idempotency, timeout, call count, and response-size limits | `ALLOW`, `DENY`, `WAIT_FOR_CONFIRMATION`, `ESCALATE` |
| Model output | Structured schema validation, secret/PII leakage checks, content-safety classification, citation/provenance checks for RAG, and business-claim validation | `ALLOW`, `REDACT`, `REGENERATE`, `BLOCK`, `ESCALATE` |
| Delivery | Final response policy, channel encoding, length limits, safe streaming, and durable audit event | `DELIVER`, `TRUNCATE`, `BLOCK`, `ESCALATE` |

The named implementation boundaries are `InputGuard`, `ContextGuard`, `ToolGuard`,
`OutputGuard`, and `GroundingGuard`. They are protocol-based and injected into direct
Spring AI calls and LangGraph nodes. Spring AI advisors provide the request/response
interception mechanism; graph edges own durable pause, retry, escalation, and
regeneration decisions. For streaming responses, output is buffered until the applicable
checks pass, or emitted only through a policy that can revoke/stop delivery safely.

Safety moderation is an optional provider capability, never the only guardrail. If a
moderation provider is unavailable, the route-specific policy chooses a safe deterministic
fallback; it must not silently allow an unsafe response. Mutation workflows additionally
require typed application validation after any model output and before a state change.

## 8. Node, assistant, tool, and memory control

### 8.1 State layers

```text
TurnContext
  short-lived current input, deadline, correlation, and extracted candidates

ConversationMemory
  bounded prompt history and customer-visible conversation context

WorkflowState
  durable, versioned, minimal state required to resume a business workflow

TenantKnowledge
  tenant-scoped indexed documents and derived retrieval context

NodeScratch
  private transient computation; not persisted unless explicitly promoted
```

Spring AI `ChatMemory` is a bounded prompt-context mechanism, not the authoritative
conversation history or workflow state. Complete conversation events remain in Emme's
PostgreSQL history. LangGraph4j checkpoints persist only the versioned workflow state.

### 8.2 Memory policies

Each `NodeProfile` selects one explicit memory policy:

```text
NONE                  no prior memory
TURN                  current user turn only
CONVERSATION_WINDOW   bounded relevant conversation window
WORKFLOW              selected durable workflow fields
TENANT_KNOWLEDGE      authorized retrieval context only
```

Nodes cannot request arbitrary memory at runtime. A node receives a projection containing
only the fields required by its contract. Payment credentials, provider signatures,
secrets, raw image bytes, and unrestricted PII are never model-visible or checkpointed.

### 8.3 Typed node boundary

The production abstraction is conceptually:

```text
NodeProfile
NodeContext<VisibleState>
NodeResult<StatePatch>
WorkflowNode<VisibleState, StatePatch>
```

The implementation may map these types to LangGraph4j `AgentState` and serialized state
maps at the adapter boundary, but application code does not pass unbounded maps between
business nodes. Every state patch is validated, namespaced, bounded, and JSON-safe.

## 9. Durable appointment workflows

### 9.1 Booking

```text
receive request
  → extract service, customer, date, preferred time, artist
  → validate trusted customer and service references
  → fan out availability, booking policy, and payment policy
  → join deterministic results
  → propose exact slot and price
  → interrupt for explicit customer confirmation
  → create AppointmentHold when payment is required
  → create normalized PaymentLink
  → interrupt with WAITING_FOR_PAYMENT
  → resume from verified provider webhook
  → verify payment and hold ownership
  → revalidate slot and policy
  → create confirmed appointment through appointments use case
  → publish notification and calendar events
```

Without prepayment, the payment branch is skipped, but confirmation remains mandatory.

### 9.2 Rescheduling

```text
load owned appointment
  → validate reschedule policy
  → extract and validate target slot
  → fan out availability, policy, and price-difference calculation
  → propose new schedule and financial effect
  → interrupt for confirmation
  → execute authorized reschedule
  → initiate additional payment or refund when policy requires
  → notify and update calendar
```

### 9.3 Cancellation

```text
load owned appointment
  → evaluate cancellation deadline and refund policy
  → inspect linked payment state
  → propose cancellation and refund/forfeiture result
  → interrupt for confirmation
  → execute authorized cancellation
  → initiate refund if eligible
  → await provider result when asynchronous
  → notify and unsync calendar
```

The appointment and payment modules remain authoritative. The workflow is a process
manager and recovery boundary, not a replacement aggregate.

## 10. Payment hold and saga design

The current appointment model creates confirmed appointments and the current payment
result needs a canonical checkout-link and business-correlation contract. The design
adds:

```text
AppointmentHold
PaymentLink
PaymentBusinessReference
PaymentPolicy
PaymentWorkflowEvent
```

The normalized payment result contains:

```text
paymentId
businessReference
amount
currency
checkoutUrl
status
expiresAt
```

Payment operations are idempotent by tenant, business reference, provider, and operation
key. A failed post-payment appointment confirmation triggers a deterministic recovery
path: revalidate the hold, offer an alternative slot, or initiate a governed refund.
The model does not choose among those outcomes; policy and application services do.

## 11. LangGraph4j enterprise patterns

### Subgraphs

Use compiled subgraphs with explicit input/output mappings for booking, rescheduling,
cancellation, payment, quote, and staff review. Shared-state subgraphs are restricted
to tightly coupled internal nodes because they increase key collision and migration
risk.

### Interruptions

Use dynamic interruptions for customer confirmation, staff approval, and payment waits.
Static breakpoints are reserved for tests and operational inspection. Every interruption
requires a persistent checkpoint and carries a bounded, customer-safe pending-action
projection.

### Checkpoints and threads

Every durable workflow uses a tenant-aware PostgreSQL checkpoint boundary. The thread ID
is derived from the workflow ID and optional bounded subgraph namespace. State contains
workflow version, graph version, policy version, and correlation identifiers so old
checkpoints can be migrated or rejected safely.

### Parallel fan-out/fan-in

Parallel branches are used only for independent reads:

```text
availability + booking policy + payment policy → join
knowledge retrievers → document joiner → post-process
appointment + refund policy + payment history → join
```

No payment capture, booking, cancellation, refund, or hold creation runs in parallel.
The executor is bounded, cancellation-aware, and owned by the application composition
root.

### Retry, timeout, and cancellation

Retries are classified by operation:

```text
retry: model transport, vector read, availability read, provider status read
do not retry blindly: book, cancel, reschedule, capture, refund, create hold
```

Mutation retry requires idempotency and a known previous outcome. Timeouts produce an
explicit durable failure or waiting state. Graph cancellation is propagated to active
read-only branches, but external payment effects require compensation rather than
thread interruption.

### Event-driven resume

Verified payment callbacks and staff decisions resume the workflow by workflow ID and
business correlation. The resume adapter reconstructs the trusted AI context and checks
tenant, principal, role, workflow version, pending action, and idempotency before calling
LangGraph4j.

### Observability and replay

Each workflow records route, node, model role, tool key, wait state, attempt, duration,
outcome, and redacted error category. Checkpoint history is available for support and
incident diagnosis, but replay never re-executes a non-idempotent mutation without an
explicit idempotency check.

The repository is currently pinned to LangGraph4j `1.8.25`. Any 1.9 upgrade, including
new `GraphInput`, checkpoint tag, subgraph saver, or interruption/error APIs, is a
separate compatibility task. The implementation must not depend on undocumented
version-specific behavior.

## 12. Module and class changes

| Area | Planned responsibility and clear names |
|---|---|
| `ai-contracts` | `ConversationRoute`, `WorkflowResumeEvent`, `PaymentLink`, `AppointmentHold`, and framework-neutral result contracts |
| `assistant` router | `ConversationRouter`, `RouteDecision`, `KnowledgeRoute` |
| `assistant` runtime | `WorkflowRuntime`, `WorkflowCheckpointStore`, `WorkflowResumeService` |
| `assistant` workflows | `AppointmentBookingWorkflow`, `AppointmentRescheduleWorkflow`, `AppointmentCancellationWorkflow`, `AppointmentPaymentWorkflow` |
| `assistant` node policy | `NodeProfile`, `NodeMemoryPolicy`, `NodeToolPolicy`, `NodeContext` |
| `assistant` tools | `AuthorizedAiToolGateway`, read-only tool adapters, mutation workflow adapters |
| `assistant` semantic | `EmbeddingService`, `EmbeddingModelSelector`, `RetrievalQualityGate`, `QueryImprovementPolicy` |
| `assistant` guardrails | `InputGuard`, `ContextGuard`, `ToolGuard`, `OutputGuard`, `GroundingGuard` |
| `assistant` RAG | `KnowledgeRetriever`, `KnowledgeAnswerService`, Spring AI RAG adapter |
| `appointments` | `AppointmentHoldService`, hold repository/expiry policy, authorized lifecycle contracts |
| `payment` | `PaymentLinkService`, business-reference correlation, webhook resume event, refund/retry policy |
| `notification` | Payment-link, pending-action, booking, cancellation, and refund notifications |
| `calendar` | Confirmed-appointment sync and cancellation/reschedule reconciliation |
| `database` | Holds, workflow correlation, payment references, expiry, idempotency, and indexes |

Existing classes are renamed or removed only after caller, configuration, focused test,
and integration evidence confirms the replacement. Business module names remain the
source of truth where an existing clear name already exists, such as
`BookAppointmentService`, `CancelAuthorizedAppointmentService`, and
`RescheduleAuthorizedAppointmentService`.

## 13. Security and data rules

- Resolve tenant, principal, roles, customer identity, and database context from the
  backend; never trust model-supplied identity fields.
- Filter every tool, vector query, semantic cache lookup, checkpoint, and workflow resume
  through the current `AiExecutionContext`.
- Apply input, context, tool, output, grounding, and delivery guardrails before the next
  model or business operation; guardrail failure must not be converted into provider
  failover.
- Do not expose mutation tools to the normal Spring AI tool loop.
- Do not store card details, provider access tokens, webhook secrets, or payment
  credentials in prompts, memory, traces, or checkpoints.
- Treat payment links as short-lived, tenant-correlated, and customer-visible only.
- Use deterministic application policy for appointment ownership, cancellation windows,
  refunds, and price differences.
- Redact PII, payment metadata, image bytes, vectors, and raw tool arguments from traces
  by default.
- Do not treat retrieved text as trusted instructions; retrieved content is data and is
  isolated from system/developer policy before it reaches a model.
- Do not deliver an answer as grounded when `RetrievalQualityGate` has not accepted the
  context; use clarification, a bounded safe response, or staff escalation.

## 14. Testing strategy

### Unit tests

- Router precedence, confidence, unsupported and ambiguous intents.
- Input/output/context/tool/grounding guardrail decisions, fail-closed behavior, and
  prompt-injection or secret-leakage rejection.
- Node profile tool allow-lists, memory projections, timeouts, and call limits.
- State serialization, version checks, and bounded state patches.
- Booking, reschedule, cancellation, and payment state transitions.
- Hold expiry, ownership, collision, and idempotency.
- Payment-link normalization and webhook correlation.
- Retry classification and compensation decisions.
- Shared embedding model-version/dimension enforcement across RAG, tools, intent
  routing, and semantic cache.
- Retrieval threshold, score-margin, support-count, freshness, and hybrid-agreement
  decisions, including the bounded query-improvement ladder and exhausted-budget path.

### Integration tests

- PostgreSQL checkpoint pause/resume after process restart.
- Tenant-aware checkpoint and state access.
- Appointment hold collision with concurrent requests.
- Payment webhook verification and duplicate delivery.
- Payment success followed by appointment confirmation.
- Payment success followed by stale-hold recovery/refund.
- Notification and calendar event publication after committed mutations.
- Spring AI structured extraction, tool limits, dynamic tenant RAG filters, and
  observability customizers.
- End-to-end retrieval score preservation from document search through Spring AI RAG,
  threshold filtering, reranking, and grounded answer generation.
- Embedding-provider outage, compatibility embedding fallback, low-confidence retrieval,
  query rewrite retry, and no-ungrounded-answer behavior.

### End-to-end tests

- Web and WhatsApp booking without payment.
- Web and WhatsApp booking with payment link and delayed callback.
- Rescheduling with additional payment or refund.
- Cancellation with eligible and ineligible refund.
- Restart while waiting for confirmation, staff approval, and payment.
- Duplicate webhook and duplicate customer message.
- Wrong-tenant and unauthorized-resume rejection.
- Provider outage and recovery.

Focused module checks run during implementation. Phase-level integration and expensive
E2E suites run at the end of each migration phase. The final enterprise gate includes
formatting, compilation, unit tests, architecture tests, migrations, Testcontainers,
startup, webhooks, and deployed E2E checks.

## 15. Implementation phases

1. Add framework-neutral route, workflow, hold, payment-link, and resume contracts.
2. Add `ConversationRouter` and direct-path bypass for simple requests.
3. Add `NodeProfile` tool and memory policy boundaries.
4. Consolidate Spring AI multi-client composition, shared embeddings, tool governance,
   input/output/context/grounding guards, observations, moderation, and current modular
   RAG.
5. Add retrieval score preservation, `RetrievalQualityGate`, and the bounded
   `QueryImprovementPolicy` for RAG, semantic tools, intent routing, and cache policies.
6. Implement typed booking workflow and explicit confirmation.
7. Implement appointment holds, expiry, collision protection, and release.
8. Implement payment-link creation, provider correlation, webhook resume, and recovery.
9. Implement reschedule and cancellation workflows with refund policy.
10. Add multi-intent decomposition, fan-out/fan-in read branches, and staff escalation.
11. Integrate notifications, calendar events, operational traces, and replay safeguards.
12. Add checkpoint/state versioning and compatibility checks.
13. Remove duplicate agents, tools, wrappers, and obsolete abstractions.
14. Run phase-level integration validation and the final enterprise gate.

## 16. Acceptance criteria

- Simple read-only conversations bypass durable graph execution while retaining complete
  conversation history.
- Every durable appointment or payment flow can pause and resume after process restart.
- Every model-facing graph node has an explicit model, tools, memory, timeout, and
  interruption policy; deterministic nodes explicitly declare `modelRole=NONE`.
- The model cannot directly create, reschedule, cancel, capture, or refund an appointment
  or payment.
- Required-payment booking holds the selected slot until payment or hold expiry.
- Verified payment callbacks resume only the matching tenant/workflow and are idempotent.
- RAG is tenant-scoped and cannot answer authoritative appointment, pricing, or payment
  questions from unstructured knowledge alone.
- Every model request and response passes the applicable typed input, context, tool,
  output, grounding, and delivery guardrails; unsafe or invalid results fail closed.
- RAG evaluates retrieval sufficiency using preserved scores and route-specific thresholds
  before answer generation, and performs only a bounded, observable query-improvement
  loop when the first retrieval is insufficient.
- RAG, semantic tool routing, semantic intent routing, and semantic cache use the same
  versioned embedding contract and explicit provider/compatibility fallback policy.
- Parallel execution is limited to independent reads and never to business mutations.
- State, traces, tool arguments, and model prompts exclude secrets and sensitive payment
  data.
- LangGraph4j can be replaced behind `WorkflowRuntime` without changing appointment or
  payment application contracts.
- Focused, phase-level, and final enterprise verification gates are defined in the
  execution plan before implementation begins.

## 17. Sources

## 18. Implementation results (2026-09-05)

The durable workflow implementation has completed the canonical embedding, retrieval-quality,
typed appointment hold/payment callback, tenant-routed checkpoint, and first appointment
lifecycle boundary slices. Reschedule and cancellation workflows require confirmation and
delegate all mutations to authorized appointment use cases; payment resume claims and lifecycle
checkpoint writes are atomic at the tenant-routed workflow-run boundary. Semantic telemetry is
redacted and its match-key metadata is bounded before durable persistence.

The remaining durable work is explicit: complete cancellation-window/refund and staff-review
composition, notification/calendar replay evidence, the full operational evaluation promotion
gate, compatibility cleanup, and phase-level integration validation. PostgreSQL/Testcontainers
checkpoint, concurrency, Redis, Kafka, and provider-runtime evidence remains queued until Docker
is available.

- [Spring AI ChatClient and multiple models](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Spring AI tool calling and tool limits](https://docs.spring.io/spring-ai/reference/api/tools.html)
- [Spring AI tool search](https://docs.spring.io/spring-ai/reference/api/tools/tool-search-tool.html)
- [Spring AI advisors and content-safety advisor](https://docs.spring.io/spring-ai/reference/api/advisors.html)
- [Spring AI moderation](https://docs.spring.io/spring-ai/reference/api/moderation.html)
- [Spring AI modular RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- [Spring AI vector similarity thresholds](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- [Spring AI evaluation](https://docs.spring.io/spring-ai/reference/api/testing.html)
- [Spring AI chat memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)
- [Spring AI observability](https://docs.spring.io/spring-ai/reference/observability/index.html)
- [LangGraph4j state, threads, and checkpoints](https://github.com/langgraph4j/langgraph4j/blob/main/langgraph4j-core/src/site/markdown/concepts/low_level.md)
- [LangGraph4j subgraphs](https://langgraph4j.github.io/langgraph4j/1.9/core/subgraph/)
- [LangGraph4j interruptions](https://langgraph4j.github.io/langgraph4j/1.9/core/core-library/)
- [LangGraph4j parallel branches](https://langgraph4j.github.io/langgraph4j/main/core/parallel-branch/)
- [LangGraph4j Spring AI integration](https://github.com/langgraph4j/langgraph4j/blob/main/spring-ai/README.md)
