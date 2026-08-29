# Technical Specification: Emme AI Platform

| Field | Value |
|---|---|
| Status | Draft |
| Requirements | [Functional](requirements.md), [NFRs](non-functional-requirements.md) |
| Design | [Master design](../superpowers/specs/2026-08-27-ai-platform-semantic-architecture-design.md) |

## 1. Dependency graph

```text
applications/emme-platform
  → modules/assistant
  → modules/ai-platform
  → libraries/ai-contracts
  → module APIs
  → infrastructure adapters

modules/assistant
  → libraries/ai-contracts
  → modules/ai-platform
  → catalog-api
  → documents-api
  → appointments-api
  → tenancy-api
  → audit-api

modules/catalog
  → libraries/ai-contracts embedding/vision ports

modules/ai-platform
  → libraries/ai-contracts
  → kernel/shared only
```

The framework-neutral `ai-contracts` library removes the current
`catalog → assistant AI` coupling and prevents an assistant/catalog cycle.
`ai-platform` owns reusable provider and capability adapters without depending
on assistant application code. Assistant remains the composition boundary for
Emme-specific use cases and Spring AI/LangGraph4j workflow definitions.

## 2. Stable application ports

```java
public interface EmbeddingGateway {
  EmbeddingResult embed(EmbeddingRequest request);
  List<EmbeddingResult> embedBatch(List<EmbeddingRequest> requests);
}

public interface ChatGateway {
  ChatResult complete(ChatRequest request);
}

public interface VisionGateway {
  VisionResult analyze(VisionRequest request);
}

public interface SemanticClassifier {
  RouteDecision classify(AiExecutionContext context, NormalizedRequest request);
}

public interface SemanticToolSelector {
  ToolCandidate select(AiExecutionContext context, ToolSelectionRequest request);
}

public interface SemanticCache {
  Optional<CacheHit> find(AiExecutionContext context, CacheLookup lookup);
  void enqueueCandidate(AiExecutionContext context, CacheCandidate candidate);
}

public interface ParallelTaskRunner {
  <T> List<T> runRequired(List<? extends Callable<T>> tasks, Deadline deadline);
  <T> List<TaskOutcome<T>> runOptional(List<? extends Callable<T>> tasks, Deadline deadline);
}

public interface WorkflowCheckpointRepository {
  void save(WorkflowCheckpoint checkpoint);
  Optional<WorkflowCheckpoint> load(AiExecutionContext context, UUID workflowId);
}

public interface LearningSignalRecorder {
  void record(LearningSignal signal);
}
```

Production implementations are injected at the composition root. Domain code
does not instantiate providers, executors, clients, or repositories.

## 3. Spring AI clients and advisors

```text
ExtractionChatClient
  PromptVersionAdvisor
  StructuredOutputValidationAdvisor
  BudgetAdvisor
  TraceAdvisor

RagChatClient
  TenantSecurityAdvisor
  MessageChatMemoryAdvisor
  RetrievalAugmentationAdvisor
  PromptVersionAdvisor
  TraceAdvisor

FallbackAgentChatClient
  TenantSecurityAdvisor
  MessageChatMemoryAdvisor
  RetrievalAugmentationAdvisor
  PromptVersionAdvisor
  BudgetAdvisor
  TraceAdvisor
  one ToolCallingAdvisor
```

The fallback client receives only the tools allowed for the current context.
Direct semantic tool routes do not need the fallback agent.

The in-process tool boundary is represented by `AiToolDefinition`,
`AiToolInvocation`, `AiToolExecutionContext`, `AiToolResult`, and
`AiToolGateway`. `AuthorizedAiToolGateway` filters definitions by backend
roles, risk, user confirmation, and staff approval. `SemanticProactiveToolRouter`
passes only read-only, no-confirmation tools to the existing tenant-filtered
semantic selector; low-confidence routes abstain. Tool handlers receive a
backend-created execution context and delegate to application use cases. The
platform currently registers `getSalonServices` as a read-only catalog tool;
booking and other mutations are not eligible for proactive execution.

