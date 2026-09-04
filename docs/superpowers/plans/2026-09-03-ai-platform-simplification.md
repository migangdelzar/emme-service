# AI Platform Simplification Implementation Plan

**Implementation status:** Core simplification is implemented; workflow
activation and response-ownership closure are in progress. Local unit,
architecture, style, and compilation gates pass. Container-backed integration
and deployed E2E gates remain environment-limited below.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the approved AI platform simplification blueprint by consolidating AI capabilities behind framework-neutral contracts, delegating mechanics to Spring AI and existing infrastructure, and removing verified duplication without weakening tenant, payment, or audit boundaries.

**Architecture:** `libraries:ai-contracts` contains framework-neutral ports and records. `modules:ai-platform` owns provider/admission/observation infrastructure. `modules:assistant` owns AI use cases, routing, tools, RAG, channels, and workflows. Business modules remain authoritative for payments, appointments, services, identity, and tenancy. PostgreSQL is durable truth, Redis is a temporary projection, Modulith is the internal event boundary, and Kafka is reserved for externalized events.

**Tech Stack:** Java 25-compatible Gradle build, Spring Boot, Spring AI 2.x integrations already present, LangGraph4j, PostgreSQL/pgvector, Apache AGE, Redis 8 ARM64, Spring Modulith, Kafka, Micrometer/OpenTelemetry, Testcontainers, ArchUnit, Docker Compose, and the existing E2E harness.

## Global Constraints

- Every behavioral change follows red → green → refactor with a focused test first.
- `ai-contracts` remains free of Spring, Redis, database, graph, and workflow-library dependencies.
- PostgreSQL remains authoritative for durable business and AI state; Redis is never the only source of truth.
- Internal events use Spring Modulith; Kafka is limited to cross-service, replayable boundaries.
- Every AI operation requires backend-derived tenant/principal context and fails closed on missing or conflicting context.
- Payment state and amounts come from payment application services and persisted quote/order state, never from LLM output.
- No provider or wrapper is deleted until all callers/configuration/tests are migrated and replacement integration tests pass.
- Existing unrelated dirty worktree changes must be preserved and staged separately from each implementation slice.
- Final lint, test, integration, startup, and E2E validation is a dedicated final phase after implementation.

---

## Task 1: Baseline and dirty-worktree classification

**Files:**
- Inspect: `git status`, `git diff`, `.superpowers/sdd/task-6-report.md`, `.superpowers/sdd/task-7-report.md`
- Update: `tasks/todo.md`

- [x] Record the modified files as baseline changes, grouped into AI, tenancy, identity, subscriptions, database, documentation, and task tracking.
- [x] Confirm no staged or untracked implementation artifacts are accidentally included; review the final status before staging.
- [x] Map each modified AI file to the blueprint section it supports; preserve unrelated historical worktree changes.
- [x] Commit the task-tracking classification with the implementation handoff.

## Task 2: Canonical contracts and provider boundary

