# Emme AI Platform Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Complete the approved Emme AI platform inside the existing `emme-service` modular monolith, connecting durable conversations, LangGraph4j workflows, appointment mutations, Mac-safe asynchronous jobs, RAG/AGE, evaluation promotion, guardrails, MCP, observability, and production verification without weakening tenant isolation.

**Architecture:** Keep `ai-contracts` framework-neutral, `ai-platform` reusable and provider-oriented, and `assistant` responsible for Emme-specific orchestration and adapters. Use Spring AI for model/embedding/tool execution, LangGraph4j for durable stateful workflows, PostgreSQL/pgvector for authoritative durable state, Redis for temporary operational state, and Spring Modulith JDBC event publication for initial asynchronous jobs. Kafka remains a future transport adapter, not the model backpressure mechanism.

**Tech Stack:** Java 25, Spring Boot 4.1.x, Spring Modulith 2.1.0, Spring AI 2.0.1, LangGraph4j 1.8.25, PostgreSQL, pgvector, optional Apache AGE, Redis 8, Ollama with Gemma 4 and EmbeddingGemma 300M, Gradle, Testcontainers, Micrometer/OpenTelemetry, Python 3.13, and Ragas.

## Global Constraints

- Tenant context must come from the authenticated JWT/backend context; the model must never supply or override `tenantId`, `principalId`, roles, permissions, prices, availability, or ownership.
- PostgreSQL is authoritative for tenants, conversations, messages, workflows, checkpoints, quotes, approvals, appointments, traces, audit records, evaluation reports, and index metadata.
- Redis is limited to sessions, locks, short-lived cache, rate limiting, temporary worker state, and live workflow events.
- PostgreSQL/pgvector is the durable semantic source; Redis vector indexes are rebuildable hot projections only.
- Spring AI is the model, embedding, structured-output, advisor, retrieval, and tool execution layer.
- LangGraph4j is the only workflow orchestrator; no competing graph or workflow engine may be introduced.
- Domain and application business rules remain framework-independent and are called by AI adapters through public use-case contracts.
- Prices, availability, appointment mutations, cancellation eligibility, payments, permissions, and final business decisions are deterministic backend results.
- Java 25 preview APIs remain behind stable Emme ports; `StructuredTaskScope` and Joiners are never used as durable queues.
- The existing `BoundedModelExecutionScheduler` is the model-capacity boundary and must remain fair across provider, capability, tenant, and user limits.
- Embedding indexes and queries must use the same EmbeddingGemma model version and 768-dimensional profile.
- Ragas runs asynchronously or in CI only; it cannot authorize promotion or replace deterministic business tests.
- All mutating commands require authorization, validation, audit logging, confirmation where specified, and a durable idempotency key.
- AGE is an optional derived read model; no transactional data or price authority moves from PostgreSQL into AGE.
- External MCP access requires an authenticated and authorized boundary; no unrestricted MCP or generated Cypher is allowed.
- No paid model API is used by normal unit or integration tests.
- Existing unrelated worktree modifications in identity, subscriptions, and tenancy must remain unstaged.
- Every task follows Red → Green → Refactor, runs its focused verification, and ends with a conventional commit.

## Current Integration Map

| Existing area | Reuse point | Planned extension |
|---|---|---|
| `libraries:kernel` | `AiExecutionContext`, `ScopedValue`, tenant/MDC bridge, `ParallelTaskRunner` | Preserve ports and add only generic contracts required by new workflows |
| `libraries:ai-contracts` | provider, embedding, routing, tool, graph, learning contracts | Add conversation, job, guardrail, MCP, and index-promotion contracts |
| `modules:ai-platform` | provider adapters, embedding chain, model scheduler, learning lifecycle | Add durable evaluation/index promotion adapters and worker ports |
| `modules:assistant` | semantic services, Spring AI advisors, tool gateway, quote workflow, Redis adapters, traces | Add generic workflow, appointment tools, quote entrypoint, live events, guardrails, MCP, and projectors |
| `modules:appointments` | deterministic availability and appointment use cases | Add actor-aware mutation commands and idempotency-compatible application contracts |
| `modules:catalog` / `modules:services` | tenant-scoped catalog/service use cases and image storage | Add post-commit projection events and read capability for design image analysis |
| `modules:documents` | tenant-scoped document/chunk search and hybrid retrieval | Add version-aware embedding/index lifecycle where existing contracts do not provide it |
| `database` | Liquibase changelog, RLS, existing AI tables, event publication | Add only required job/index/projection columns/tables after contract tests |
| deployment | JVM/native Compose, Redis, Kafka and observability overlays | Add AI worker/status configuration and OpenTelemetry agent wiring |

## Task Dependency Graph

```text
Task 1 contracts and durable conversation boundary
    ↓
Task 2 generic LangGraph4j workflow and resume
    ↓
Task 3 quote entrypoint and image reader
    ↓
Task 4 appointment actor-aware mutations and tools
    ↓
Task 5 durable AI jobs and backpressure
    ↓
Task 6 live status, SSE/WebSocket recovery, and WhatsApp graph integration
    ↓
Task 7 RAG ingestion/versioning and AGE projection lifecycle
    ↓
Task 8 evaluation transport, shadow/canary indexes, and rollback
    ↓
Task 9 guardrails and authenticated MCP
    ↓
Task 10 observability, security, E2E, deployment, and release evidence
```

Tasks 1–4 produce the customer-facing vertical slice. Task 5 is required
before enabling long-running asynchronous work. Tasks 7–8 may be developed in
parallel after the generic workflow contracts exist, but their production
flags remain disabled until Task 10 verification passes.

---

### Task 1: Durable conversation-aware AI request boundary

**Files:**

- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/api/command/ProcessConversationCommand.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/api/result/ProcessConversationResult.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/api/usecase/ProcessConversationUseCase.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/ConversationMemoryPort.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ProcessConversationService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/request/ChatRequest.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/controller/AiController.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/security/AiWebExecutionContextFactory.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/application/port/out/ConversationRepository.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/application/port/out/ConversationEventRepository.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ProcessConversationServiceTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/in/web/controller/AiControllerConversationTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/in/web/security/AiWebExecutionContextFactoryTest.java`

**Interfaces:**

```java
public interface ProcessConversationUseCase {
  ProcessConversationResult process(ProcessConversationCommand command);
}

public record ProcessConversationCommand(
    UUID conversationId,
    String message,
    String channel,
    String idempotencyKey) {}

public interface ConversationMemoryPort {
  ConversationSnapshot load(UUID conversationId, AiExecutionContext context);
  void appendUserMessage(UUID conversationId, String message, AiExecutionContext context);
  void appendAssistantMessage(UUID conversationId, String message, AiExecutionContext context);
}

public record ConversationSnapshot(
    UUID conversationId,
    List<ConversationEventDetails> events) {}
```

- Consumes: Existing conversation use cases/repositories and
  `AiExecutionContextScope`.
- Produces: A typed request boundary that resolves an existing conversation
  only within the authenticated tenant and persists both user and assistant
  messages.

- [ ] **Step 1: Write the failing test.** Add tests that reject a conversation
  from another tenant, reject a blank message or missing idempotency key,
  append the user message before orchestration, and append the assistant result
  after successful completion.

```java
@Test
void rejectsConversationOutsideAuthenticatedTenant() {
  assertThatThrownBy(() -> service.process(commandFor(otherTenantConversation)))
      .isInstanceOf(SecurityException.class);
}

@Test
void persistsBothSidesOfACompletedConversationTurn() {
  ProcessConversationResult result = service.process(commandFor(ownConversation));

  assertThat(result.conversationId()).isEqualTo(CONVERSATION_ID);
  assertThat(memory.messages()).containsExactly("question", "answer");
}
```

- [ ] **Step 2: Run test to verify it fails.**

Run:

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*ProcessConversationServiceTest' --tests '*AiControllerConversationTest'
```

Expected: compilation or assertion failure because the use case and
conversation-aware context path do not exist.

- [ ] **Step 3: Write the minimal implementation.** Add the records and port,
  require `AiExecutionContextScope.requireCurrent()`, resolve the optional
  conversation through a tenant-scoped application boundary, append the user
  message, delegate to the workflow port, append the validated response, and
  return the durable conversation/workflow identifiers. Update `AiController`
  to accept `conversationId` and the `Idempotency-Key` header; do not accept a
  frontend tenant ID.

- [ ] **Step 4: Run test to verify it passes.**

Run the focused command from Step 2. Expected: all selected tests pass and
existing AI controller tests remain green.

- [ ] **Step 5: Refactor and commit.** Keep persistence calls behind
  `ConversationMemoryPort`, remove duplicated context construction, run
  `:modules:assistant:spotlessCheck`, and commit:

```shell
git add modules/assistant/src/main/java modules/assistant/src/test/java
git commit -m "feat(assistant): add durable conversation AI boundary"
```

### Task 2: Generic LangGraph4j workflow and persisted resume

**Files:**

- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/ConversationWorkflowPort.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/ConversationWorkflowGraph.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/LangGraphConversationWorkflowAdapter.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/domain/workflow/ConversationWorkflowStatus.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/domain/workflow/ConversationWorkflowSnapshot.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ProcessConversationService.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/ConversationWorkflowGraphTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/LangGraphConversationWorkflowAdapterTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ProcessConversationServiceWorkflowTest.java`

**Interfaces:**

```java
public interface ConversationWorkflowPort {
  ConversationWorkflowSnapshot startOrResume(
      ProcessConversationCommand command, AiExecutionContext context);
}

public enum ConversationWorkflowStatus {
  RECEIVED, RUNNING, WAITING_FOR_CONFIRMATION, WAITING_FOR_APPROVAL,
  CLARIFICATION_REQUIRED, SUCCEEDED, REJECTED, FAILED
}
```

- Consumes: Task 1 conversation boundary, existing
  `TenantAwareCheckpointSaver`, and `QuoteWorkflowGraph` conventions.
- Produces: One generic workflow composition root with the quote workflow as a
  capability subgraph and PostgreSQL checkpoints keyed by trusted workflow and
  conversation identifiers.

- [ ] **Step 1: Write the failing test.** Verify graph nodes expose the
  required lifecycle, route to a waiting state for approval, stop at the
  checkpoint, and resume without re-executing completed nodes.

```java
@Test
void pausesAtApprovalAndResumesFromThePersistedCheckpoint() throws Exception {
  CompiledGraph<AgentState> graph = workflowGraph.compile();

  AgentState paused = invoke(graph, inputWith("needsApproval", true));
  assertThat(paused.value("status")).hasValue("WAITING_FOR_APPROVAL");

  AgentState resumed = resume(graph, paused, Map.of("approval", "approved"));
  assertThat(resumed.value("status")).hasValue("SUCCEEDED");
}
```

- [ ] **Step 2: Run test to verify it fails.**

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*ConversationWorkflowGraphTest' --tests '*LangGraphConversationWorkflowAdapterTest'
```

Expected: missing generic graph/adapter symbols or a failing status assertion.