Durable execution traces use the application-level `AiTraceRecorder` contract.
`TracingChatCompletionPort` records each named chat-provider attempt, including
failed local attempts before a configured fallback succeeds. The authorized
tool gateway records successful, rejected, and failed tool calls after applying
the same backend authorization gates used for execution. The JDBC adapter
derives tenant, principal, conversation, workflow, and trace identifiers from
`AiExecutionContext`; it never accepts those values from model arguments.
Request, response, argument, and error payloads are redacted before storage.
Governed self-improvement uses `LearningCandidate` records from
`libraries:ai-contracts`. Candidate text is bounded and rejected when it still
contains common email, phone, or bearer-token patterns. The platform policy
admits only redacted candidates backed by accepted, validated, successful
outcomes without staff correction or policy violations. Candidates are stored
in PostgreSQL as `PENDING_EVALUATION`; they are never promoted during a
customer request. `LearningCandidateLifecyclePolicy` requires complete offline
datasets, deterministic safety checks, regression and shadow approval, and a
separate canary result before promotion. `JdbcLearningCandidateStateStore`
updates status with tenant and expected-version predicates, so concurrent
workers cannot overwrite one another. The asynchronous evaluator and versioned
embedding-index promotion worker remain a subsequent phase.
Admitted candidates are dispatched through the framework-neutral
`LearningCandidateEvaluationRequester` port. The assistant adapter publishes a
stable `LearningCandidateEvaluationRequested` Spring Modulith event using the
existing durable publication registry, partitioned by the backend tenant. The
event contains only trusted candidate/context identifiers and correlation
metadata; the candidate text and evidence remain in tenant-filtered PostgreSQL
and are loaded by the offline evaluator. Rejected candidates do not dispatch
evaluation work.

The repository also contains an offline Python 3.13 Ragas scaffold at
`tools/ai-evaluation`. It accepts anonymized JSONL samples, redacts common PII
before constructing a Ragas `EvaluationDataset`, and emits advisory metric and
gate results. It deliberately does not call the Java lifecycle service or
promote an embedding index. The Java lifecycle remains the authority for
candidate status; explicit shadow and canary evidence must be supplied by
separate controlled stages.
Token counts and estimated cost are nullable because provider usage metadata is
not guaranteed. Trace writes are best effort and PostgreSQL is authoritative
for the durable records; a no-op recorder is used where JDBC is unavailable.
`TracingEmbeddingModelPort` records embedding attempts without storing vector
values, and `SpringAiNailDesignExtractor` records structured extraction
metadata without storing image bytes.

## 4. Provider adapters

```text
LocalOllamaProvider
LocalMlxOpenAiCompatibleProvider
OxAlphaCloudProvider
MockProvider
```

Provider selection is task-aware and tenant-policy-aware. Embeddings are
versioned independently from chat models. All active vector queries reject
model/version/dimension mismatches.

The Spring AI chat boundary uses named `ChatClient` beans in explicit order.
`ChatProviderChain` catches only `ChatProviderUnavailableException`; the
application falls back to the existing provider-neutral model port after all
configured Spring AI clients are unavailable. The local Ollama client is
feature-gated, and additional provider modules may contribute named clients
without changing application logic. Every configured client receives the
tenant-security and prompt-version advisors at request execution time. No
advisor may derive or override tenant identity from model output.

## 5. LangGraph4j integration

LangGraph4j owns the workflow graph and checkpoint lifecycle. Graph nodes call
application ports. Spring AI is used inside model-facing nodes.

The platform must not combine a LangGraph4j model tool loop with a second Spring
AI tool loop. The recommended ownership is:

```text
LangGraph4j
  → outer workflow, branches, retries, pause/resume

Spring AI ToolCallingAdvisor
  → one fallback-agent model/tool loop

Application use cases
  → authoritative tool execution
```

