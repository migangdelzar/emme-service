# Implementation Plan: Emme AI Platform

| Field | Value |
|---|---|
| Status | Phases 0, 2, and the initial Phase 4 semantic slice complete; Phases 1 and 3–4 implementation in progress |
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
| 1 | Execution context, ScopedValue, executors | Context and concurrency unit tests | In progress |
| 2 | StructuredTaskScope and Joiners | Parallel runner and cancellation tests | Complete |
| 3 | AI foundation and Spring AI providers | Provider/embedding contract tests | In progress |
| 4 | pgvector intent/tool/cache indexes | Tenant-filtered vector integration tests | In progress |
| 5 | LangGraph4j graph and checkpoints | Workflow persistence/resume tests | Not started |
| 6 | Spring AI advisors and controlled tools | Advisor/tool policy integration tests | Not started |
| 7 | Design extraction, deterministic quote, HITL | Quote and optimistic-lock tests | Not started |
| 8 | Online enrichment and evaluation | Candidate/promotion safety tests | Not started |
| 9 | Channels, operations, and hardening | E2E, failure, and observability tests | Not started |

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

## 5. Phase 3–4 — AI and vector foundation

- Upgrade and pin Spring AI.
- Resolve compatible LangGraph4j versions before use.
- Add neutral AI foundation module.
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

### Phase 4 progress

The studio tenant-schema changelog now provisions tenant-scoped intent and tool
reference tables plus a principal-scoped expiring semantic cache. All three
tables use the existing explicit RLS convention, 1024-dimensional pgvector
columns, HNSW cosine indexes, and supporting tenant/active/expiry indexes. A
database-module migration contract test protects the changelog inclusion and
isolation invariants. JDBC retrieval and cache adapters now bind tenant and
principal from the authenticated AI execution context, apply model/prompt/
context filters, and constrain semantic tool search to backend-authorized
keys. Cache writes, hit accounting, and PostgreSQL integration tests remain a
follow-up slice.

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

- Add trace redaction and candidate records.
- Add asynchronous embedding and evaluation worker.
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