- [ ] **Step 3: Write the minimal implementation.** Build one graph with
  `receive_request`, `resolve_authenticated_context`, `initialize_workflow`,
  `normalize_input`, `detect_explicit_intent`, `decompose_multi_intent_request`,
  `semantic_route_with_pgvector`, `confidence_gate`, `extract_required_slots`,
  `retrieve_context_if_needed`, `execute_tool`, `validate_business_result`,
  `approval_gate`, `compose_response`, and `finish` nodes. Use conditional
  edges and checkpoint configuration; delegate all business work to ports.
  Add the quote subgraph at the quote route and preserve its existing state
  transitions.

- [ ] **Step 4: Run test to verify it passes.** Run the focused command from
  Step 2 and the existing `*QuoteWorkflowGraphTest` suite. Expected: all pass.

- [ ] **Step 5: Refactor and commit.** Keep graph state separate from chat
  memory, make node names constants, and commit:

```shell
git add modules/assistant/src/main/java modules/assistant/src/test/java
git commit -m "feat(assistant): add generic persisted conversation workflow"
```

### Task 3: Authenticated quote submission and image reader

**Files:**

- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/request/DesignQuoteRequest.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/response/DesignQuoteResponse.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/controller/DesignQuoteController.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/storage/CatalogDesignImageReader.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/DesignImageMetadataRepository.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ProcessDesignQuoteService.java`
- Modify: `modules/catalog/src/main/java/com/emme/catalog/application/port/out/ImageStorage.java`
- Modify: `modules/catalog/src/main/java/com/emme/catalog/adapter/out/client/storage/LocalImageStorage.java`
- Modify: `database/src/main/resources/db/emme-studio/releases/0.1.0/025-ai-design-images.sql`
- Modify: `database/src/main/resources/db/emme-studio/changelog.yaml`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/in/web/controller/DesignQuoteControllerTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/storage/CatalogDesignImageReaderTest.java`
- Test: `database/src/test/java/com/emme/database/AiDesignImageMigrationContractTest.java`

**Interfaces:**

```java
public interface DesignImageReader {
  Optional<StoredImage> read(String storageKey, AiExecutionContext context);
}

public record StoredImage(String contentType, byte[] bytes, String checksum) {}
```

- Consumes: Existing catalog image storage abstraction and
  `ProcessDesignQuoteUseCase`.
- Produces: A tenant-scoped multipart/start endpoint that returns a durable
  workflow identifier and connects approved image storage to Spring AI vision
  extraction.

- [ ] **Step 1: Write the failing test.** Cover unauthorized requests, image
  size/content-type rejection, tenant-scoped storage keys, and successful
  invocation of `ProcessDesignQuoteUseCase` with the stored key.

```java
@Test
void storesImageUnderAuthenticatedTenantAndStartsQuoteWorkflow() throws Exception {
  mockMvc.perform(multipart("/api/ai/quotes")
          .file(new MockMultipartFile("image", "design.jpg", "image/jpeg", bytes)))
      .andExpect(status().isAccepted())
      .andExpect(jsonPath("$.workflowId").exists());

  verify(storage).store(TENANT_ID, bytes);
  verify(quote).process(any(ProcessDesignQuoteCommand.class));
}
```

- [ ] **Step 2: Run test to verify it fails.**

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*DesignQuoteControllerTest' --tests '*CatalogDesignImageReaderTest'
```

Expected: endpoint/adapter is not found.

- [ ] **Step 3: Write the minimal implementation.** Add request validation,
  authenticated context construction, tenant-prefixed storage, image metadata
  persistence, and delegation to the existing quote use case. Extend the
  catalog storage capability with a tenant-safe read operation without
  exposing filesystem paths. Keep local storage for development and leave an
  object-storage adapter behind the same port.

- [ ] **Step 4: Run test to verify it passes.** Run focused assistant and
  database migration tests. Expected: all pass with RLS contract coverage.

- [ ] **Step 5: Refactor and commit.** Ensure image bytes never appear in logs,
  traces, URLs, or Redis values. Commit:

```shell
git add modules/assistant modules/catalog database/src/main/resources/db/emme-studio
git commit -m "feat(assistant): expose tenant-safe design quote entrypoint"
```

### Task 4: Appointment actor-aware mutations and AI tools

**Files:**

- Create: `modules/appointments/src/main/java/com/emme/appointments/api/command/AppointmentActor.java`
- Create: `modules/appointments/src/main/java/com/emme/appointments/api/command/CreateAppointmentCommand.java`
- Create: `modules/appointments/src/main/java/com/emme/appointments/api/command/CancelAppointmentCommand.java`
- Create: `modules/appointments/src/main/java/com/emme/appointments/api/command/RescheduleAppointmentCommand.java`
- Create: `modules/appointments/src/main/java/com/emme/appointments/api/usecase/BookAppointmentUseCase.java`
- Create: `modules/appointments/src/main/java/com/emme/appointments/api/usecase/CancelAuthorizedAppointmentUseCase.java`
- Create: `modules/appointments/src/main/java/com/emme/appointments/api/usecase/RescheduleAuthorizedAppointmentUseCase.java`
- Modify: `modules/appointments/src/main/java/com/emme/appointments/application/service/CreateAppointmentService.java`
- Modify: `modules/appointments/src/main/java/com/emme/appointments/application/service/CancelAppointmentService.java`
- Modify: `modules/appointments/src/main/java/com/emme/appointments/application/service/RescheduleAppointmentService.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/tool/AppointmentToolConfiguration.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/tool/CreateAppointmentToolHandler.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/tool/CancelAppointmentToolHandler.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/tool/RescheduleAppointmentToolHandler.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/tool/AiToolRisk.java`
- Test: `modules/appointments/src/test/java/com/emme/appointments/application/service/AuthorizedAppointmentMutationTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/tool/AppointmentToolHandlerTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/tool/AuthorizedAiToolGatewayAppointmentTest.java`

**Interfaces:**

```java
public record AppointmentActor(
    UUID tenantId, UUID principalId, Set<String> roles, String idempotencyKey) {}