The service uses a tenant-aware Emme `BaseCheckpointSaver` boundary around a
JDBC adapter. The adapter persists LangGraph checkpoint state in
`ai_workflow_checkpoint`, including the next node required for resume. Both
the wrapper and JDBC adapter require the backend `AiExecutionContext`; the
LangGraph thread ID must equal its workflow ID. The stock LangGraph4j
PostgreSQL saver is not the source of truth because its documented in-memory
cache can return stale state across instances; PostgreSQL remains authoritative.

The quote persistence slice includes JDBC adapters for workflow runs,
extraction results, quote drafts, and review tasks. All queries include the
authenticated tenant predicate. Review writes require the authenticated
reviewer and the expected task version, then append the decision in the same
transaction. The workflow owner (`principal_id`) is deliberately distinct from
the authenticated actor in `AiExecutionContext`: a staff member is authorized
by the application use case to resolve a client-owned workflow. The inbound
HTTP review adapter derives a stable PII-free reviewer UUID from the trusted
JWT issuer/subject, obtains tenant identity from the backend tenant context,
and requires a correlation ID plus idempotency key. The application service
loads the review and workflow under tenant predicates before rebinding the
workflow correlation. The concrete LangGraph resume adapter updates the
approval gate and invokes the persisted graph thread; rejected reviews remain
terminal without resuming the graph.

Redis operational state is exposed through `AiOperationalStatePort` and
`AiLiveEventPublisher`. The implementation is disabled unless
`app.ai.redis.enabled=true` and a `StringRedisTemplate` exists. Keys include
the backend-resolved tenant and workflow/conversation identity. Workflow
status is TTL-bound, lock release uses a compare-and-delete Lua script, and
live events contain only bounded status fields suitable for SSE/WebSocket
delivery. Redis is not used for durable history, quote artifacts, workflow
decisions, appointments, or audit logs.

Semantic intent routing is feature-gated and executes before model fallback.
Its vector decision requires the configured top-1 score and top-1/top-2 margin;
abstention invokes the existing model route. Semantic response caching is
principal-scoped, expiry-bound, and limited to context-free informational chat.
Transactional terms such as booking, availability, price, cancellation,
payment, and account changes bypass the cache deterministically.

## 6. Concurrency

```text
HTTP/worker boundary
  → create AiExecutionContext
  → bind ScopedValue
  → run graph
  → StructuredTaskScope for independent reads
  → Joiner for completion/cancellation
```

Named executors:

```text
applicationTaskExecutor
aiIoExecutor
aiBackgroundExecutor
aiCpuExecutor
aiScheduler
```

`StructuredTaskScope` is isolated behind `ParallelTaskRunner` because it is a
Java 25 preview API. No code globally overrides `ForkJoinPool.commonPool()`.

## 7. Error handling

| Failure | Behavior |
|---|---|
| Missing tenant context | Fail closed |
| Embedding timeout | Use deterministic route or safe fallback; never execute an unvalidated tool |
| Vector-store timeout | Skip semantic cache/tool shortcut and continue safe workflow |
| Low score/margin | Abstain and clarify or use structured fallback |
| Invalid model schema | Bounded repair, then clarify/HITL |
| Model timeout | Provider fallback only when tenant policy permits |
| MCP failure | Retry only safe operations; persist failure and offer safe degradation |
| Checkpoint conflict | Reject stale resume and require fresh state |
| Review version conflict | Return conflict; do not overwrite newer decision |
| Redis outage | Disable temporary acceleration; preserve PostgreSQL authority |
| Mutating retry | Require idempotency key and application-level duplicate protection |

## 8. Test strategy

- Unit: score/margin policy, slot validation, cache eligibility, tool policy,
  Joiner aggregation, context propagation, idempotency, and HITL transitions.
- Integration: PostgreSQL/pgvector, checkpoint persistence, Redis locks,
  Spring AI test doubles, MCP callbacks, and outbox events.
- End-to-end: FAQ, availability, booking, quote, ambiguous quote, multi-intent,
  wrong tenant, duplicate booking, provider timeout, MCP failure, and restart
  resume.
- Contract: provider adapters, MCP schemas, embedding dimensions, and event
  payloads.