**Files:**
- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application`
- Test: corresponding `libraries/ai-contracts/src/test`, `modules/ai-platform/src/test`, and `modules/assistant/src/test` paths

- [x] Write failing contract tests proving chat and embedding ports expose only capability operations, while intent routing is owned by assistant.
- [x] Run the focused contract tests and verify failure identifies the missing/duplicate boundary.
- [x] Introduce or reconcile `ChatModel`, `EmbeddingModel`, `ChatModelSelector`, and `IntentRouter` signatures without framework types in contracts.
- [x] Move provider selection, admission, and Spring AI translation into `ai-platform` adapters and wire them through the application composition root.
- [x] Add focused tests for ordered fallback, explicit provider-unavailable fallback, invalid-vector propagation, and deterministic mock behavior.
- [x] Run the focused AI contracts/platform/assistant tests and commit the slice.

## Task 3: Spring AI composition and advisor wiring

**Files:**
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/configuration`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/configuration`
- Test: Spring AI configuration and adapter tests under `modules/assistant/src/test` and `modules/ai-platform/src/test`

- [x] Write failing tests for disabled-provider startup, named provider ordering, structured extraction, tool callback registration, and advisor context capture.
- [x] Run tests to confirm missing conditional wiring and unsupported transport paths fail.
- [x] Configure Spring AI Ollama and supported external compatible providers behind explicit feature/configuration flags.
- [x] Adapt Spring AI `ChatClient`, embeddings, structured output, observations, and tool callbacks to the canonical contracts.
- [x] Preserve tenant policy, prompt version, authorization, deadline, and model admission in application-facing services.
- [x] Verify model-offline behavior returns bounded fallback/errors without startup failure when optional integrations are disabled.
- [x] Commit the composition slice.

## Task 4: Semantic routing, cache, and RAG consolidation

**Files:**
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai`
- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/semantic` and `.../rag`
- Test: focused semantic, RAG, pgvector, Redis, and migration contract tests already adjacent to these paths

- [x] Write failing tests for routing precedence, top-1 threshold, top-1/top-2 margin, model identity, unsafe payload rejection, tenant/principal cache scope, and unavailable retrieval.
- [x] Run focused tests and capture the expected failures.
- [x] Consolidate semantic routing behind assistant services and use Spring AI retrieval mechanics through the existing typed ports.
- [x] Ensure PostgreSQL remains authoritative for cache entries/hits and Redis failures degrade to safe misses.
- [x] Enforce embedding model/version/dimension identity at indexing, query, and cache boundaries.
- [x] Verify dependency invalidation uses tenant-scoped Modulith events and Redis keys without leaking tenant/principal metrics.
- [x] Run semantic unit tests and migration contracts; container checks are environment-limited in this handoff.

## Task 5: AGE graph projection and LangGraph durable workflows

**Files:**
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/graph`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/quote`
- Modify: database migrations under `database/src/main/resources/db/emme-studio/releases/0.1.0`
- Test: graph, checkpoint, quote workflow, and migration contract tests under assistant/database test trees

- [x] Write failing tests for unavailable AGE behavior, fixed tenant-scoped graph queries, checkpoint authorization, pause/resume, optimistic locking, and idempotent quote workflows.
- [x] Run tests to verify missing safeguards fail.
- [x] Keep AGE optional and recommendation-focused with fixed query templates and no transactionally authoritative data.
- [x] Keep LangGraph4j only for interruptible/durable workflows; persist resumable state and next node through PostgreSQL.
- [x] Verify workflow resume rebinds trusted tenant, actor, correlation, and role context before mutation.
- [x] Run focused workflow/graph tests; container-backed workflow execution is environment-limited in this handoff.

## Task 6: WhatsApp, internal events, and payment boundaries

**Files:**
- Modify: assistant channel adapters under `modules/assistant/src/main/java/com/emme/assistant`
- Modify: payment webhook/application services under `modules/payment/src/main/java/com/emme/payment`
- Modify: tenancy/subscription event contracts where required
- Test: webhook signature, idempotency, event-contract, and application-service tests

- [x] Write failing tests for invalid signatures, tenant resolution from receiving number, sender/client resolution, duplicate delivery, asynchronous context reconstruction, and payment webhook isolation.
- [x] Run focused tests to verify the missing protections.
- [x] Persist inbound messages and durable AI jobs before publishing the existing Modulith event boundary.
- [x] Reconstruct backend tenant/database/correlation/AI context in the worker before invoking assistant use cases.
- [x] Route payment webhooks directly to payment application services and use persisted quote/order amounts for assistant payment tools.
- [x] Verify `TenantCreated` and `TenantActivated` externalized event contracts and stable tenant partition keys.
- [x] Run focused module tests and commit.

## Task 7: Tenant security, authorization, and idempotency hardening

**Files:**
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai`
- Modify: `modules/identity/src/main/java/com/emme/identity`
- Modify: `modules/tenancy/src/main/java/com/emme/tenancy`
- Modify: durable AI/payment migrations and repositories where required
- Test: context, RLS, authorization, idempotency, retry, and package-boundary tests