public interface BookAppointmentUseCase {
  AppointmentDetails book(CreateAppointmentCommand command);
}

public record CreateAppointmentCommand(
    AppointmentActor actor,
    UUID customerId,
    UUID serviceId,
    UUID artistId,
    Instant startsAt,
    Instant endsAt,
    boolean confirmed) {}
```

- Consumes: Existing appointment domain rules, repositories, events, and
  `AiToolIdempotencyStore`.
- Produces: AI-safe appointment mutations that validate tenant ownership,
  customer ownership, role, state, policy, collision, confirmation, and
  idempotency in the application layer.

- [ ] **Step 1: Write the failing test.** Add tests proving that a mutation is
  rejected for another tenant, another customer, missing confirmation, missing
  idempotency, invalid appointment state, and duplicate requests. Verify a
  successful request invokes the domain service once and a replay returns the
  stored result.

```java
@Test
void doesNotCancelAnAppointmentOutsideTheActorTenant() {
  assertThatThrownBy(() -> cancel.cancel(command(OTHER_TENANT)))
      .isInstanceOf(SecurityException.class);
}

@Test
void duplicateBookingReturnsTheDurableFirstResult() {
  AppointmentDetails first = gateway.execute(createBookingInvocation());
  AppointmentDetails replay = gateway.execute(createBookingInvocation());

  assertThat(replay).isEqualTo(first);
  verify(useCase, times(1)).book(any());
}
```

- [ ] **Step 2: Run test to verify it fails.**

```shell
mise exec java@25.0.2 -- ./gradlew :modules:appointments:test --tests '*AuthorizedAppointmentMutationTest' :modules:assistant:test --tests '*AppointmentToolHandlerTest'
```

Expected: missing actor-aware command and handler failures.

- [ ] **Step 3: Write the minimal implementation.** Add actor-aware commands
  and application services. Map `AiExecutionContext` to `AppointmentActor` in
  the tool handlers. Keep the LLM-provided fields limited to validated
  business arguments. Require confirmation for all mutations and route every
  mutation through `AuthorizedAiToolGateway` and the existing PostgreSQL
  idempotency store.

- [ ] **Step 4: Run test to verify it passes.** Run focused appointment and
  assistant tests plus existing appointment controller tests. Expected: all
  pass and existing non-AI callers retain compatible use cases.

- [ ] **Step 5: Refactor and commit.** Keep old public methods as adapters only
  when they can preserve existing callers without weakening the new actor-aware
  path. Commit:

```shell
git add modules/appointments modules/assistant
git commit -m "feat(ai): add authorized appointment mutation tools"
```

### Task 5: Durable AI jobs, worker fairness, and backpressure

**Files:**

- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/job/AiJobType.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/job/AiJobRequest.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/AiJobPublisher.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/AiJobStatusStore.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/domain/job/AiJobStatus.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/event/SpringModulithAiJobPublisher.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/AiJobWorkerService.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/AiJobExecutorConfiguration.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/AiJobProperties.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/AiExecutorConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/event/package-info.java`
- Modify: `database/src/main/resources/db/emme-studio/releases/0.1.0/026-ai-job-state.sql`
- Modify: `database/src/main/resources/db/emme-studio/changelog.yaml`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/AiJobWorkerServiceTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/AiJobExecutorConfigurationTest.java`
- Test: `database/src/test/java/com/emme/database/AiJobMigrationContractTest.java`

**Interfaces:**

```java
public enum AiJobType {
  CONVERSATION, DESIGN_QUOTE, EVALUATION, GRAPH_PROJECTION, INDEX_PROMOTION
}

public interface AiJobPublisher {
  void publish(AiJobRequest request, AiExecutionContext context);
}

public interface AiJobStatusStore {
  AiJobStatus claim(UUID jobId, AiExecutionContext context);
  void complete(UUID jobId, AiExecutionContext context);
  void fail(UUID jobId, String errorCode, AiExecutionContext context);
}
```

- Consumes: Existing Spring Modulith JDBC event publication and
  `BoundedModelExecutionScheduler`.
- Produces: Durable job lifecycle, bounded worker executor, retry/quarantine
  behavior, and metrics for queue depth, lag, claim duration, and failure.

- [ ] **Step 1: Write the failing test.** Verify jobs are persisted before
  publication, duplicate events do not run twice, worker claims are tenant
  scoped, retryable failures are retried with bounded backoff, and exhausted
  jobs enter a durable quarantine/dead-letter state.

```java
@Test
void duplicateJobPublicationExecutesOnlyOnce() {
  worker.handle(eventFor(JOB_ID));
  worker.handle(eventFor(JOB_ID));

  verify(graph, times(1)).run(any(), any());
}

@Test
void exhaustedRetryMovesJobToDeadLetter() {
  worker.handle(retryableFailureEvent(JOB_ID));

  assertThat(statusStore.status(JOB_ID)).isEqualTo(AiJobStatus.DEAD_LETTER);
}
```

- [ ] **Step 2: Run test to verify it fails.**

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*AiJobWorkerServiceTest' :database:test --tests '*AiJobMigrationContractTest'
```

Expected: missing job contracts, tables, or worker behavior.

- [ ] **Step 3: Write the minimal implementation.** Persist job state inside
  the initiating transaction, publish a trusted Spring Modulith event, handle
  it on a bounded executor, rebind context from the durable record, and claim
  idempotently. Use exponential backoff with a fixed maximum attempt count and
  store exhausted jobs as `DEAD_LETTER`. Apply the existing model scheduler
  for every model operation. Do not create a second queue or use Kafka for
  admission control.

