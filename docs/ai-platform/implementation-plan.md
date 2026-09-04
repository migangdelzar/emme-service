# Implementation Plan: Emme AI Platform

| Field | Value |
|---|---|
| Status | Core phases and safety boundaries implemented; real generic conversation capability wiring and runtime certification remain |
| Technical specification | [TSPEC](technical-specification.md) |
| Requirements | [Requirements](requirements.md) |
| Verification | [Evaluation specification](evaluation-specification.md) |

## 1. Execution rules

- Implement one phase at a time.
- Write the failing test before production code for each behavior.
- Preserve unrelated worktree changes.
- Keep each phase reversible and feature-gated where appropriate.
- Run formatting, compilation, static analysis, unit tests, integration tests,
  and applicable end-to-end tests after each phase.

## 2. Phases

| Phase | Scope | Primary tests | Status |
|---|---|---|---|
| 0 | Java 25 repository/runtime baseline | Toolchain, CI/config validation | Complete |
| 1 | Execution context, ScopedValue, executors | Context and concurrency unit tests | Complete |
| 2 | StructuredTaskScope and Joiners | Parallel runner and cancellation tests | Complete |
| 3 | `ai-contracts`, `ai-platform`, and Spring AI providers | Provider/embedding/chat contract tests | In progress — contracts, provider adapters, embedding, and ordered chat boundaries complete |
| 4 | pgvector intent/tool/cache indexes | Tenant-filtered vector integration tests | In progress — semantic intent/tool routing and safe chat cache complete; Redis hot projection and tool-search index are opt-in; real pgvector JDBC/runtime coverage now passes |
| 5 | LangGraph4j graph and checkpoints | Workflow persistence/resume tests | Complete |
| 6 | Spring AI advisors and controlled tools | Advisor/tool policy integration tests | In progress — tenant/prompt advisors, read-only controlled gateway, opt-in Redis progressive tool search, durable mutation idempotency with stale-claim recovery, and concrete appointment mutation handlers complete; runtime integration remains |
| 7 | Design extraction, deterministic quote, HITL | Quote, optimistic-lock, endpoint, and resume tests | In progress — durable quote workflow, secured staff review endpoint, and LangGraph resume adapter complete |
| 8 | Online enrichment and evaluation | Candidate/promotion safety tests | In progress — durable candidate capture, redaction boundary, and offline promotion gates complete; evaluator/index promotion pending |
| 9 | Channels, operations, and hardening | E2E, failure, and observability tests | In progress — Redis status/locks/events, workflow metrics, durable model/tool traces, authoritative workflow responses, and fail-fast activation boundary complete |

## 3. Phase 0 — Java 25

- Verify Gradle runtime selection.
- Verify `mise`, CI, Docker, Kubernetes, Buildpacks, and Testcontainers.
- Add a fail-fast runtime validator.
- Verify preview compilation and execution.
- Verify JVM and native-image lanes separately.

### Phase 0 result

The repository now has a dependency-free `scripts/verify-java25-runtime.mjs`
preflight with unit coverage, explicit `mise run toolchain:jvm` and
`mise run toolchain:native` lanes, and a Java 25 preflight in backend CI. The
JVM lane was verified locally with Java 25.0.2 and Gradle 9.4.1. The native
lane remains an explicit build command because it requires a GraalVM Native
Image toolchain and is not part of the default JVM checks.

## 4. Phase 1–2 — Concurrency

- Add immutable `AiExecutionContext`.
- Add ScopedValue binding at HTTP and worker boundaries.
- Add context bridge for existing ThreadLocal/MDC compatibility.
- Add named executor beans and tenant/provider limits.
- Add `ParallelTaskRunner` and StructuredTaskScope implementation.
- Add Joiner policies for required, optional, and first-success operations.
- Test cancellation, deadline, exception propagation, and context integrity.

### Concurrency progress

The kernel now contains the stable `ParallelTaskRunner` port, deadline and
outcome types, and a Java 25 `StructuredTaskScope` adapter using
`allSuccessfulOrThrow`, `awaitAll`, and `anySuccessfulResultOrThrow`. Its tests
cover ordered required work, optional failures, first-success cancellation,
deadline cancellation, fatal-error propagation, and ScopedValue inheritance.
The runner also installs the bridge for structured subtasks when an AI context
is bound, so existing tenant-routed application services do not lose their
ThreadLocal/MDC compatibility state. Phase 1 remains in progress until tenant
and provider backpressure limits are implemented.

