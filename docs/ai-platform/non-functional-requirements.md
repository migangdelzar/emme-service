# Non-Functional Requirements: Emme AI Platform

| Field | Value |
|---|---|
| Status | Draft |
| Parent | [PRD](PRD.md) |

Targets below are initial engineering targets to validate with load tests and
production baselines; they are not claims about current performance.

## 1. Compatibility

- NFR-AI-001: Java 25 is the only supported JVM baseline.
- NFR-AI-002: Java 25 preview flags are present for `StructuredTaskScope` and
  Joiner compilation, tests, and JVM execution.
- NFR-AI-003: Spring AI and LangGraph4j versions are pinned and verified against
  Spring Boot 4.1 and the repository’s dependency lock.
- NFR-AI-004: Java agents are supported on JVM deployments; native-image paths
  have an in-process telemetry alternative.

## 2. Performance

- NFR-AI-010: Semantic cache hits have a bounded fast path and do not invoke the
  LLM.
- NFR-AI-011: Vector and model operations have explicit deadlines.
- NFR-AI-012: Parallel read-only work uses structured cancellation and does not
  leave child operations running after the parent deadline.
- NFR-AI-013: AI worker concurrency is bounded globally, per provider, and per
  tenant.
- NFR-AI-014: The system measures p50, p95, and p99 latency separately for
  embedding, vector search, model calls, tool calls, and total workflow.

## 3. Reliability

- NFR-AI-020: Workflow checkpoints survive application restart.
- NFR-AI-021: Mutating use cases are idempotent.
- NFR-AI-022: Retry policies distinguish safe reads from mutations.
- NFR-AI-023: Failed asynchronous jobs are retried with backoff and moved to a
  dead-letter state after the configured limit.
- NFR-AI-024: Redis failure degrades temporary acceleration but does not lose
  durable business state.
- NFR-AI-025: Vector-store failure causes safe abstention or fallback, never an
  unvalidated tool execution.

## 4. Security and privacy

- NFR-AI-030: Every tenant-scoped relational and vector query includes an
  authenticated tenant predicate.
- NFR-AI-031: User-specific memory and cache records include tenant and user
  scope.
- NFR-AI-032: Prompt/completion content is not exported to telemetry by default.
- NFR-AI-033: PII is redacted before evaluation or learning candidates are
  created.
- NFR-AI-034: Tool authorization is enforced in the application layer even when
  a model or vector selector produced the candidate.
- NFR-AI-035: No unrestricted SQL, Cypher, tenant selector, or permission tool is
  exposed to an LLM.

## 5. Observability

- NFR-AI-040: Every workflow correlates `traceId`, `tenantId`,
  `conversationId`, and `workflowId`.
- NFR-AI-041: Metrics use bounded labels; high-cardinality identifiers belong in
  traces or durable records.
- NFR-AI-042: Operators can observe cache hit rate, abstention, fallback,
  provider health, queue lag, HITL wait, token usage, and tenant cost.
- NFR-AI-043: Approval decisions and tool executions produce audit events.

## 6. Cost and governance

- NFR-AI-050: Provider routing supports per-tenant cloud opt-in/out.
- NFR-AI-051: Tenant and workflow AI budgets are enforced before model calls.
- NFR-AI-052: Production index promotion requires versioned evidence and a
  rollback pointer.
- NFR-AI-053: Ragas evaluation is asynchronous or CI-only.

## 7. Maintainability

- NFR-AI-060: Domain modules do not import Spring AI, LangGraph4j, Redis, or
  provider SDKs.
- NFR-AI-061: Provider, vector, executor, checkpoint, and memory boundaries use
  injectable ports.
- NFR-AI-062: Preview Java concurrency APIs are isolated behind a stable Emme
  abstraction.
- NFR-AI-063: Each acceptance requirement has unit, integration, or end-to-end
  verification.