- [ ] **Step 4: Run test to verify it passes.** Run focused tests and the
  existing `BoundedModelExecutionSchedulerTest` suite. Expected: all pass.

- [ ] **Step 5: Refactor and commit.** Add bounded executor configuration with
  explicit queue capacity, maximum worker count, and per-tenant fairness
  metrics. Commit:

```shell
git add libraries/ai-contracts modules/assistant database/src/main/resources/db/emme-studio
git commit -m "feat(ai): add durable bounded AI job processing"
```

### Task 6: Live workflow status, SSE recovery, and WhatsApp graph integration

**Files:**

- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/AiLiveEventReader.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/redis/RedisAiLiveEventReader.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/controller/AiWorkflowStatusController.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/response/AiWorkflowEventResponse.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/redis/RedisAiLiveEventPublisher.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/redis/RedisAiKeys.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/application/service/ProcessWhatsAppMessageService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/adapter/in/web/controller/ConversationController.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/in/web/controller/AiWorkflowStatusControllerTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/redis/RedisAiLiveEventReaderTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/application/service/ProcessWhatsAppMessageWorkflowTest.java`

**Interfaces:**

```java
public interface AiLiveEventReader {
  List<AiLiveEvent> read(UUID tenantId, UUID conversationId, String afterEventId, int limit);
}

public record AiLiveEvent(
    String id,
    String type,
    String payload,
    Instant createdAt) {}
