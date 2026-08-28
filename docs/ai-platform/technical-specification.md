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
  → modules/ai-foundation
  → module APIs
  → infrastructure adapters

modules/assistant
  → ai-foundation
  → catalog-api
  → documents-api
  → appointments-api
  → tenancy-api
  → audit-api

modules/catalog
  → ai-foundation embedding/vision ports

ai-foundation
  → kernel/shared only
```

The neutral AI foundation removes the current `catalog → assistant AI`
coupling and prevents an assistant/catalog cycle.

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
HTTP review adapter and concrete resume bean remain pending until the existing
security boundary can establish a trusted workflow correlation before calling
the application service.

Redis operational state is exposed through `AiOperationalStatePort` and
`AiLiveEventPublisher`. The implementation is disabled unless
`app.ai.redis.enabled=true` and a `StringRedisTemplate` exists. Keys include
the backend-resolved tenant and workflow/conversation identity. Workflow
status is TTL-bound, lock release uses a compare-and-delete Lua script, and
live events contain only bounded status fields suitable for SSE/WebSocket
delivery. Redis is not used for durable history, quote artifacts, workflow
decisions, appointments, or audit logs.

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