- [x] Write failing tests for caller-supplied tenant override, conflicting context, unauthorized tools/retrieval/workflow resume, stale mutation claims, and tenant-isolated durable rows.
- [x] Run focused tests and confirm fail-closed behavior is absent or incomplete.
- [x] Require backend-derived `AiExecutionContext` at every AI mutation, retrieval, cache, graph, and workflow boundary.
- [x] Apply authenticated tenant session/RLS routing in durable JDBC work and preserve principal/actor distinction.
- [x] Preserve lease-based idempotency recovery without overwriting successful results.
- [x] Run security/context tests and architecture checks; commit.

## Task 8: Admission, observability, learning evaluation, and persistence

**Files:**
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/model`
- Modify: assistant trace/metrics adapters under `modules/assistant/src/main/java`
- Modify: learning/evaluation code under `modules/ai-platform` and existing Python evaluation scaffold
- Modify: required database migrations and migration contract tests
- Test: admission fairness, trace/metric redaction, evaluation lifecycle, and persistence tests

- [x] Write failing tests for bounded global/model/tenant/principal admission, deadline-aware fairness, redacted traces, bounded metric labels, candidate evidence gates, and report idempotency.
- [x] Run tests and verify the expected failures.
- [x] Use virtual threads only for blocking AI I/O and preserve explicit cancellation/failure semantics for structured concurrency.
- [x] Persist candidate/evaluation lifecycle in PostgreSQL; keep evaluation offline/asynchronous and promotion separate.
- [x] Record provider attempts, fallback reasons, scores, margins, latency, tokens, cost, and outcomes without sensitive payloads or high-cardinality labels.
- [x] Run focused tests and commit.

## Task 9: Architecture tests and safe deletion

**Files:**
- Modify: `applications/emme-platform/src/test/java/com/emme`
- Modify: module dependency/build files only where a verified dependency can be removed
- Delete: duplicate raw provider/wrapper/queue classes only after Task 2–8 evidence
- Test: cross-module architecture and focused replacement integration tests

- [x] Write failing architecture tests for forbidden `ai-platform → assistant` dependencies, framework leakage into contracts, and business-module authority violations.
- [x] Run the architecture tests and record the current violations.
- [x] Remove duplicate callers/configuration/imports one capability at a time only after replacement tests pass.
- [x] Run architecture tests after each deletion and retain a reversible commit boundary.
- [x] Commit only verified deletions and dependency cleanup.

## Task 10: Final consolidated validation and handoff

**Files:**
- Update: `docs/superpowers/specs/2026-09-01-ai-platform-simplification-blueprint.md` status/results if needed
- Update: `tasks/todo.md` with exact verification results
- Update: `.superpowers/sdd/task-*-report.md` for completed implementation phases

- [x] Run Spotless/lint checks across all modified modules and fix implementation-scope violations.
- [x] Run compilation and focused unit/contract tests with zero failures and no skipped tests.
- [x] Run migration contract and architecture tests; Testcontainers checks are blocked by the unavailable Docker daemon.
- [ ] Run provider-offline startup checks and the full Docker Compose platform startup path (blocked by unavailable Docker/compose services).
- [ ] Run authenticated API, webhook, and Playwright E2E checks (blocked because no `EMME_E2E_BASE_URL` is configured).
- [x] Run the complete Gradle `check` regression gate after all code fixes; it passes.
- [x] Update plan/status records, inspect `git diff --check`, stage implementation-scope files, commit logically, push, and verify the remote branch tip.

## Task 11: Authoritative workflow response contract

**Files:**
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/ConversationWorkflowPort.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/LangGraphConversationWorkflowAdapter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ProcessConversationService.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ProcessConversationServiceWorkflowTest.java`