## 5. Phase 3–4 — AI contracts, provider, and vector foundation

- Upgrade and pin Spring AI.
- Resolve compatible LangGraph4j versions before use.
- Keep framework-neutral contracts in `libraries:ai-contracts`.
- Keep reusable model providers and capability adapters in `modules:ai-platform`;
  assistant owns Emme-specific composition and use cases.
- Refactor existing ModelProvider behind focused ports.
- Add model/provider routing.
- Add intent, tool, and cache schemas and indexes.
- Reuse existing documents/catalog pgvector patterns.
- Add embedding model/version validation.

### Phase 3 progress

The assistant now has a framework-free semantic reference matcher with
immutable embedding vectors, exact model-version and dimension checks, cosine
ranking, top-1/top-2 margin gating, and backend-authorized candidate filtering.
It is reused by typed intent, tool-selection, and semantic-cache application
services. The services abstain rather than invoke an LLM when their policies do
not accept a candidate.

The shared Gradle platform now pins Spring AI `2.0.1` and LangGraph4j `1.8.25`.
Spring AI `2.0.x` is compatible with the repository’s Spring Boot `4.1.x`
baseline. The stable LangGraph4j `1.8.x` line is selected instead of the
available `1.9.0-beta3` pre-release. Concrete framework integrations are still
being added incrementally. The first Spring AI integration is a provider-neutral
embedding port plus an infrastructure adapter. It converts Spring AI’s
`EmbeddingModel` output into the application vector type and rejects configured
dimension drift before semantic search or persistence. It is not globally
auto-wired by a starter: conditional configuration constructs the local Ollama
model from the application’s embedding settings, registers explicitly named
providers in order, and exposes the application-level failover chain. This
integration is disabled by default. Tenant-specific cloud-escalation policy is
the next provider-policy slice.

The provider boundary now also includes an ordered application-level embedding
failover chain. It only falls back for an explicit provider-unavailable error;
dimension, model-version, and other application failures remain terminal. The
ordered providers are supplied by the composition root, so tenant-specific
cloud-escalation policy can be added without changing the semantic matcher.

### Phase 4 progress

The studio tenant-schema changelog now provisions tenant-scoped intent and tool
reference tables plus a principal-scoped expiring semantic cache. The active
schema is 768-dimensional after migration `021-ai-embeddinggemma-dimension.sql`
converts the initial 1024-dimensional definition and fails closed when vectors
must be reindexed. The tables use the existing explicit RLS convention, HNSW
cosine indexes, and supporting tenant/active/expiry indexes. A
database-module migration contract test protects the changelog inclusion and
isolation invariants. JDBC retrieval and cache adapters now bind tenant and
principal from the authenticated AI execution context, apply model/prompt/
context filters, and constrain semantic tool search to backend-authorized
keys. Cache writes now use a tenant/principal-scoped idempotency key, and cache
hits are atomically accounted for in PostgreSQL. The resolver abstains when the
durable hit update cannot confirm that the entry is still active and unexpired.
Real pgvector coverage now runs as an explicit Testcontainers integration gate
using the pinned `pgvector/pgvector:0.8.6-pg16-trixie` image. The test uses the
production JDBC adapters against the live extension and verifies both
tenant-filtered reference search and durable semantic-cache write/read/hit
accounting. It is intentionally a narrow JDBC integration harness rather than
the full application context, because the latter includes the existing tenant
routing/JPA bootstrap and is unrelated to vector operator verification.

### Quote vertical-slice progress