```

- Consumes: Task 1 conversation identifiers, Task 5 job status, existing Redis
  Streams publisher, and PostgreSQL workflow repositories.
- Produces: Reconnectable SSE status delivery and a WhatsApp path that waits
  for durable workflow completion before sending the complete response.

- [ ] **Step 1: Write the failing test.** Verify authorized SSE access,
  tenant-scoped replay, last-event ID behavior, PostgreSQL status fallback when
  Redis history has expired, and WhatsApp duplicate-event suppression.

```java
@Test
void expiredRedisEventsFallBackToDurableWorkflowStatus() throws Exception {
  when(events.read(any(), any(), eq("expired"), anyInt())).thenReturn(List.of());
  when(workflows.current(TENANT_ID, CONVERSATION_ID)).thenReturn(completedSnapshot());

  mockMvc.perform(get("/api/ai/workflows/{id}/events", CONVERSATION_ID)
          .header("Last-Event-ID", "expired"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("COMPLETED"));
}
```

- [ ] **Step 2: Run test to verify it fails.**

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*AiWorkflowStatusControllerTest' --tests '*RedisAiLiveEventReaderTest'
```

Expected: no AI stream/status endpoint exists.

- [ ] **Step 3: Write the minimal implementation.** Add a tenant-scoped
  stream reader, SSE endpoint using `TEXT_EVENT_STREAM`, optional WebSocket
  adapter only if the existing web stack already supports it, and fallback to
  PostgreSQL when the requested Redis event ID is unavailable. Project durable
  transitions to Redis only after the PostgreSQL state is written. Update
  WhatsApp to call the generic workflow use case and send only the persisted
  final response.

- [ ] **Step 4: Run test to verify it passes.** Run focused controller/Redis/
  WhatsApp tests and existing conversation web tests. Expected: all pass.

- [ ] **Step 5: Refactor and commit.** Bound stream replay and retention,
  avoid token-level streaming, and commit:

```shell
git add modules/assistant
git commit -m "feat(assistant): add recoverable AI workflow events"
```

### Task 7: RAG lifecycle and Apache AGE projection reconciliation

**Files:**

- Create: `modules/documents/src/main/java/com/emme/documents/api/event/KnowledgeDocumentChanged.java`
- Create: `modules/documents/src/main/java/com/emme/documents/application/port/out/KnowledgeEmbeddingIndexer.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/GraphProjectionScheduler.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/graph/AgeGraphProjectionListener.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/graph/CatalogGraphProjectionMapper.java`
- Create: `modules/catalog/src/main/java/com/emme/catalog/api/event/CatalogItemChanged.java`
- Create: `modules/catalog/src/main/java/com/emme/catalog/api/event/CatalogItemDeleted.java`
- Create: `modules/services/src/main/java/com/emme/services/api/event/ServiceCatalogEntryChanged.java`
- Create: `modules/services/src/main/java/com/emme/services/api/event/ServiceCatalogEntryRetired.java`
- Modify: `modules/catalog/src/main/java/com/emme/catalog/application/service/CreateCatalogItemService.java`
- Modify: `modules/catalog/src/main/java/com/emme/catalog/application/service/DeleteCatalogItemService.java`
- Modify: `modules/services/src/main/java/com/emme/services/application/service/CreateServiceCatalogEntryService.java`
- Modify: `modules/services/src/main/java/com/emme/services/application/service/UpdateServiceCatalogEntryService.java`
- Modify: `modules/services/src/main/java/com/emme/services/application/service/RetireServiceCatalogEntryService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiAgeConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/RagQueryService.java`
- Modify: `database/src/main/resources/db/emme-studio/releases/0.1.0/027-ai-knowledge-index.sql`
- Modify: `database/src/main/resources/db/emme-studio/changelog.yaml`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/graph/AgeGraphProjectionListenerTest.java`
- Test: `modules/catalog/src/test/java/com/emme/catalog/application/service/CatalogChangeEventTest.java`
- Test: `modules/services/src/test/java/com/emme/services/application/service/ServiceCatalogChangeEventTest.java`
- Test: `modules/documents/src/test/java/com/emme/documents/application/service/KnowledgeEmbeddingIndexerTest.java`
- Integration test: `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/adapter/out/graph/AgeGraphProjectionRecoveryIntegrationTest.java`

**Interfaces:**

```java
public interface GraphProjectionScheduler {
  void schedule(UUID tenantId, GraphProjection projection, String sourceVersion);
}

public interface KnowledgeEmbeddingIndexer {
  void index(KnowledgeDocument document, AiExecutionContext context);
}

public record KnowledgeDocument(
    UUID documentId,
    UUID tenantId,
    String version,
    List<String> chunks) {}
```

- Consumes: Existing tenant-scoped document search, catalog/service application
  services, AGE adapter, and Spring Modulith events.
- Produces: Versioned document indexing and automatic, replayable, tenant-safe
  AGE projection with freshness and reconciliation metadata.

- [ ] **Step 1: Write the failing test.** Verify catalog/service changes emit
  trusted post-commit events, document indexing preserves tenant and embedding
  version metadata, projection is idempotent, stale projection is detectable,
  and replay cannot write another tenant’s graph.

```java
@Test
void catalogChangeSchedulesOnlyItsTenantProjection() {
  listener.onCatalogChanged(eventFor(TENANT_ID, SERVICE_ID));

  verify(projector).schedule(eq(TENANT_ID), any(), eq("catalog-v1"));
  verify(projector, never()).schedule(eq(OTHER_TENANT_ID), any(), anyString());
}
```

- [ ] **Step 2: Run test to verify it fails.**

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*AgeGraphProjectionListenerTest' :modules:catalog:test --tests '*CatalogChangeEventTest' :modules:services:test --tests '*ServiceCatalogChangeEventTest'
```

Expected: projection listeners and source events do not exist.

- [ ] **Step 3: Write the minimal implementation.** Publish events after the
  source transaction, map only allowlisted graph nodes/edges, use the existing
  backend-derived graph name, update projection version/timestamp, and provide
  a replay command that reads authoritative PostgreSQL data. Add document
  embedding indexing through the existing search abstraction and reject model
  or dimension mismatch. Keep prices and transactional policies outside RAG.

- [ ] **Step 4: Run test to verify it passes.** Run focused unit tests, the
  existing AGE integration test, and pgvector document retrieval tests.

- [ ] **Step 5: Refactor and commit.** Keep projection mapping separate from
  AGE JDBC syntax, document source freshness, and commit:

```shell
git add modules/assistant modules/catalog modules/services modules/documents database/src/main/resources/db/emme-studio
git commit -m "feat(ai): add replayable RAG and AGE projections"
```

### Task 8: Evaluation transport, versioned indexes, shadow, and canary promotion

**Files:**

- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/learning/SemanticIndexVersion.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/learning/IndexPromotionRequest.java`
- Create: `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/SemanticIndexPromotionStore.java`
- Create: `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/JdbcSemanticIndexPromotionStore.java`
- Create: `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/SemanticIndexBuildWorker.java`
- Create: `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/SemanticIndexPromotionWorker.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/event/SemanticEvaluationExportAdapter.java`
- Create: `tools/ai-evaluation/src/emme_ai_evaluation/export.py`
- Modify: `tools/ai-evaluation/src/emme_ai_evaluation/pipeline.py`
- Modify: `tools/ai-evaluation/src/emme_ai_evaluation/contracts.py`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/LearningCandidateLifecycleService.java`
- Modify: `database/src/main/resources/db/emme-studio/releases/0.1.0/028-ai-semantic-index-versions.sql`
- Modify: `database/src/main/resources/db/emme-studio/changelog.yaml`
- Test: `modules/ai-platform/src/test/java/com/emme/ai/platform/learning/SemanticIndexPromotionWorkerTest.java`
- Test: `modules/ai-platform/src/test/java/com/emme/ai/platform/learning/JdbcSemanticIndexPromotionStoreTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/event/SemanticEvaluationExportAdapterTest.java`
- Test: `tools/ai-evaluation/tests/test_export.py`
- Test: `tools/ai-evaluation/tests/test_pipeline.py`
- Integration test: `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/learning/SemanticIndexPromotionIntegrationTest.java`

**Interfaces:**

```java
public interface SemanticIndexPromotionStore {
  SemanticIndexVersion createShadow(IndexPromotionRequest request, AiExecutionContext context);
  void recordEvaluation(UUID indexId, EvaluationGates gates, AiExecutionContext context);
  void promoteCanary(UUID indexId, AiExecutionContext context);
  void rollback(UUID indexId, AiExecutionContext context);
  Optional<SemanticIndexVersion> active(String scope, AiExecutionContext context);
}

public record EvaluationGates(
    boolean datasetComplete,
    boolean safetyPassed,
    boolean regressionPassed,
    boolean shadowComparisonPassed,
    boolean canaryPassed) {}
```

- Consumes: Existing learning candidate lifecycle, Ragas JSONL pipeline,
  pgvector semantic reference tables, and durable evaluation events.
- Produces: Asynchronous trace export, versioned shadow/canary indexes, active
  pointer, rollback pointer, and deterministic promotion/audit gates.

- [ ] **Step 1: Write the failing test.** Verify candidate traces are exported
  only after redaction, an evaluation report cannot promote without regression,
  safety, shadow, and canary gates, concurrent promotion is rejected, and
  rollback restores the previous active pointer.

```java
@Test
void cannotPromoteWithoutCanaryEvidence() {
  assertThatThrownBy(() -> worker.promote(indexWithoutCanaryEvidence()))
      .isInstanceOf(IllegalStateException.class);
}

@Test
void rollbackRestoresPreviousActiveIndex() {
  worker.promote(indexWithAllPassingGates());
  worker.rollback(indexId);

  assertThat(store.activeIndex()).isEqualTo(previousIndexId);
}
```

- [ ] **Step 2: Run test to verify it fails.**

```shell
mise exec java@25.0.2 -- ./gradlew :modules:ai-platform:test --tests '*SemanticIndexPromotionWorkerTest' :modules:assistant:test --tests '*SemanticEvaluationExportAdapterTest'
cd tools/ai-evaluation && python -m unittest tests.test_export tests.test_pipeline
```

Expected: no index-version store or export path exists.

- [ ] **Step 3: Write the minimal implementation.** Export only redacted,
  anonymized traces with trusted IDs. Build inactive indexes with the active
  embedding model/version, compare against the labeled regression dataset,
  execute shadow evaluation, select a configured canary scope, and update an
  active pointer transactionally only when every required gate passes. Store
  the previous pointer for rollback. Never mutate active rows in place.

- [ ] **Step 4: Run test to verify it passes.** Run Java and Python focused
  tests, pgvector integration tests, and existing candidate lifecycle tests.

- [ ] **Step 5: Refactor and commit.** Keep Python evaluation advisory and Java
  lifecycle authoritative. Commit:

```shell
git add libraries/ai-contracts modules/ai-platform modules/assistant tools/ai-evaluation database/src/main/resources/db/emme-studio
git commit -m "feat(ai): add governed semantic index promotion"
```

### Task 9: Input/output guardrails and authenticated MCP boundary

**Files:**

- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/guardrail/GuardrailDecision.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/guardrail/GuardrailPort.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/mcp/McpToolClient.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/PromptSafetyPort.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/ResponseSafetyPort.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/AiGuardrailService.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/security/DeterministicPromptSafetyAdapter.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/security/ResponseEgressValidationAdapter.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/mcp/SpringAiMcpToolClient.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiMcpConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ProcessConversationService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiChatClientAdapter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/tool/AuthorizedAiToolGateway.java`
- Modify: `modules/assistant/build.gradle.kts`
- Test: `libraries/ai-contracts/src/test/java/com/emme/ai/contracts/GuardrailContractTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/AiGuardrailServiceTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/mcp/SpringAiMcpToolClientTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/tool/AuthorizedAiToolGatewaySecurityTest.java`

**Interfaces:**

```java
public interface GuardrailPort {
  GuardrailDecision inspect(String content, GuardrailPhase phase, AiExecutionContext context);
}

public enum GuardrailPhase {
  INPUT, RETRIEVAL, OUTPUT
}

public interface McpToolClient {
  ToolResult execute(ToolExecutionRequest request, ToolExecutionContext context);
}
```

- Consumes: Existing tenant/security advisors, redactor, authorized tool
  gateway, and Spring AI dependency management.
- Produces: Deterministic input screening, structured-output/egress checks,
  and an authenticated external MCP adapter that cannot receive arbitrary
  tenant or role values from the model.

- [ ] **Step 1: Write the failing test.** Verify prompt-injection patterns are
  rejected or escalated, retrieved instructions are treated as data, responses
  containing secrets or unsupported transaction values are rejected, MCP calls
  without trusted context are denied, and only policy-allowed tools are
  visible.

```java
@Test
void rejectsMcpCallWithoutBackendTenantContext() {
  assertThatThrownBy(() -> client.execute(request, missingContext))
      .isInstanceOf(SecurityException.class);
}

@Test
void rejectsResponseContainingInternalSecret() {
  assertThat(guardrails.inspect("api_key=secret", GuardrailPhase.OUTPUT, context).allowed())
      .isFalse();
}
```

- [ ] **Step 2: Run test to verify it fails.**

```shell
mise exec java@25.0.2 -- ./gradlew :libraries:ai-contracts:test --tests '*GuardrailContractTest' :modules:assistant:test --tests '*AiGuardrailServiceTest' --tests '*SpringAiMcpToolClientTest'
```

Expected: guardrail and MCP ports are absent.

- [ ] **Step 3: Write the minimal implementation.** Add length/content-type/
  control-character checks, deterministic injection screening, untrusted
  content delimiters, typed output validation, secret/tenant-leak detection,
  and application-result reconciliation. Add the Spring AI MCP client using
  the Spring AI BOM-managed MCP client artifact, configure endpoint/auth
  properties, and route calls through `AuthorizedAiToolGateway`. Do not expose
  MCP to the public HTTP surface without authentication and role policy.

- [ ] **Step 4: Run test to verify it passes.** Run focused guardrail/MCP tests,
  existing advisor/tool tests, and tenant-isolation tests. Expected: all pass.

- [ ] **Step 5: Refactor and commit.** Keep safety decisions framework-neutral,
  keep MCP transport in an adapter, and commit:

```shell
git add libraries/ai-contracts modules/assistant
git commit -m "feat(ai): add governed guardrails and MCP boundary"
```

### Task 10: Observability, dashboards, release verification, and documentation

**Files:**

- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/observability/AiOpenTelemetryConfiguration.java`
- Create: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/observability/AiOpenTelemetryConfigurationTest.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/observability/MicrometerAiWorkflowObserver.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/trace/AiTraceRedactor.java`
- Modify: `infra/kubernetes/base/grafana-dashboard.yaml`
- Modify: `infra/kubernetes/base/prometheus-alerts.yaml`
- Modify: `deployment/compose/compose.observability.yaml`
- Modify: `deployment/compose/compose.environment-e2e.yaml`
- Modify: `deployment/compose/compose.environment-regression.yaml`
- Modify: `deployment/helm/emme/templates/deployment.yaml`
- Modify: `deployment/helm/emme/values.yaml`
- Create: `deployment/compose/compose.ai-platform.contract.test.mjs`
- Create: `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/AiPlatformEndToEndIntegrationTest.java`
- Create: `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/TenantIsolationFailureIntegrationTest.java`
- Modify: `docs/ai-platform/implementation-plan.md`
- Modify: `docs/ai-platform/operational-runbook.md`
- Modify: `docs/ai-platform/README.md`
- Modify: `docs/ai-platform/technical-specification.md`
- Create: `docs/ai-platform/examples/quote-request.json`
- Create: `docs/ai-platform/examples/quote-review-response.json`
- Create: `docs/ai-platform/examples/multi-intent-booking.json`

**Interfaces:**

```java
public interface AiWorkflowObserver {
  void workflowStarted(String workflowType);
  void workflowFinished(String workflowType, String outcome, Duration duration);
  void modelAttempt(AiModelExecutionTrace trace);
  void toolAttempt(AiToolCallTrace trace);
}
```

- Consumes: All previous tasks, existing Micrometer observer, redacted durable
  traces, deployment overlays, and runbook.
- Produces: End-to-end evidence for tenant isolation, model backpressure,
  workflow resume, Redis/PostgreSQL failure behavior, OTel deployment, and
  operational alerting.

- [ ] **Step 1: Write the failing test.** Add contract tests for OTel agent
  configuration, bounded metric labels, required dashboard panels, alert rules,
  image/quote/booking end-to-end behavior, restart/resume, wrong-tenant access,
  provider timeout, MCP failure, Redis outage, and AGE fallback.

```java
@Test
void observerDoesNotUseTenantOrWorkflowAsMetricLabels() {
  observer.workflowFinished("DESIGN_QUOTE", "SUCCESS", Duration.ofMillis(20));

  assertThat(registry.get("emme_ai_workflow_duration").tags())
      .noneMatch(tag -> Set.of("tenantId", "workflowId", "conversationId").contains(tag.getKey()));
}
```

- [ ] **Step 2: Run test to verify it fails.**

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*AiOpenTelemetryConfigurationTest' :modules:assistant:integrationTest --tests '*AiPlatformEndToEndIntegrationTest'
node deployment/compose/compose.ai-platform.contract.test.mjs
```

Expected: missing OTel deployment and end-to-end contract coverage.

- [ ] **Step 3: Write the minimal implementation.** Add OTel JVM-agent
  configuration to JVM deployment profiles, retain durable redacted payload
  traces, add bounded metrics and required dashboards/alerts, and implement
  environment-dependent integration tests with mock/local providers. Add
  explicit feature flags for generic workflows, mutations, AGE projection,
  MCP, and index promotion. Update the runbook with startup, outage, replay,
  dead-letter, graph, cache, provider, and rollback procedures.

- [ ] **Step 4: Run test to verify it passes.** Execute the complete verification
  matrix using Java 25:

```shell
mise run toolchain:jvm
mise exec java@25.0.2 -- ./gradlew spotlessCheck
mise exec java@25.0.2 -- ./gradlew :libraries:ai-contracts:test :modules:ai-platform:test :modules:assistant:test
mise exec java@25.0.2 -- ./gradlew :database:test :modules:assistant:integrationTest
node deployment/compose/compose.e2e.contract.test.mjs
node deployment/compose/compose.kafka.contract.test.mjs
node deployment/compose/compose.age.contract.test.mjs
node deployment/compose/compose.ai-platform.contract.test.mjs
cd tools/ai-evaluation && python -m unittest discover -s tests -v
```

Expected: all focused/unit/integration/contract tests pass. Existing unrelated
architecture and Markdown failures are recorded separately with their exact
failure output; no AI failure is hidden or reclassified.

- [ ] **Step 5: Refactor and commit.** Update Mermaid diagrams, examples,
  implementation status, runbook, and rollback evidence. Commit:

```shell
git add modules/assistant infra deployment docs/ai-platform
git commit -m "chore(ai): close platform observability and release gates"
```

## Definition of Done

- [ ] The generic conversation workflow is reachable from authenticated web and
  WhatsApp boundaries and persists conversation messages.
- [ ] Quote submission accepts a secure tenant-scoped image reference and
  reaches extraction, deterministic calculation, and HITL review.
- [ ] Appointment create, cancel, and reschedule tools delegate to
  actor-aware application use cases and pass authorization, confirmation,
  ownership, idempotency, and audit tests.
- [ ] PostgreSQL/Spring Modulith provides durable asynchronous job delivery;
  worker retry, fairness, bounded capacity, and dead-letter evidence exists.
- [ ] Redis provides only temporary state/live events/cache and PostgreSQL
  recovery works after Redis event expiry or outage.
- [ ] SSE or the existing supported live transport replays events and falls
  back to durable workflow status.
- [ ] RAG is tenant-filtered and versioned; prices and transactional decisions
  remain outside RAG.
- [ ] AGE projection is tenant-scoped, curated, replayable, freshness-tracked,
  and falls back safely to PostgreSQL/pgvector.
- [ ] Learning candidates are redacted, asynchronously evaluated, built into
  inactive indexes, shadow-tested, canaried, promoted by pointer, and
  rollback-capable.
- [ ] Prompt/output guardrails and authenticated MCP are present and tested.
- [ ] Durable traces, OTel spans, bounded metrics, dashboards, alerts, and
  runbooks are present.
- [ ] Java 25 verification, formatting, compilation, unit, integration,
  contract, E2E, failure, and evaluation checks are recorded.
- [ ] Every implementation task is committed and pushed without staging the
  unrelated identity/subscriptions/tenancy worktree changes.

## Known External Blockers to Track Separately

- The current repository has unrelated Modulith/layer architecture failures in
  identity/tenancy/subscriptions and two existing adapter placement checks.
- The current Markdown validator reports unrelated pre-existing failures in
  legacy superpowers documents and the vendored Ragas README.
- The native-image lane requires a GraalVM Native Image 25 environment.
- The default shell currently resolves Java 17; all Gradle hooks and release
  commands must use the repository’s Java 25 `mise` toolchain.