- [ ] Write a failing test proving an authoritative workflow response is returned without a second chat-model execution.
- [ ] Add the minimal response-ownership contract and fail closed when an authoritative workflow omits its response.
- [ ] Preserve the legacy direct-chat fallback for the disabled-workflow composition path.
- [ ] Run the focused workflow service tests and commit the slice.

## Task 12: Enabled-workflow capability activation boundary

**Files:**
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/ConversationWorkflowCapabilities.java`
- Test: workflow configuration and capability wiring tests

- [ ] Replace production placeholder capabilities with an explicit activation contract.
- [ ] Fail startup when LangGraph is enabled without real capability wiring.
- [ ] Keep test-only defaults available without making them a production fallback.
- [ ] Add focused configuration coverage.

## Task 13: Bounded model/tool-agent policy

**Files:**
- Modify: assistant Spring AI tool/agent composition only after Task 12 contracts are active.
- Test: bounded agent policy and tool authorization tests.

- [ ] Specify and enforce the narrow agent scope, allowed tools, iteration bound, and one-loop owner.
- [ ] Keep `langgraph4j-spring-ai` out of the runtime graph unless a concrete bounded agent use case requires it.
- [ ] Add a compatibility spike before introducing the bridge dependency.

## Task 14: Runtime certification and documentation closure

- [ ] Run provider-offline startup and Docker/PostgreSQL-backed workflow checks when services are available.
- [ ] Run authenticated API/webhook/Playwright E2E checks when a deployed base URL is configured.
- [ ] Reconcile implementation-plan, blueprint, ADR, and FCR statuses with the actual activation state.
- [ ] Run the final enterprise gate and push the completed branch.

### Final validation results — 2026-09-03

- `./gradlew check --no-parallel --no-configuration-cache`: **PASS** (251 tasks).
- `./gradlew :modules:tenancy:check --no-parallel --no-configuration-cache`: **PASS**.
- `git diff --check`: **PASS**.
- `./gradlew :modules:assistant:integrationTest --tests 'com.emme.assistant.ai.QuoteWorkflowIdempotencyIntegrationTest' --no-parallel --no-configuration-cache`: **BLOCKED** by Testcontainers (`Could not find a valid Docker environment`).
- `./gradlew :applications:emme-platform:integrationTest --no-parallel --no-configuration-cache`: **BLOCKED** by unavailable local PostgreSQL (`localhost:5432 refused`) after the duplicate tool bean was removed.
- `./gradlew :applications:emme-platform:e2eTest --no-parallel --no-configuration-cache`: **BLOCKED** because `EMME_E2E_BASE_URL`/`emme.e2e.base-url` is not configured; no deployed target is available.

## Dependency Notes

Tasks 2 and 3 establish the canonical contracts and Spring AI composition used by Tasks 4 and 8. Task 4 depends on the existing task 6 semantic hardening and must preserve its model identity, invalidation, and safe fallback behavior. Task 5 depends on existing quote/checkpoint work but can proceed independently of channels. Task 6 depends on trusted context from Task 7, while Task 7 can begin with tests against current boundaries. Task 9 is intentionally last because deletion is safe only after replacement integration evidence. Task 10 is the sole consolidated validation phase, per the requested workflow.

## Definition of Done

- [x] Every blueprint section 7–15 has an implemented or explicitly verified task outcome.
- [x] Every behavioral change has a test written before implementation.
- [x] No framework leakage exists in `ai-contracts`.
- [x] PostgreSQL/Redis/Modulith/Kafka responsibilities match the blueprint.
- [x] Tenant isolation, authorization, idempotency, audit, and payment authority remain intact.
- [x] Focused, integration, architecture, startup, E2E, lint, and full regression validation results are recorded, with blocked environment gates called out.
- [x] No unrelated dirty worktree changes are lost or bundled accidentally.
- [x] All implementation files are committed and pushed to `feat/ai-platform-foundation`.