The assistant now contains a framework-independent `NailDesignFeatures`
extraction contract with closed enums, bounded confidence, and ambiguity
reasons. `DeterministicQuoteCalculator` evaluates only immutable, versioned
tenant template lines and emits a candidate range plus review reasons; model
output cannot supply a price. `ProcessDesignQuoteService` binds these rules to
the backend `AiExecutionContext`, uses the durable idempotency lookup, persists
extraction/draft/review artifacts through ports, and pauses at
`WAITING_FOR_STAFF` when the calculation is not safe to send. A Spring AI
structured-output adapter now maps the model response to this contract with
schema validation and provider-native structured output. The LangGraph4j graph
now models the quote route as explicit conditional nodes, interrupts after
`wait_for_staff`, and resumes through the approval gate after a staff state
update. `JdbcLangGraphCheckpointSaver` persists tenant/workflow-scoped
checkpoints in PostgreSQL and `TenantAwareCheckpointSaver` rejects missing or
mismatched workflow context. JDBC quote workflow, extraction, draft, and
review adapters plus the optimistic-lock review application service are now
implemented. The secured inbound staff endpoint derives identity from the
validated JWT and tenant from backend context, then the service resolves
workflow correlation before saving and resuming the graph. The resume adapter
is wired only when the quote and LangGraph feature flags are enabled.

Redis status, locking, and live-event adapters are now available behind
application ports and are disabled by default. The implementation uses Redis
only for temporary operational state; PostgreSQL remains authoritative for all
business and workflow records. Micrometer workflow counters and duration
histograms are emitted without tenant, principal, conversation, or workflow ID
labels to avoid high-cardinality metric series.

Semantic intent routing now runs before the existing model classifier when the
feature flag and embedding provider are enabled. The semantic chat cache is
restricted to context-free informational questions and uses durable JSONB
payloads, principal/tenant filtering, expiry, and hit confirmation. Spring AI
chat integration provides an ordered named-client chain with explicit provider
unavailability fallback to the existing provider boundary. Every configured
named client is composed with tenant-security and prompt-version advisors at
request execution time.

The optional Redis Stack projection now uses the official Spring AI
`RedisVectorStore` for two separate indexes: a principal-scoped response-cache
projection and a backend-authorized progressive tool-search index. PostgreSQL
remains canonical. A Redis cache hit is re-confirmed through the durable cache
resolver before decoding the response. The tool-search advisor is added only
when the tool callback provider is present and is the sole Spring AI tool
advisor; its session key includes tenant, principal, conversation, and role
scope. The local Kubernetes manifest uses a Redis 8 ARM64-compatible image, but
the application flags remain disabled by default. Cache projection keys now also
receive the durable expiry as a Redis TTL, while the metadata expiry filter and
PostgreSQL confirmation remain the correctness boundaries. Redis tag metadata
is encoded at the projection boundary and queried through Spring AI's typed
filter-expression builder, so model versions and context fingerprints may
contain reserved characters safely. The projection configures the durable id
and response payload as returned metadata, and a Testcontainers gate verifies
the pinned Redis 8 image, vector read/write, TTL, and tenant isolation.

The controlled tool boundary now snapshots backend-authorized eligible tools,
uses pgvector semantic matching before execution, and invokes only typed tool
handlers. The platform registers `getSalonServices` as a read-only example;
the handler delegates to the Services application use case and derives the
tenant only from `AiExecutionContext`. Mutation tools remain confirmation and
approval gated.

Mutation execution now also passes through `AiToolIdempotencyStore`. For a
mutation, the gateway derives an operation key from the backend tool key,
authenticated principal, and context idempotency key, atomically claims it in PostgreSQL,
and stores the authoritative `AiToolResult` before returning. Completed
replays return the stored result without invoking the handler again; concurrent
claims are rejected; failed handlers release their claim for retry. Claim
cleanup failures are suppressed onto the original handler error, and a
completion-write failure remains in progress so the command fails closed.
Claims now carry a configurable lease bounded to 24 hours. A later request may
reclaim only an expired `IN_PROGRESS` claim; `SUCCEEDED` rows remain replay-only,
and completion clears the lease. `ai_tool_idempotency` is tenant-scoped with
RLS and is extended by migrations `022-ai-tool-idempotency.sql` and
`023-ai-tool-idempotency-lease.sql`. A no-op implementation is used only when the
tenant-aware JDBC boundary is unavailable in an isolated/infrastructure-free
composition. This closes the gateway replay gap but does not create concrete
appointment mutation handlers; those remain application-use-case work because
the existing appointment commands do not yet expose a compatible idempotency
contract.

Durable execution observability is now wired through `AiTraceRecorder`. Each
configured Spring AI chat/embedding provider attempt, structured design
extraction attempt, and controlled tool attempt records
backend tenant/principal/conversation/workflow correlation, provider/model or
tool metadata, outcome, latency, and nullable token/cost usage. The JDBC
adapter applies deterministic PII redaction before storing request, response,
argument, or error payloads in `ai_model_execution` and `ai_tool_call`.
Persistence is best effort at the execution boundary, so an observability
outage does not change customer-facing model/tool semantics. The no-op recorder
is selected when JDBC is unavailable.

### Optional Apache AGE graph progress

The graph foundation is now implemented behind provider-neutral contracts in
`libraries:ai-contracts` and an opt-in JDBC adapter in `modules:assistant`.
Apache AGE is treated as a disposable relationship read model. Migration 024
creates a tenant-scoped registry and attempts the AGE extension only when it is
available, so the standard pgvector image remains compatible. The adapter
derives one graph name from the authenticated tenant context, projects
allowlisted node/edge types idempotently, and exposes only the curated
`DESIGN_TO_SERVICE` traversal. All AGE statements run inside the same JDBC
transaction that loads AGE and sets its search path; dynamic Cypher and
LLM-generated graph queries are not accepted.

The optional local Compose overlay builds
`deployment/postgres/age-pgvector/Dockerfile` from the official AGE PG17 image
and official pgvector PG17/trixie artifacts. The default runtime and
application flag remain disabled. Live Testcontainers coverage verifies
idempotent projection, curated retrieval, derived graph-name isolation for two
tenants, and the registry version. Event-driven projection of every catalog
aggregate is intentionally a follow-up because the current catalog APIs do not
yet expose the required durable projection events.

## 6. Phase 5–7 — Workflow and quote vertical slice

- Add LangGraph4j workflow state and PostgreSQL checkpoints.
- Add deterministic route and semantic gateway nodes.
- Add specialized Spring AI clients and advisors.
- Add filtered application tool gateway.
- Add design extraction schema validation.
- Integrate deterministic quote calculation.
- Add HITL review persistence, endpoint, notification, resume, and optimistic
  locking.

## 7. Phase 8–9 — Learning and production hardening

- Trace redaction and durable candidate records are implemented. Candidate
  admission requires redacted bounded text and strong outcome evidence; records
  start in `PENDING_EVALUATION`.
- Deterministic lifecycle transitions now require `EVALUATING`, complete
  offline dataset/safety/regression/shadow gates, and a separate canary gate
  before `PROMOTED`. Optimistic PostgreSQL state updates prevent concurrent
  workers from overwriting a candidate.
- Admitted candidates now dispatch a stable, tenant-partitionable
  `LearningCandidateEvaluationRequested` event through Spring Modulith's
  durable publication registry. The envelope contains only trusted IDs and
  correlation metadata; candidate content remains in PostgreSQL and the
  evaluator remains asynchronous/offline.
- The offline `tools/ai-evaluation` package now provides a Python 3.13 Ragas
  scaffold. It redacts PII before evaluation, keeps tenant/principal metadata
  out of Ragas inputs, emits advisory regression/shadow metrics, and never
  promotes candidates or changes production indexes.
- Evaluation reports now persist metrics and gate evidence in a tenant-scoped,
  idempotent PostgreSQL table. A context-bound Java worker applies each report
  to the candidate lifecycle and safely ignores terminal-state redelivery.
- Add external evaluator transport and asynchronous embedding/index promotion
  worker.
- Add shadow/canary index promotion.
- Add Ragas evaluation scaffold.
- Add Java agent configuration for JVM observability.
- Add dashboards and alerts.
- Add WhatsApp/web streaming recovery and asynchronous job behavior.
- Run full regression, security, and architecture checks.

## 8. Definition of done

- Requirements and acceptance criteria are verified.
- Tests were written before implementation for new behaviors.
- No existing tests regress.
- Tenant and user isolation tests pass.
- Java 25 verification passes on the actual supported runtime.
- No uncommitted generated implementation artifacts remain.
- Documentation, ADRs, FCRs, runbook, and rollback evidence are updated.
