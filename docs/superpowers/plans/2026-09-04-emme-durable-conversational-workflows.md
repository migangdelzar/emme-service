# Emme Durable Conversational Workflows Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Each task follows test-first red → green → refactor and uses checkbox tracking.

**Goal:** Implement the approved durable-conversation design with guarded Spring AI model paths, one versioned embedding capability shared by RAG/tools/cache, score-aware self-improving retrieval, and typed LangGraph4j workflows for consequential conversations.

**Architecture:** Simple read-only requests use the deterministic application router and Spring AI direct path. Semantic tools, intent routing, cache, and RAG use one embedding-first fast path with route-specific thresholds and explicit abstention. Appointment, payment, staff, and other multi-turn flows use typed LangGraph4j subgraphs with PostgreSQL checkpoints; Emme application services remain authoritative for authorization and business mutations.

**Tech Stack:** Java 25, Spring Boot 4.1.0, Spring AI 2.0.1, LangGraph4j 1.8.25 core, Gradle 9.4.1, PostgreSQL/pgvector, Redis 8, Spring Modulith, Micrometer/OpenTelemetry, JUnit 5, Mockito, AssertJ, ArchUnit, Testcontainers, and the existing Python 3.13 Ragas evaluation scaffold.

## Global Constraints

- Every behavioral change follows red → green → refactor with a focused test written before production code.
- `libraries:ai-contracts` remains free of Spring, LangGraph4j, Redis, JDBC, provider SDK, and Emme business-rule dependencies.
- `EmbeddingService` is the one cross-module embedding capability; its result carries the model-space identity needed by vectors, cache, and retrieval.
- PostgreSQL is authoritative for conversation history, workflow state, checkpoints, holds, payment correlation, cache entries, traces, and evaluation evidence; Redis is a rebuildable projection.
- Embeddings select candidates only. Deterministic score/margin/policy gates decide whether a shortcut is accepted.
- Model output never authorizes or directly performs appointment, payment, refund, tenant, role, or staff-review mutations.
- Input, context, tool, output, grounding, and delivery guardrails fail closed and cannot be weakened by model text or tool arguments.
- Low retrieval relevance is not provider unavailability. Query improvement is bounded and observable; ungrounded factual fallback is prohibited.
- Embedding-provider failover is allowed only for explicit provider-unavailable failures. Invalid dimensions, model-version mismatches, authorization failures, and policy failures remain errors.
- Semantic tool routing is limited to backend-authorized read-only tools. Mutation tools run through typed workflows after confirmation and idempotency checks.
- No `langgraph4j-spring-ai` runtime dependency is added in this plan. LangGraph4j core owns workflow durability; Spring AI owns model, tool, embedding, and RAG mechanics behind adapters.
- Internal domain events remain Spring Modulith events in the initial runtime. Workflow completion,
  notification, and calendar integration must use the PostgreSQL-backed Modulith publication
  registry; they must not activate Kafka, require broker configuration, or import Kafka types.
  Kafka becomes eligible only when an independently deployed consumer and its versioned delivery
  contract are approved.
- Existing clear names remain preferred: `BookAppointmentService`, `CancelAuthorizedAppointmentService`, and `RescheduleAuthorizedAppointmentService`. New names are short, noun-based, and describe one responsibility.
- Focused module checks run after each task. Phase-level integration checks run after each phase. The final enterprise gate runs after all implementation work.
- Existing unrelated worktree changes are preserved and never staged with a task unless that task owns them.

### Naming rules

- Use one short noun or noun phrase that states the responsibility: `QueryImprover`,
  `RetrievalQualityGate`, `InputGuard`, `NodePolicyRegistry`, and
  `AppointmentBookingWorkflow`.
- Keep established names when they already state the responsibility clearly, including
  `BookAppointmentService`, `CancelAuthorizedAppointmentService`, and
  `RescheduleAuthorizedAppointmentService`.
- Do not introduce generic suffixes such as `Manager`, `Helper`, `Processor`, `Handler`, or
  `Orchestrator` when a precise responsibility name is available. `Workflow` is reserved for
  a durable state machine; `Service` is reserved for an application use case; `Adapter` is
  reserved for a framework or external boundary.
- Rename an existing class only when its current name hides its responsibility or causes a
  duplicate concept. A rename must be accompanied by a caller inventory, focused tests, and a
  compatibility decision in the same task.

---

## 0. Separate-session handoff

The next session must start from the branch state, not from an assumed clean baseline. The
coordinator uses Luna High when that runtime option is available; spawned implementation
workers use Luna Medium. If model selection is unavailable, keep the same coordinator/worker
responsibility split and do not change the technical plan.

- [ ] Check out and synchronize the feature branch:

  ```bash
  git checkout feat/ai-platform-foundation
  git pull --ff-only origin feat/ai-platform-foundation
  git status --short
  git log --oneline --decorate -8
  ```

- [ ] Read this plan, the approved design, and the completed simplification record before
  selecting a task:

  ```bash
  sed -n '1,220p' docs/superpowers/plans/2026-09-04-emme-durable-conversational-workflows.md
  sed -n '1,260p' docs/superpowers/specs/2026-09-04-emme-durable-conversational-workflows-design.md
  sed -n '1,220p' docs/superpowers/plans/2026-09-03-ai-platform-simplification.md
  ```

- [ ] Re-run the caller inventory for the selected task and compare it with the task's file
  map. If the branch has already completed a task, mark it complete using its commit and
  verification evidence instead of reimplementing it.

- [ ] Keep one task in progress at a time. For each task, write the failing test, run the
  smallest focused test, implement the smallest production change, run focused compile/test
  and `spotlessCheck`, refactor, and commit only that task's files. Do not run `spotlessApply`
  during every slice; run it once before the final commit only if the final gate requires it.

- [ ] Do not run the expensive enterprise gate while a phase is still being developed. Run
  focused module checks during the phase, phase-level integration checks at the phase boundary,
  and the complete enterprise gate only after Task 13's compatibility cleanup is complete.

## 1. Scope and Current Baseline

This plan extends the completed simplification work recorded in
`docs/superpowers/plans/2026-09-03-ai-platform-simplification.md`; it does not repeat its
completed provider, tenancy, checkpoint, or response-ownership migrations.

The current repository already provides:

| Capability | Current implementation | Remaining implementation in this plan |
|---|---|---|
| Embedding providers | `EmbeddingModelSelector` orders provider attempts and `SpringAiEmbeddingConfiguration` wires named Spring AI models | Converge cross-module consumers on one versioned `EmbeddingService` result and retain compatibility only during migration |
| Semantic tools | `SemanticProactiveToolRouter` embeds first, filters authorized keys, and applies top-score/margin policy | Reuse the normalized turn embedding, make abstention explicit, and prevent model tool selection for accepted known read-only tools |
| Semantic cache | `SemanticChatCache` embeds before hot/durable lookup and falls through to `ChatService` on miss/failure | Attach shared embedding identity and output guard checks to the fast path |
| RAG | `DocumentKnowledgeRetrievalAdapter` embeds before document search; optional Spring AI RAG uses `RetrievalAugmentationAdvisor` | Preserve document search scores, apply `RetrievalQualityGate`, and run bounded query improvement before answer generation |
| Conversation workflow | `ConversationWorkflowGraph` and quote workflow support checkpointing and interruption | Add per-node model/tool/memory policy, guardrail projections, and appointment/payment subgraphs |
| Guardrails | Context, tool authorization, and response non-blank checks exist in separate boundaries | Add typed layered input, context, output, grounding, and delivery policies |

## 2. Scope, flow coverage, and budget

The deterministic application router selects the smallest safe path. Only flows that need
multi-turn state, approval, external callbacks, or compensating actions enter LangGraph4j.

| Flow | Entry decision | Durable subgraph | Interrupt/resume point | Authoritative side effects |
|---|---|---|---|---|
| Informational chat | Direct route or high-confidence semantic cache/tool shortcut | None; Spring AI direct path | None | None; cache is an optimization |
| Tenant knowledge answer | RAG route with accepted retrieval quality | `KnowledgeAnswerService` bounded retrieval subflow | Clarification or `NO_ANSWER` when quality remains below policy | None; sources remain tenant-scoped |
| New appointment without payment | Booking intent with no payment policy | `AppointmentBookingWorkflow` | Customer confirmation before hold/book | Appointment use cases and committed appointment events |
| New appointment with payment | Booking intent requiring prepayment | `AppointmentBookingWorkflow` → `AppointmentPaymentWorkflow` | Customer confirmation, then `WAITING_FOR_PAYMENT` until verified provider callback | Hold, payment link, verified capture, appointment confirmation |
| Reschedule | Owned appointment plus target slot | `AppointmentRescheduleWorkflow` | Confirmation before mutation; resume after restart | Authorized reschedule, refund/charge policy, events |
| Cancel/refund | Owned appointment plus cancellation policy | `AppointmentCancellationWorkflow` | Confirmation or staff approval; resume for provider outcome | Authorized cancellation, refund, events |
| Staff review/escalation | Role/policy requires human decision | Existing quote/review graph extended by node profiles | `WAITING_FOR_APPROVAL` with reviewer identity | Review audit and approved application use case |
| Notification/calendar | Committed appointment/payment event | Event consumers, not model graph nodes | Retry from event publication/outbox state | Notification delivery and calendar synchronization |

### Model and embedding budget

The plan does not add a supervisor model. Model calls are attached to the smallest
responsibility that needs them and are bounded per operation.

| Path | Embedding work | Chat/model work | Shortcut rule |
|---|---|---|---|
| Direct informational chat | None unless semantic cache/tool routing is enabled | One answer-model call on cache/tool abstention | A semantic hit returns without a chat-model call |
| Semantic tool or cache | One shared turn embedding and candidate lookup | Zero on accepted read-only tool/cache; one normal chat call on miss | Authorization and deterministic score/margin policy precede acceptance |
| RAG | One embedding per original or bounded improved query variant | One answer-model call only after an accepted retrieval context; at most one bounded rewrite-model call when configured | Retrieval-quality rejection never becomes an ungrounded answer |
| Durable workflow | Embeddings only in explicitly configured retrieval/semantic nodes | A node may use only its registered `NodeModelRole`; no global model or tool registry | Deterministic validation, authorization, mutation, and delivery nodes use no model |

The query-improvement ladder runs deterministic normalization and bounded variants before any
rewrite-model call. Each variant has its own embedding and quality decision, while the whole
request shares one deadline and maximum-attempt budget. This keeps embedding-first behavior
useful for latency and cost without treating similarity as authorization or truth.

### File Map

| Area | Create | Modify | Test |
|---|---|---|---|
| Embedding contract | None; reuse `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/semantic/EmbeddingVector.java` and `EmbeddingModelVersion.java` | `EmbeddingService.java`, `AiEmbeddingAdapter.java`, `SpringAiEmbeddingModel.java`, selector/adapters, catalog consumer, assistant semantic consumers | Contract, platform, catalog, and assistant embedding tests |
| Retrieval score | None | `DocumentChunkDetails.java`, `DocumentApplicationMapper.java`, `SearchDocumentChunksService.java`, `DocumentKnowledgeRetrievalAdapter.java` | Documents search service and assistant RAG adapter tests |
| Semantic fast path | `SemanticQuery.java`, `SemanticQueryFactory.java` | `ChatService.java`, `ProactiveToolRouter.java`, `SemanticResponseCache.java`, `SemanticProactiveToolRouter.java`, `SemanticChatCache.java` | No-duplicate-embedding, tool abstention, cache miss, and provider fallback tests |
| RAG quality | `RetrievalQualityPolicy.java`, `RetrievalQualityDecision.java`, `RetrievalQualityGate.java`, `QueryImprovementPolicy.java`, `QueryImprover.java` | `SpringAiRagProperties.java`, `RagQueryService.java`, RAG configuration and adapters | Threshold, margin, support, freshness, retry-budget, and no-answer tests |
| Guardrails | `GuardrailAction.java`, `GuardrailDecision.java`, typed boundary requests, `InputGuard.java`, `ContextGuard.java`, `ToolGuard.java`, `OutputGuard.java`, `GroundingGuard.java`, `DeliveryGuard.java`, `GuardrailPipeline.java` | Spring AI advisors, `ProcessConversationService.java`, `ChatService.java`, `SpringAiToolCallbackProvider.java`, RAG answer path, channel delivery | Guardrail unit, advisor, tool, output, delivery, and workflow tests |
| Node policy | `NodeModelRole.java`, `NodeProfile.java`, `NodeMemoryPolicy.java`, `NodeToolPolicy.java`, `NodeGuardrailPolicy.java`, `NodeContext.java`, `NodeResult.java`, `NodePolicyRegistry.java` | `ConversationWorkflowCapabilities.java`, `ConversationWorkflowGraph.java`, LangGraph adapter/configuration | Projection, allow-list, guardrail, interruption, timeout, and state-boundary tests |
| Appointment/payment workflows | `AppointmentHold.java`, `PaymentLink.java`, workflow event contracts, `AppointmentBookingWorkflow.java`, `AppointmentRescheduleWorkflow.java`, `AppointmentCancellationWorkflow.java`, `AppointmentPaymentWorkflow.java` | Appointment/payment application ports and assistant workflow composition | Workflow transition, collision, webhook, refund, restart, and idempotency tests |
| Durable persistence | `035-appointment-holds.sql`, `036-ai-payment-workflow.sql`, and `037-ai-workflow-correlations.sql` under `database/src/main/resources/db/emme-studio/releases/0.1.0/` | `database/src/main/resources/db/emme-studio/changelog.yaml`, JDBC adapters, and migration contract tests | Schema, RLS, index, rollback-shape, and integration tests |
| Learning/operations | None; extend existing learning/evaluation records only when the bounded evidence fields require it | `AiSemanticExecutionTrace.java`, `JdbcAiTraceRecorder.java`, `MicrometerSemanticMetrics.java`, `tools/ai-evaluation/src/emme_ai_evaluation/contracts.py`, `pipeline.py`, and `redaction.py` | Redaction, metric-cardinality, evaluator, and promotion-gate tests |

## 3. Execution Order

```text
Phase A — contracts and evidence
  Task 1: versioned embeddings
  Task 2: retrieval score preservation

Phase B — semantic fast paths and RAG
  Task 3: shared turn embedding
  Task 4: retrieval quality gate
  Task 5: bounded query improvement
  Task 6: embedding-first tools and semantic cache

Phase C — guardrails and Spring AI composition
  Task 7: typed guardrail contracts
  Task 8: guardrail enforcement and grounded answer composition

Phase D — durable workflows
  Task 9: node profiles and typed state projections
  Task 10: booking, hold, and payment workflow
  Task 11: reschedule, cancellation, staff, notification, and calendar workflows

Phase E — evidence, cleanup, and release gate
  Task 12: observability, offline evaluation, and controlled improvement
  Task 13: compatibility cleanup and final enterprise validation
```

Tasks within a phase are sequential where contracts or state shape are shared. Tests
that use only fakes may run in parallel after their contract task is complete. PostgreSQL,
Testcontainers, startup, webhook, and E2E checks remain phase-level or final checks.

Phase checkpoints are explicit:

| Checkpoint | Required evidence |
|---|---|
| After Phase A | `:libraries:ai-contracts:test`, `:modules:ai-platform:test`, `:modules:documents:test`, `:modules:assistant:test`, and `:modules:catalog:test` focused suites pass; score and model identity are present at the module boundaries |
| After Phase B | Assistant semantic and RAG unit tests plus `:modules:assistant:integrationTest` semantic/RAG tests pass; accepted shortcuts and grounded retrieval are measured separately from provider failures |
| After Phase C | Guardrail, advisor, controller, chat, tool, and RAG configuration tests pass; startup verifies advisor ordering and fail-closed behavior |
| After Phase D | Appointment, payment, assistant workflow, notification, and calendar integration tests pass with PostgreSQL/Testcontainers; restart, webhook replay, hold expiry, and tenant isolation are evidenced |
| Before Phase E closure | Offline evaluation fixture set is non-empty, redaction and bounded metric-label tests pass, and no production threshold or prompt is auto-promoted |

### Task dependency map

```text
Task 1 → Task 2 → Task 3 → Task 4 → Task 5
                         ↘ Task 6
Task 7 → Task 8
Task 8 + Task 5 → Task 9
Task 9 + existing appointment/payment services → Task 10
Task 10 → Task 11 → Task 12 → Task 13
```

Task 6 can begin after Task 3, but its final wiring waits for Task 7/8 when the full output
guard is required. Task 9 consumes the guardrail contracts, so it cannot be started from an
older branch snapshot that lacks Task 7. Task 10 and Task 11 must not create parallel mutation
implementations: they call existing authorized appointment/payment use cases and add only the
durable hold, callback, and graph boundaries.

Before Task 11, the active event boundary must be Modulith-first: current internal events have no
`@Externalized` metadata, asynchronous consumers use Spring Modulith listeners, and event
publication is recoverable through PostgreSQL. If the event-boundary plan is being implemented
in the same session, read and complete
`docs/superpowers/plans/2026-09-05-modulith-first-event-boundaries.md`'s event classification
and default-runtime configuration tasks before the Task 11 phase checkpoint. Do not make
workflow progress depend on Kafka availability.

## 4. Phase A — Contracts and Evidence

### Task 1: Converge on one versioned embedding capability

**Files:**

- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/embedding/EmbeddingService.java`
- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/semantic/EmbeddingVector.java`
- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/semantic/EmbeddingModelConfiguration.java`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiEmbeddingModel.java`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/capability/AiEmbeddingAdapter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/provider/EmbeddingModelSelector.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/provider/TracingEmbeddingModelPort.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcSemanticCacheAdapter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcSemanticReferenceSearchAdapter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/RedisSemanticCacheHotStore.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiEmbeddingModelAdapter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/EmbeddingModelPort.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/SemanticCachePort.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/SemanticReferenceSearchPort.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiEmbeddingProviderRegistry.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/tool/SemanticProactiveToolRouter.java`
- Modify: `modules/catalog/src/main/java/com/emme/catalog/application/service/MatchCatalogItemsService.java`
- Test: `libraries/ai-contracts/src/test/java/com/emme/ai/contracts/CanonicalAiContractsTest.java`
- Test: `modules/ai-platform/src/test/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiEmbeddingModelTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/EmbeddingModelSelectorTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/persistence/JdbcSemanticAdapterTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/RedisSemanticCacheHotStoreTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/provider/TracingEmbeddingModelPortTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiEmbeddingConfigurationTest.java`
- Test: `modules/catalog/src/test/java/com/emme/catalog/repository/CatalogRepositoryTest.java`

**Interfaces:**

The canonical cross-module service returns the existing framework-free versioned value:

```java
public interface EmbeddingService {
  EmbeddingVector embed(String text);
}

public record EmbeddingVector(
    List<Float> values,
    EmbeddingModelVersion model) {}
```

`EmbeddingModelVersion` is the canonical full identity and already carries `modelName`,
`version`, `dimension`, `distanceMetric`, and `queryInstructionVersion`. The existing
three-field `EmbeddingModelConfiguration` remains the provider/index configuration value;
it is not expanded into a second identity type. A provider result with the wrong dimension
or model identity throws before search, cache, or persistence.

- [x] **Step 1: Write the failing contract tests.** Add tests proving the service returns the vector and exact model identity, rejects a dimension mismatch, and prevents a legacy composite provider from being registered as the canonical embedding service.
- [x] **Step 2: Run the focused tests.** Run:

  ```bash
  ./gradlew :libraries:ai-contracts:test --tests '*CanonicalAiContractsTest' \
    :modules:ai-platform:test --tests '*SpringAiEmbeddingModelTest' \
    :modules:assistant:test --tests '*EmbeddingModelSelectorTest'
  ```

  Expected result: compilation or assertion failures identify the raw `List<Float>` contract and duplicate assistant vector type.
- [x] **Step 3: Implement the minimum contract migration.** Make `EmbeddingService` return the shared `EmbeddingVector`; make Spring AI and selector adapters construct that value; migrate catalog to call `.values()`; migrate assistant semantic, cache, and RAG classes to the shared vector. Keep `EmbeddingModel` and `EmbeddingModelPort` as deprecated adapters until Task 13.
- [x] **Step 4: Run the focused tests.** The contract, provider, selector, catalog, and assistant tests pass with no fabricated zero vectors and with provider-unavailable failover unchanged.
- [x] **Step 5: Refactor and commit.** Remove duplicated conversion code, retain clear names, run `git diff --check`, and commit:

  ```bash
  git add libraries/ai-contracts modules/ai-platform modules/assistant modules/catalog
  git commit -m "refactor(ai): converge on versioned embedding service"
  ```

### Task 2: Preserve document search scores across the RAG boundary

**Files:**

- Modify: `modules/documents/src/main/java/com/emme/documents/api/result/DocumentChunkDetails.java`
- Modify: `modules/documents/src/main/java/com/emme/documents/application/mapper/DocumentApplicationMapper.java`
- Modify: `modules/documents/src/main/java/com/emme/documents/application/service/SearchDocumentChunksService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/DocumentKnowledgeRetrievalAdapter.java`
- Test: `modules/documents/src/test/java/com/emme/documents/application/service/SearchDocumentChunksServiceTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/persistence/DocumentKnowledgeRetrievalAdapterTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/TenantScopedDocumentRetrieverTest.java`

**Interfaces:**

Preserve the authoritative search score in the application result:

```java
public record DocumentChunkDetails(
    UUID id,
    UUID documentId,
    int chunkIndex,
    String content,
    String contentFingerprint,
    double score) {

  public DocumentChunkDetails(
      UUID id,
      UUID documentId,
      int chunkIndex,
      String content,
      String contentFingerprint) {
    this(id, documentId, chunkIndex, content, contentFingerprint, 0.0);
  }
}
```

`SearchDocumentChunksService` maps each `DocumentSearchHit.chunkId()` to its score and
passes the score to the mapper. `DocumentKnowledgeRetrievalAdapter` maps
`DocumentChunkDetails.score()` into `RetrievedDocument.score()` instead of `0.0`.

- [ ] **Step 1: Write the failing score-preservation tests.** Assert that a search hit with score `0.92` reaches `DocumentChunkDetails.score()` and then `RetrievedDocument.score()`; assert that chunk ordering follows the ranked hit order.
- [ ] **Step 2: Run the focused tests.** Run:

  ```bash
  ./gradlew :modules:documents:test --tests '*SearchDocumentChunksServiceTest' \
    :modules:assistant:test --tests '*DocumentKnowledgeRetrievalAdapterTest' \
    --tests '*TenantScopedDocumentRetrieverTest'
  ```

  Expected result: the new score assertions fail because the current application result discards the hit score and the adapter emits `0.0`.
- [ ] **Step 3: Implement score propagation.** Carry the score map through the service, add the compatibility constructor, and map trusted score/source metadata into the shared RAG result. Reject non-finite or negative scores at the result boundary.
- [ ] **Step 4: Run tests and refactor.** Confirm score, order, tenant filtering, and untrusted metadata behavior; keep the source identifier supplied by the backend and not by document metadata.
- [ ] **Step 5: Commit.**

  ```bash
  git add modules/documents modules/assistant
  git commit -m "fix(rag): preserve document retrieval scores"
  ```

## 5. Phase B — Semantic Fast Paths and RAG

### Task 3: Reuse one normalized turn embedding

**Files:**

- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticQuery.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticQueryFactory.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ChatService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/ProactiveToolRouter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/SemanticResponseCache.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/tool/SemanticProactiveToolRouter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticChatCache.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ChatServiceTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/tool/SemanticProactiveToolRouterTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/SemanticChatCacheTest.java`

**Interfaces:**

```java
public record SemanticQuery(String text, EmbeddingVector embedding) {}

public interface SemanticQueryFactory {
  SemanticQuery create(String rawText, AiExecutionContext context);
}

public interface ProactiveToolRouter {
  Optional<AiToolResult> route(SemanticQuery query);
}

public interface SemanticResponseCache {
  Optional<String> lookup(String conversationContext, SemanticQuery query);
  Optional<UUID> store(String conversationContext, SemanticQuery query, String response);
}
```

The factory normalizes one input, validates the current backend context, and memoizes the
embedding only for the current operation and embedding namespace. It does not create a
cross-tenant persistent cache. Rewritten or expanded RAG queries are new `SemanticQuery`
values and receive new embeddings. `ProactiveToolRouter.route(SemanticQuery query)` and
`SemanticResponseCache.lookup(String conversationContext, SemanticQuery query)` consume the
prepared value directly; tools and cache remain separate responsibilities rather than being
combined behind a generic fast-path abstraction.

- [x] **Step 1: Write failing tests.** Prove that a message checked for a semantic tool and semantic cache calls the embedding service once; prove a cache miss calls normal chat; prove an empty authorized-tool set does not call embeddings.
- [x] **Step 2: Run focused tests.** Run:

  ```bash
  ./gradlew :modules:assistant:test --tests '*ChatServiceTest' \
    --tests '*SemanticProactiveToolRouterTest' --tests '*SemanticChatCacheTest'
  ```

  Expected result: the duplicate embedding invocation assertion fails and the new `SemanticQuery` type is absent.
- [x] **Step 3: Implement the shared query object and fast path.** Keep semantic cache eligibility, identity, TTL, and safety checks unchanged; pass the prepared query into tool and cache operations instead of embedding independently.
- [x] **Step 4: Refactor and verify.** Confirm semantic failures still produce safe misses/abstentions, security failures still propagate, and normal chat remains the fallback after a miss.
- [x] **Step 5: Commit.**

  ```bash
  git add modules/assistant
  git commit -m "refactor(assistant): reuse turn embeddings in semantic paths"
  ```

### Task 4: Add deterministic retrieval-quality gating

**Files:**

- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/rag/KnowledgeRoute.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/rag/RetrievalQualityPolicy.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/rag/RetrievalQualityDecision.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/rag/RetrievalQualityGate.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiRagProperties.java`
- Modify: `applications/emme-platform/src/main/resources/application.yml`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/rag/RetrievalQualityGateTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiRagPropertiesTest.java`

**Interfaces:**

```java
public enum KnowledgeRoute { FAQ, POLICY, DESIGN, GENERAL }

public record RetrievalQualityPolicy(
    double minimumTopScore,
    double minimumMargin,
    int minimumSupportingDocuments,
    Duration maximumDocumentAge,
    boolean requireLexicalAgreement) {}

public interface RetrievalQualityGate {
  RetrievalQualityDecision evaluate(
      KnowledgeRoute route,
      String query,
      List<RetrievedDocument> documents,
      RetrievalQualityPolicy policy);
}
```

`RetrievalQualityDecision` contains `accepted`, `topScore`, `secondScore`, `margin`,
`supportingDocumentCount`, `freshDocumentCount`, `lexicalAgreement`, and a bounded
`reasonCode`. It never stores document text. Scores are compared using the vector-store
similarity scale used by the configured index. Policies are route-specific and calibrated
from evaluation data rather than copied from one universal threshold.

- [x] **Step 1: Write failing tests.** Cover empty results, one result without margin, top score below threshold, insufficient independent support, stale documents, lexical disagreement, and an accepted result with finite scores.
- [x] **Step 2: Run the focused test.** Run `./gradlew :modules:assistant:test --tests '*RetrievalQualityGateTest'`; expected failure is the missing gate and policy.
- [x] **Step 3: Implement the deterministic gate.** Sort by score, inspect only bounded metadata fields such as source type and effective date, calculate top-two margin and support count, and return a reason code for every rejection.
- [x] **Step 4: Add typed configuration.** Add explicit per-route settings under `app.ai.spring-rag.quality`, enforce finite ranges and positive limits, and preserve the existing retrieval-limit default.
- [x] **Step 5: Run, refactor, and commit.**

  ```bash
  ./gradlew :modules:assistant:test --tests '*RetrievalQualityGateTest' \
    --tests '*SpringAiRagPropertiesTest'
  git add modules/assistant applications/emme-platform/src/main/resources/application.yml
  git commit -m "feat(rag): add retrieval quality gates"
  ```

### Task 5: Add bounded query improvement before grounded generation

**Files:**

- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/rag/QueryImprovementPolicy.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/rag/QueryImprover.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/rag/KnowledgeAnswerService.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/rag/GroundedAnswer.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiQueryImprover.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/RagQueryService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/RagAnswerPort.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiRagConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/TenantScopedDocumentRetriever.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/rag/KnowledgeAnswerServiceTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiQueryImproverTest.java`
- Integration test: `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/RagQualityIntegrationTest.java`

**Interfaces:**

```java
public record QueryImprovementPolicy(
    int maximumAttempts,
    int maximumVariants,
    int maximumQueryCharacters,
    Duration maximumDuration,
    boolean allowCompression,
    boolean allowRewrite,
    boolean allowTranslation,
    boolean allowExpansion) {}

public interface QueryImprover {
  List<String> improve(
      String originalQuery,
      KnowledgeRoute route,
      RetrievalQualityDecision previous,
      AiExecutionContext context,
      QueryImprovementPolicy policy);
}

public interface KnowledgeAnswerService {
  GroundedAnswer answer(KnowledgeQuery query, KnowledgeRoute route, AiExecutionContext context);
}

public interface RagAnswerPort {
  GroundedAnswer answer(KnowledgeQuery query, AiExecutionContext context);
}

public record GroundedAnswer(
    String text,
    KnowledgeRoute route,
    RetrievalQualityDecision retrieval,
    boolean grounded) {}
```

The service attempts original query → compression for follow-ups → rewrite → translation
when the index language requires it → bounded multi-query expansion. Every attempt passes
the same tenant/source filter and `RetrievalQualityGate`. It stops on the first accepted
context. If the budget is exhausted it returns `NO_ANSWER` or `CLARIFY`; it does not call
the answer model with rejected tenant knowledge. Spring AI's low-temperature query
transformers and document post-processors remain inside `SpringAiQueryImprover` and the
RAG adapter; the application service owns loop limits and route policy.

- [x] **Step 1: Write failing tests.** Verify accepted first retrieval uses one embedding and one answer call; low confidence triggers at most `maximumAttempts`; a successful rewrite stops further attempts; exhausted attempts never generate an ungrounded answer; provider-unavailable embeddings use provider fallback without entering the rewrite loop.
- [x] **Step 2: Run focused tests.** Run `./gradlew :modules:assistant:test --tests '*KnowledgeAnswerServiceTest' --tests '*SpringAiQueryImproverTest'`; expected failure is the missing bounded orchestration.
- [x] **Step 3: Implement the query-improvement loop.** Use the existing `KnowledgeRetriever`, shared embedding service, quality gate, and answer port. Preserve query provenance, attempt number, reason code, and score metrics without persisting raw query variants in traces.
- [x] **Step 4: Wire Spring AI transformers.** Configure bounded transformer clients from the configured Spring AI chat client, use the grounded answer boundary only with accepted documents, and keep empty-context behavior fail closed.
- [x] **Step 5: Run focused tests and commit.**

  ```bash
  ./gradlew :modules:assistant:test --tests '*KnowledgeAnswerServiceTest' \
    --tests '*SpringAiQueryImproverTest' --tests '*RagQueryServiceTest'
  git add modules/assistant
  git commit -m "feat(rag): add bounded query improvement"
  ```

### Task 6: Make semantic tools and cache explicit embedding-first shortcuts

#### Current compatibility slice — prepared proactive routing

The proactive semantic tool boundary now accepts only a prepared
`SemanticQuery`. ChatService prepares the query once when semantic shortcuts
are configured and passes that same value to the tool route. The deprecated
raw-string route and its embedding-owning constructor were removed after
focused API, routing, and ChatService tests were migrated. The semantic-cache
raw-string overloads remain as a separate compatibility family until their
callers are migrated.

- [x] Add a failing API-boundary test for the prepared-only proactive route.
- [x] Remove the deprecated raw-string route and legacy constructor.
- [x] Migrate ChatService and proactive-route tests to the shared query.
- [x] Remove the remaining semantic-cache raw-string compatibility family.

**Files:**

- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/tool/SemanticProactiveToolRouter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticToolSelector.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticCacheResolver.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticChatCache.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/tool/AiToolGateway.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiToolCallbackProvider.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiSemanticConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SemanticRoutingProperties.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SemanticCacheProperties.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ChatServiceTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/SemanticChatCacheTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/SemanticIntentRouterTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/semantic/SemanticToolSelectorTraceTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/tool/SemanticProactiveToolRouterTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiSemanticConfigurationTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiToolConfigurationTest.java`

**Interfaces and invariants:**

```text
InputGuard
  → authorized eligibility filter
  → shared embedding
  → vector candidate search
  → route-specific score + margin + freshness gate
  → deterministic shortcut or explicit abstention
```

- Tool acceptance invokes only an authorized read-only tool with typed empty/safe
  arguments. Mutation tools route to the durable workflow and require confirmation.
- Cache acceptance requires high similarity, margin, principal/tenant identity, prompt,
  knowledge, policy, source, locale, embedding, and response-model versions, plus TTL and
  output safety revalidation.
- A semantic miss or embedding failure is a safe miss and then normal chat. It never
  fabricates a vector or changes authorization.

- [ ] **Step 1: Write failing tests.** Prove accepted semantic tool selection does not invoke `ChatCompletionPort`; low-confidence tools abstain; transactional messages never enter the cache; cache hits below threshold or with stale identity miss; cache miss invokes chat exactly once.
- [ ] **Step 2: Run focused tests.** Run:

  ```bash
  ./gradlew :modules:assistant:test --tests '*Semantic*Test' \
    --tests '*SpringAiToolCallbackProviderTest'
  ```

  Expected failure identifies missing fast-path ownership or the new shared query signature.
- [ ] **Step 3: Implement the route policies.** Add separate tool/cache thresholds, keep backend authorization before similarity search, and route semantic abstention to normal handling without exposing the full registry to a model.
- [ ] **Step 4: Verify provider boundaries.** Confirm `EmbeddingModelSelector` falls over only on `EmbeddingProviderUnavailableException`; invalid vectors and security failures remain visible.
- [ ] **Step 5: Commit.**

  ```bash
  git add modules/assistant
  git commit -m "feat(assistant): enforce embedding-first semantic shortcuts"
  ```

## 6. Phase C — Guardrails and Spring AI Composition

### Task 7: Define typed guardrail contracts

**Files:**

- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/guardrail/GuardrailAction.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/guardrail/GuardrailDecision.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/guardrail/InputRequest.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/guardrail/ContextRequest.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/guardrail/ToolRequest.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/guardrail/GroundingRequest.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/guardrail/OutputRequest.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/guardrail/DeliveryRequest.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/guardrail/GuardrailRequest.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/InputGuard.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/ContextGuard.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/ToolGuard.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/OutputGuard.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/GroundingGuard.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/DeliveryGuard.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/GuardrailPipeline.java`
- Test: `libraries/ai-contracts/src/test/java/com/emme/ai/contracts/guardrail/GuardrailContractTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/guardrail/GuardrailPipelineTest.java`

**Interfaces:**

```java
public enum GuardrailAction {
  ALLOW, REDACT, CLARIFY, DENY, BLOCK, ESCALATE, REGENERATE, NO_ANSWER, DELIVER
}

public record GuardrailDecision(
    GuardrailAction action,
    String code,
    Map<String, String> safeAttributes) {}

public record InputRequest(
    String message, long contentBytes, int attachmentCount, String idempotencyKey) {}

public record ContextRequest(
    UUID tenantId,
    UUID principalId,
    Set<String> roles,
    String traceId,
    Instant deadline) {}

public record ToolRequest(
    String toolKey,
    Map<String, String> arguments,
    boolean mutating,
    boolean confirmed,
    String idempotencyKey) {}

public record GroundingRequest(
    boolean retrievalAccepted,
    double topScore,
    double margin,
    List<String> sourceIds) {}

public record OutputRequest(
    String channel,
    String content,
    boolean structured,
    boolean containsBusinessClaim) {}

public record DeliveryRequest(
    String channel,
    String content,
    int maximumCharacters,
    boolean streaming) {}

public record GuardrailRequest(
    InputRequest input,
    ContextRequest context,
    ToolRequest tool,
    GroundingRequest grounding,
    OutputRequest output,
    DeliveryRequest delivery) {}

public interface InputGuard {
  GuardrailDecision check(InputRequest request, AiExecutionContext context);
}

public interface OutputGuard {
  GuardrailDecision check(OutputRequest request, AiExecutionContext context);
}

public interface ContextGuard {
  GuardrailDecision check(ContextRequest request, AiExecutionContext context);
}

public interface ToolGuard {
  GuardrailDecision check(ToolRequest request, AiExecutionContext context);
}

public interface GroundingGuard {
  GuardrailDecision check(GroundingRequest request, AiExecutionContext context);
}

public interface DeliveryGuard {
  GuardrailDecision check(DeliveryRequest request, AiExecutionContext context);
}

public interface GuardrailPipeline {
  GuardrailDecision evaluate(GuardrailRequest request, AiExecutionContext context);
}
```

`ContextGuard`, `ToolGuard`, `GroundingGuard`, and `DeliveryGuard` use the same decision type
with their specific typed input. The request objects contain only bounded, boundary-local data;
they are never persisted or copied into decisions. `GuardrailPipeline` is the only class that
composes the individual guards, so each guard remains independently testable and injectable.

- [x] **Step 1: Write failing contract tests.** Assert every guardrail action is typed, reason codes are non-blank, safe attributes are bounded, and a denial cannot be represented as `ALLOW`.
- [x] **Step 2: Run focused tests.** Run `./gradlew :libraries:ai-contracts:test --tests '*GuardrailContractTest' :modules:assistant:test --tests '*GuardrailPipelineTest'`; expected failure is missing contracts.
- [x] **Step 3: Implement the contracts and pipeline.** The pipeline evaluates input → context → tool/grounding → output in order and stops on `BLOCK`, `DENY`, `ESCALATE`, or `NO_ANSWER`; it never converts guardrail errors into provider failover.
- [x] **Step 4: Refactor and commit.**

  ```bash
  git add libraries/ai-contracts modules/assistant
  git commit -m "feat(ai): add typed guardrail contracts"
  ```

The typed contract and ordered pipeline were added in the current framework-first
slice. `DefaultGuardrailPipeline` is the injectable composition implementation;
provider transport and concrete policy checks remain Task 8 work.

### Task 8: Enforce guardrails around direct chat, RAG, tools, and delivery

**Files:**

- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/DefaultInputGuard.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/DefaultContextGuard.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/DefaultToolGuard.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/DefaultOutputGuard.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/DefaultGroundingGuard.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/guardrail/DefaultDeliveryGuard.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/advisor/InputGuardAdvisor.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/advisor/OutputGuardAdvisor.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/advisor/GroundingGuardAdvisor.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ProcessConversationService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ChatService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ConversationWorkflowFinalizationService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/controller/AiController.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/application/service/ProcessWhatsAppMessageService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiRagConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiChatConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiToolCallbackProvider.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/request/ChatRequest.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/request/RagRequest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/guardrail/DefaultInputGuardTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/guardrail/DefaultContextGuardTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/guardrail/DefaultToolGuardTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/guardrail/DefaultOutputGuardTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/guardrail/DefaultGroundingGuardTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/guardrail/DefaultDeliveryGuardTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/guardrail/GuardrailPipelineTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/in/web/controller/AiControllerConversationTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiToolCallbackProviderTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/advisor/InputGuardAdvisorTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/advisor/OutputGuardAdvisorTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/advisor/GroundingGuardAdvisorTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ChatServiceTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/RagQueryServiceTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiAdvisorConfigurationTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiChatConfigurationTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiRagConfigurationTest.java`

**Interfaces and enforcement order:**

```text
Inbound request
  → InputGuard: auth-bound size/content/attachment/rate/idempotency checks
  → ContextGuard: trusted tenant/principal/role/deadline binding
  → Spring AI advisor chain: prompt boundary, retrieval, model call
  → ToolGuard: schema, allow-list, role, confirmation, idempotency, timeout, response size
  → GroundingGuard: accepted retrieval provenance for knowledge answers
  → OutputGuard: schema, safety, PII/secret leakage, business-claim policy
  → Delivery policy: channel encoding, length, safe streaming, audit event
```

Retrieved documents are data, never instructions. Prompt injection detection isolates
retrieved text from system/developer policy. Moderation is optional provider support and
not the only safety control. If moderation is unavailable, the route policy returns a
safe deterministic decision. Streaming responses are buffered until required checks pass.

- [ ] **Step 1: Write failing tests.** Cover oversized/blank/invalid input, mismatched tenant, prompt-injection content, unauthorized tool, missing confirmation, invalid structured output, unsafe output, rejected grounding, moderation outage, and safe streaming completion.
- [ ] **Step 2: Run focused tests.** Run:

  ```bash
  ./gradlew :modules:assistant:test --tests '*Guard*Test' \
    --tests '*AiController*Test' --tests '*ChatServiceTest' \
    --tests '*SpringAiRagConfigurationTest'
  ```

  Expected result: assertions fail for the newly required boundaries.
- [ ] **Step 3: Implement concrete guards and advisors.** Keep each guard focused, inject every dependency, use backend context, enforce bounded sizes, and map guard decisions to clarification/block/escalation without exposing sensitive evidence.
- [ ] **Step 4: Wire ordered advisors.** Preserve tenant-security → prompt-version → retrieval → output/grounding ordering and retain Spring AI observations/customizers.

Current progress: typed input/output guard advisors are now composed into the
Spring AI chat and RAG chains with tenant-security → input → prompt-version →
retrieval → output precedence. Application-service delivery invocation and the
grounding advisor remain before this task can be marked complete. Grounded RAG
results now preserve the actual retrieved source IDs so grounding checks can
evaluate provenance without synthesizing evidence.

The direct `ChatService` path now also accepts optional typed input/output
guards, checks input before semantic shortcuts, and validates proactive, cached,
and normal responses before returning them. Grounded RAG and controller/channel
delivery boundaries remain outstanding. `RagQueryService` now projects the
bounded retrieval decision and actual source IDs into `GroundingGuard` and
returns the deterministic no-answer response for rejected grounding. The
compatibility constructor path remains single-autowired, blank mock messages
retain their established graceful behavior, and the standalone Spring chat
root tolerates absent optional guardrail advisors.
The WhatsApp application boundary now checks `DeliveryGuard` before recording
and sending a reply, using the channel's 4,096-character text limit.
The web controller now checks the same typed delivery boundary inside the
trusted execution scope for legacy chat, durable conversation, and RAG
responses; web does not impose an arbitrary application character cap.
The durable conversation service also checks delivery before persisting the
assistant response or completing idempotency, so blocked responses cannot be
recorded as successfully delivered turns.
The Spring AI RAG advisor chain now also includes a grounding advisor after
retrieval; it projects Spring AI document provenance into `GroundingRequest`
and fails closed before generation when the typed grounding decision rejects.
- [x] **Step 5: Run, refactor, and commit.**

  ```bash
  ./gradlew :modules:assistant:test --tests '*Guard*Test' \
    --tests '*SpringAi*ConfigurationTest' --tests '*AiController*Test'
  git add modules/assistant
  git commit -m "feat(assistant): enforce model input and output guardrails"
  ```

## 7. Phase D — Durable Workflows

### Task 9: Add per-node model, tool, memory, and guardrail policy

**Files:**

- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/workflow/NodeProfile.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/workflow/NodeModelRole.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/workflow/NodeMemoryPolicy.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/workflow/NodeToolPolicy.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/workflow/NodeGuardrailPolicy.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/workflow/NodeContext.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/workflow/NodeResult.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/application/workflow/NodePolicyRegistry.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/ConversationWorkflowCapabilities.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/ConversationWorkflowGraph.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/LangGraphConversationWorkflowAdapter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfiguration.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/workflow/NodePolicyRegistryTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/ConversationWorkflowGraphTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiLangGraphProductionBoundaryTest.java`

**Interfaces:**

```java
public record NodeProfile(
    String nodeId,
    NodeModelRole modelRole,
    NodeToolPolicy tools,
    NodeMemoryPolicy memory,
    NodeGuardrailPolicy guardrails,
    int maxToolCalls,
    Duration timeout,
    boolean mayInterrupt,
    boolean requiresApproval) {}

public record NodeContext<S>(
    S visibleState,
    AiExecutionContext executionContext,
    NodeProfile profile) {}

public record NodeResult<P>(P statePatch, GuardrailDecision decision) {}

public enum NodeModelRole { NONE, ROUTER, EXTRACTOR, ANSWER, REVIEW }

public record NodeToolPolicy(
    Set<String> allowedKeys, boolean readOnly, boolean requiresConfirmation) {}

public record NodeMemoryPolicy(Set<String> allowedScopes, int maxTurns, boolean includeLongTerm) {}

public record NodeGuardrailPolicy(
    boolean checkInput,
    boolean checkContext,
    boolean checkTool,
    boolean checkGrounding,
    boolean checkOutput) {}
```

Deterministic nodes use `modelRole = NodeModelRole.NONE`. A node receives a projected state and filtered
tool gateway; no node can request arbitrary memory or the full tool registry. State patches
are immutable, namespaced, bounded, and JSON-safe before conversion to LangGraph4j
`AgentState`. The mapping to graph maps exists only in the adapter.

Current progress: the provider-neutral immutable node policy value objects and
`NodePolicyRegistry` are in place. The registry rejects duplicate and unknown node
identifiers, and `NodeProfile` validates bounded timeout/tool-call policy while the
tool and memory policies defensively copy their allow-lists. The conversation graph
now validates that its registry contains exactly one policy for every graph node,
and the LangGraph composition root supplies the default registry without adding a
bridge dependency. Capability nodes now reject approval or confirmation pauses when
their profile does not permit interruption, and enforce the configured node timeout.
The tool and memory policy objects expose immutable allow-list projections for
candidate tool keys and memory scopes. Full capability invocation projection,
approval semantics, and adapter state-patch namespace validation remain in the
next slices. `WorkflowStep` now rejects non-JSON values and recursively copies
bounded maps/lists before the graph adapter can serialize them. Capability
requests created by the graph now also carry the resolved `NodeProfile`; the
three-argument request constructor remains for direct non-graph callers.

- [ ] **Step 1: Write failing tests.** Prove model-facing nodes require an explicit model/tool/memory/timeout/interruption policy; deterministic nodes declare `NONE`; disallowed tools and memory fields are absent from the projection; timeout and approval policy are enforced.
- [ ] **Step 2: Run focused tests.** Run `./gradlew :modules:assistant:test --tests '*NodePolicyRegistryTest' --tests '*ConversationWorkflowGraphTest'`; expected failure is missing policy enforcement.
- [ ] **Step 3: Implement typed profiles and projection.** Register one profile per graph node, reject duplicate/unknown node IDs, filter tools before node invocation, and validate state patches before LangGraph serialization.
- [ ] **Step 4: Verify core-only integration.** Keep `langgraph4j-core` as the only graph runtime dependency; do not add the bridge dependency while the profile boundary is being established.
- [ ] **Step 5: Commit.**

  ```bash
  git add modules/assistant
  git commit -m "feat(workflow): enforce per-node AI policies"
  ```

### Task 10: Implement hold-first booking and payment resume

**Files:**

- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/appointment/AppointmentHold.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/payment/PaymentLink.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/payment/PaymentWorkflowEvent.java`
- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/workflow/PaymentWorkflow.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/AppointmentBookingWorkflow.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/AppointmentPaymentWorkflow.java`
- Create: `modules/appointments/src/main/java/com/emme/appointments/api/command/CreateAppointmentHoldCommand.java`
- Create: `modules/appointments/src/main/java/com/emme/appointments/api/usecase/CreateAppointmentHoldUseCase.java`
- Create: `modules/appointments/src/main/java/com/emme/appointments/api/usecase/ReleaseAppointmentHoldUseCase.java`
- Create: `modules/appointments/src/main/java/com/emme/appointments/application/service/CreateAppointmentHoldService.java`
- Create: `modules/appointments/src/main/java/com/emme/appointments/application/service/ReleaseAppointmentHoldService.java`
- Create: `modules/payment/src/main/java/com/emme/payment/api/command/CreatePaymentLinkCommand.java`
- Create: `modules/payment/src/main/java/com/emme/payment/api/usecase/CreatePaymentLinkUseCase.java`
- Create: `modules/payment/src/main/java/com/emme/payment/application/service/CreatePaymentLinkService.java`
- Create: `database/src/main/resources/db/emme-studio/releases/0.1.0/035-appointment-holds.sql` (the current branch already uses `034-calendar-event-link-cardinality.sql`)
- Create: `database/src/main/resources/db/emme-studio/releases/0.1.0/036-ai-payment-workflow.sql`
- Create: `database/src/main/resources/db/emme-studio/releases/0.1.0/037-ai-workflow-correlations.sql`
- Modify: `database/src/main/resources/db/emme-studio/changelog.yaml`
- Modify: `modules/payment/src/main/java/com/emme/payment/adapter/in/webhook/MercadoPagoWebhookController.java`
- Modify: `modules/payment/src/main/java/com/emme/payment/adapter/out/persistence/adapter/PaymentWebhookEventPersistenceAdapter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/LangGraphConversationWorkflowAdapter.java`
- Test: `modules/appointments/src/test/java/com/emme/appointments/adapter/out/persistence/adapter/AppointmentCollisionAdapterTest.java`
- Test: `modules/appointments/src/test/java/com/emme/appointments/application/service/AppointmentMutationAuthorizationTest.java`
- Test: `modules/payment/src/test/java/com/emme/payment/PaymentApplicationBoundaryTest.java`
- Test: `database/src/test/java/com/emme/database/AppointmentCollisionMigrationContractTest.java`
- Test: `database/src/test/java/com/emme/database/AiWorkflowCheckpointMigrationContractTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/AppointmentBookingWorkflowTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/AppointmentPaymentWorkflowTest.java`
- Integration test: `modules/appointments/src/integrationTest/java/com/emme/appointments/repository/AppointmentCollisionConcurrencyIntegrationTest.java`
- Integration test: `modules/payment/src/integrationTest/java/com/emme/payment/PaymentIntegrationTest.java`
- Integration test: `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/PaymentWorkflowResumeIntegrationTest.java`

**Interfaces:**

```java
public interface CreateAppointmentHoldUseCase {
  AppointmentHold create(CreateAppointmentHoldCommand command);
}

public interface CreatePaymentLinkUseCase {
  PaymentLink create(CreatePaymentLinkCommand command);
}

public interface PaymentWorkflow {
  WorkflowHandle resume(PaymentWorkflowEvent event, AiExecutionContext context);
}

public interface ConversationWorkflow {
  WorkflowHandle startOrResume(WorkflowCommand command, AiExecutionContext context);
}
```

`AppointmentBookingWorkflow` implements the existing `ConversationWorkflow` contract;
`AppointmentPaymentWorkflow` implements the new `PaymentWorkflow` contract. The concrete
class names describe the durable subgraph, while the protocols stay small and framework-free.

The new boundary records are deliberately narrow:

```java
public record CreateAppointmentHoldCommand(
    UUID appointmentId, String idempotencyKey) {}

public record CreatePaymentLinkCommand(
    UUID workflowId, UUID holdId, String idempotencyKey) {}

public record AppointmentHold(
    UUID holdId, UUID appointmentId, Instant expiresAt, String idempotencyKey) {}

public record PaymentLink(
    UUID linkId, UUID workflowId, String provider, String checkoutUrl, Instant expiresAt) {}

public record PaymentWorkflowEvent(
    UUID workflowId, String provider, String eventId, String providerReference, String status) {}
```

Tenant, customer, price, currency, and ownership are resolved from trusted backend state;
they are not accepted from model output or a client-supplied workflow map. Each record
validates identifiers, non-blank keys, expiry timestamps, and provider callback status before
crossing its module boundary.

The booking graph is:

```text
extract → validate → availability/policy/payment fan-out → join → propose
  → customer confirmation interrupt → AppointmentHold
  → PaymentLink → WAITING_FOR_PAYMENT
  → verified webhook → verify payment/hold → revalidate slot
  → confirmed appointment → Modulith events
```

The hold and payment services derive tenant, customer, amount, currency, and ownership
from trusted application state. Store each record in the same tenant boundary already used by
the owning module: schema-local persistence for tenant-schema tables, or shared persistence with
RLS and explicit tenant predicates for control-plane tables. Do not introduce a second tenant
lookup or a mixed schema/RLS access path. Provider callbacks are signature-verified and
idempotent. Graph replay cannot repeat a mutation without the business idempotency key.

Current progress: the framework-neutral appointment-hold, payment-link, normalized
payment-event, and payment-workflow contracts are now present with identifier,
correlation, expiry, and normalized-status validation. Tenant-scoped persistence,
idempotent application services, callback ownership checks, and checkpointed graph
edges remain for the next slices. Appointment and payment modules now expose the
planned hold/link commands and use-case ports through the canonical contracts
library. The appointments module now also has a tenant-schema hold repository port and
an injected-clock `CreateAppointmentHoldService` that returns an existing hold for a
replayed idempotency key before reading the appointment and otherwise creates a bounded
expiry. Its release service delegates to the same repository boundary. Persistence
adapters, callback ownership checks, and checkpointed graph edges remain. The payment
module now has tenant-schema link/source ports and a `CreatePaymentLinkService` that
reads amount, currency, description, and expiry from the trusted source port, reuses
idempotent links, and extracts a provider-neutral checkout URL from provider metadata.
The appointment module now persists holds through a tenant-schema JPA adapter and
forward migration `035-appointment-holds.sql` with RLS, expiry/appointment indexes,
and tenant/idempotency uniqueness. Payment-link persistence, callback ownership checks,
and checkpointed graph edges remain. The payment-link repository now persists its
idempotency key separately from the provider-neutral `PaymentLink` value object, so
replay uniqueness is durable without widening the shared contract. Composition is
intentionally deferred until the remaining tenant and provider dependencies are
established.

- [ ] **Step 1: Write failing tests.** Cover hold creation, duplicate hold idempotency, concurrent collision, expiry, payment-link amount from persisted state, duplicate callback, wrong tenant/workflow callback, successful resume, stale hold recovery, and no direct model mutation.
- [ ] **Step 2: Run focused tests.** Run appointment/payment unit and migration contract tests; expected failure is missing hold/link contracts and workflow nodes.
- [ ] **Step 3: Implement contracts, tables, and services.** Add tenant-scoped RLS tables with unique business idempotency keys, expiry indexes, provider correlation, and lifecycle constraints. Use existing appointment/payment application services for final mutations.
- [ ] **Step 4: Implement typed graph edges.** Add explicit interrupts and checkpointed statuses, verify trusted context before every resume, and use read-only fan-out only before mutation.
- [ ] **Step 5: Run focused tests and commit.**

  ```bash
  ./gradlew :modules:appointments:test :modules:payment:test \
    :modules:assistant:test --tests '*Workflow*Test' \
    :database:test --tests '*Migration*Test'
  git add libraries/ai-contracts modules/appointments modules/payment modules/assistant database
  git commit -m "feat(workflow): add hold-first payment booking"
  ```

### Task 11: Add reschedule, cancellation, staff, notification, and calendar paths

**Files:**

- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/AppointmentRescheduleWorkflow.java`
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/AppointmentCancellationWorkflow.java`
- Modify: `modules/appointments/src/main/java/com/emme/appointments/application/service/RescheduleAuthorizedAppointmentService.java`
- Modify: `modules/appointments/src/main/java/com/emme/appointments/application/service/CancelAuthorizedAppointmentService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/LangGraphQuoteWorkflowResumeAdapter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfiguration.java`
- Modify: `modules/notification/src/main/java/com/emme/notification/application/port/out/NotificationEventPublisher.java`
- Modify: `modules/calendar/src/main/java/com/emme/calendar/application/port/out/GoogleCalendarPort.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/ConversationWorkflowCapabilities.java`
- Test: `modules/appointments/src/test/java/com/emme/appointments/application/service/AppointmentMutationAuthorizationTest.java`
- Test: `modules/payment/src/test/java/com/emme/payment/PaymentApplicationBoundaryTest.java`
- Test: `modules/notification/src/test/java/com/emme/notification/application/service/NotificationDeliveryBoundaryTest.java`
- Test: `modules/calendar/src/test/java/com/emme/calendar/CalendarPackageConventionTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/AppointmentRescheduleWorkflowTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/AppointmentCancellationWorkflowTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ResumeConversationWorkflowServiceTest.java`
- Integration test: `modules/appointments/src/integrationTest/java/com/emme/appointments/repository/AppointmentCollisionConcurrencyIntegrationTest.java`
- Integration test: `modules/payment/src/integrationTest/java/com/emme/payment/PaymentIntegrationTest.java`
- Integration test: `modules/notification/src/integrationTest/java/com/emme/notification/NotificationIntegrationTest.java`
- Integration test: `modules/calendar/src/integrationTest/java/com/emme/calendar/CalendarIntegrationTest.java`

**Interfaces:**

```java
public interface ConversationWorkflow {
  WorkflowHandle startOrResume(WorkflowCommand command, AiExecutionContext context);
}
```

`AppointmentRescheduleWorkflow` and `AppointmentCancellationWorkflow` implement this
existing protocol and decode only their typed command payloads. They return `WorkflowHandle`
and never expose LangGraph4j state or graph maps to appointment, payment, notification, or
calendar modules.

Rescheduling loads the owned appointment, evaluates policy, fans out target availability,
price difference, and payment/refund policy, then interrupts for confirmation. Cancellation
evaluates the cancellation window and refund policy before confirmation. Staff review uses
backend role and reviewer identity. Notifications and calendar synchronization occur from
committed Modulith events, never from model output. These events are internal to the initial
deployment: no `@Externalized` annotation, Kafka provider, broker bootstrap setting, or direct
Kafka API is introduced by this task. Listener failure recovery and publication retry remain
owned by the existing Modulith JDBC registry.

- [ ] **Step 1: Write failing tests.** Cover owned-appointment checks, unavailable target slot, price increase/decrease, refund eligibility, cancellation window, staff-only approval, duplicate resume, notification event idempotency, and calendar reconciliation after restart.
- [ ] **Step 2: Run focused tests.** Run:

  ```bash
  ./gradlew :modules:appointments:test :modules:payment:test \
    :modules:notification:test :modules:calendar:test \
    :modules:assistant:test --tests '*ResumeConversationWorkflowServiceTest' \
    --tests '*Appointment*WorkflowTest'
  ```

  Expected failure identifies missing graph routes and event wiring.
- [ ] **Step 3: Implement graph subflows.** Reuse `RescheduleAuthorizedAppointmentService`, `CancelAuthorizedAppointmentService`, `RefundPaymentService`, and existing event publishers. Keep all mutation nodes deterministic and application-service backed.
- [ ] **Step 4: Run phase integration checks.** Run the assistant workflow integration suite and payment callback integration suite. If Docker or PostgreSQL is unavailable, record the exact blocked command and keep the test strict.
- [ ] **Step 5: Commit.**

  ```bash
  git add modules/assistant modules/appointments modules/payment modules/notification modules/calendar
  git commit -m "feat(workflow): add appointment lifecycle subflows"
  ```

## 8. Phase E — Evidence, Cleanup, and Release Gate

### Task 12: Add operational evidence and controlled retrieval improvement

**Files:**

- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/trace/AiSemanticExecutionTrace.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/observability/MicrometerSemanticMetrics.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/trace/AiTraceRecorder.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/trace/AiTraceRedactor.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiTraceRecorder.java`
- Modify: `tools/ai-evaluation/src/emme_ai_evaluation/contracts.py`
- Modify: `tools/ai-evaluation/src/emme_ai_evaluation/pipeline.py`
- Modify: `tools/ai-evaluation/src/emme_ai_evaluation/redaction.py`
- Modify: `tools/ai-evaluation/tests/test_pipeline.py`
- Modify: `tools/ai-evaluation/README.md`
- Modify: `docs/superpowers/specs/2026-09-04-emme-durable-conversational-workflows-design.md` results/status
- Modify: `tasks/todo.md` with exact phase evidence
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/trace/AiTraceRedactorTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/observability/MicrometerSemanticMetricsTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiTraceRecorderTest.java`
- Test: `tools/ai-evaluation/tests/test_pipeline.py`
- Test: `tools/ai-evaluation/tests/test_redaction.py`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/trace/AiSemanticExecutionTraceTest.java`

**Interfaces and evidence:**

Record only bounded metadata:

```text
operation, route, attempt, accepted, reasonCode,
topScore, secondScore, margin, supportCount,
embeddingModelVersion, retrievalLatency, modelLatency,
providerOutcome, fallbackReason
```

Never persist raw prompts, query variants, retrieved text, vectors, payment data, or
unbounded tenant/principal labels in metrics. Live retrieval outcomes may create a
PII-redacted evaluation candidate, but production thresholds, prompts, tools, and tenant
knowledge change only through offline evaluation, regression, shadow, canary, and explicit
promotion evidence.

- [ ] **Step 1: Write failing tests.** Assert score/margin/attempt metrics use bounded operation labels, traces redact sensitive payloads, evaluation candidates require accepted policy and successful outcome, and promotion rejects incomplete evaluation evidence.
- [ ] **Step 2: Run focused tests.** Run:

  ```bash
  ./gradlew :modules:assistant:test --tests '*Trace*Test' \
    --tests '*SemanticMetricsTest'
  cd tools/ai-evaluation && uv run pytest tests/test_pipeline.py tests/test_redaction.py
  ```

  Expected failure identifies missing retrieval-attempt fields or promotion gates.
- [ ] **Step 3: Implement evidence and offline evaluation.** Use existing trace/evaluation boundaries, add retrieval-quality metrics, and update the Python scaffold to evaluate groundedness/relevancy without promoting automatically.
- [ ] **Step 4: Verify.** Run Java focused tests and the offline Python dataset/evaluation gate with a non-empty fixture set; keep missing provider data a safe evaluation failure.
- [ ] **Step 5: Commit.**

  ```bash
  git add modules/assistant tools docs tasks/todo.md
  git commit -m "feat(ai): record guarded retrieval evidence"
  ```

### Task 13: Remove compatibility duplicates and run the enterprise gate

#### Current compatibility slice — canonical embedding service

The deprecated Assistant `EmbeddingModelPort` subtype was deleted after all
embedding callers migrated to the provider-neutral `EmbeddingService`. The
remaining deprecated chat/provider compatibility families are independent and
remain tracked for later deletion.

- [x] Add a failing source-inventory test for the deprecated embedding port.
- [x] Migrate production, composition-root, and test callers to
      `EmbeddingService`.
- [x] Delete the compatibility source and run Assistant verification gates.

#### Current compatibility slice — canonical chat completion boundary

Legacy Assistant chat composition now consumes `AiChatCompletion` rather than
the deprecated composite `AiModelProvider`. Provider-identified Spring AI
adapters and mock coverage preserve execution-context and provider-admission
policy, while the composite provider remains only for the embedding, image,
and retrieval callers that still need migration.

- [x] Add canonical chat adapter and composition-root coverage.
- [x] Migrate legacy chat composition and preserve empty-input compatibility.
- [x] Verify AI-contract, AI-platform, and Assistant gates.
- [ ] Migrate the remaining composite capability callers before deleting the
      composite provider.

The document retrieval adapter has since been moved to the canonical
`EmbeddingService` boundary. Its tenant binding, dimension validation, and
score-preservation behavior remain covered; platform embedding and image
adapters still block composite-provider deletion.

The image capability has since been moved to `CaptionImageUseCase` directly:
Spring AI vision, mock captioning, and unsupported Groq behavior are composed
at the provider boundary, and the redundant composite image adapter was
deleted. The platform embedding adapter remains the final provider caller
blocking composite-provider deletion.

The platform embedding adapter has since been removed as well. Mock, Ollama,
and unsupported Groq paths now expose `EmbeddingService` directly with
versioned vector identity; only the deprecated composite provider declaration,
compatibility mock, and composition bean/test family remain to be deleted.

The deprecated composite provider family has since been deleted after the
repository-wide production caller inventory became clean. Canonical chat,
embedding, image, and retrieval capabilities are now composed independently;
the remaining compatibility cleanup families are tracked separately.

**Files:**

- Modify/delete deprecated embedding aliases only after repository-wide caller search:
  `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/model/EmbeddingModel.java`,
  `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/EmbeddingModelPort.java`,
  `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/EmbeddingVector.java`
- Delete only verified duplicate provider, tool, RAG, and workflow wrappers after replacement tests pass
- Modify: `applications/emme-platform/src/test/java/com/emme/ApplicationServiceArchitectureTest.java`
- Modify: `applications/emme-platform/src/test/java/com/emme/CrossModuleDependencyArchitectureTest.java`
- Modify: `applications/emme-platform/src/test/java/com/emme/LayerConventionTest.java`
- Modify: `applications/emme-platform/src/test/java/com/emme/NamedInterfaceArchitectureTest.java`
- Modify: `applications/emme-platform/src/test/java/com/emme/NamingConventionArchitectureTest.java`
- Modify: `applications/emme-platform/src/test/java/com/emme/RepositoryFrameworkBoundaryArchitectureTest.java`
- Modify: `applications/emme-platform/src/test/java/com/emme/SchemaOwnershipTest.java`
- Modify: `docs/superpowers/plans/2026-09-03-ai-platform-simplification.md` with a link to this plan and completed-scope boundary
- Modify: `docs/superpowers/specs/2026-09-01-ai-platform-simplification-blueprint.md` implementation results
- Modify: `tasks/todo.md` exact final evidence

- [ ] **Step 1: Write failing architecture tests.** Assert no assistant application consumer imports the deprecated local vector type, no contract imports Spring or LangGraph4j, no model-facing node receives an unrestricted tool registry, and no RAG adapter emits a constant zero score for a ranked hit.
- [ ] **Step 2: Run architecture and caller inventory.** Run:

  ```bash
  rg -n '\b(EmbeddingModelPort|EmbeddingVector|AiModelProvider|KnowledgeRetriever)\b' libraries modules applications
  ./gradlew :applications:emme-platform:test --tests '*Architecture*' \
    --tests '*Convention*'
  ```

  Expected result: only intentionally retained compatibility declarations remain before deletion.
- [ ] **Step 3: Delete one duplicate at a time.** Migrate its final callers, remove imports/configuration, run the replacement module tests, then commit a reversible deletion. Do not remove `AiModelProvider` until its chat/embedding callers and compatibility tests are gone.
- [ ] **Step 4: Run the focused and phase checks.** Run the contract, assistant, documents, catalog, appointments, payment, notification, calendar, AI platform, migration, architecture, and workflow suites with no skipped tests.
- [ ] **Step 5: Run the final enterprise gate.** Execute:

  ```bash
  ./gradlew spotlessCheck check --no-parallel --no-configuration-cache
  ./gradlew :applications:emme-platform:integrationTest --no-parallel --no-configuration-cache
  ./gradlew :applications:emme-platform:coverageCheck --no-parallel --no-configuration-cache
  ./gradlew :applications:emme-platform:e2eTest --no-parallel --no-configuration-cache
  git diff --check
  git status --short
  ```

  Expected result: local gates pass with zero failures and zero skipped tests. If a
  container, PostgreSQL, or deployed E2E dependency is unavailable, record the exact
  blocker in `tasks/todo.md`; do not weaken or skip the test.
- [ ] **Step 6: Commit documentation closure and push.**

  ```bash
  git add docs tasks
  git commit -m "docs(ai): close durable workflow implementation plan"
  git push origin feat/ai-platform-foundation
  git log --oneline origin/feat/ai-platform-foundation -1
  ```

## 9. Dependency and Rollback Notes

| Dependency | Required before | Rollback boundary |
|---|---|---|
| Versioned embedding result | Score-aware RAG, shared semantic query, cache/tool identity | Keep deprecated adapters until all consumers compile and focused tests pass |
| Score preservation | Retrieval quality gate | Disable thresholded RAG feature flag and retain bounded unavailable response; never emit ungrounded knowledge |
| Retrieval quality gate | Query improvement and grounded answer | Use deterministic `NO_ANSWER`/`CLARIFY`, not direct answer fallback |
| Guardrail pipeline | Spring AI advisors and durable model nodes | Disable affected model path and expose safe bounded response; business mutations remain unavailable |
| Node profiles | Typed LangGraph subgraphs | Keep existing core graph behind feature flag; fail startup if enabled without complete profiles |
| Holds/payment links | Payment-dependent booking | Release expired holds; retain payment callback idempotency and never confirm without verified payment |
| Appointment lifecycle subgraphs | Notifications/calendar event integration | Keep business services callable directly; graph resume is retry-safe through workflow idempotency |

## 10. Definition of Done

- [ ] One versioned `EmbeddingService` result is shared by catalog, RAG, intent routing, semantic tools, and semantic cache.
- [ ] Document search scores survive the Documents → Assistant → Spring AI RAG boundary.
- [ ] Tools, cache, intent, and RAG use embedding-first decisions with separate score/margin/policy gates and explicit abstention.
- [ ] RAG retries only through the bounded query-improvement ladder and never generates an ungrounded tenant-knowledge answer.
- [ ] Input, context, tool, output, grounding, and delivery guardrails are typed, injected, observable, and fail closed.
- [ ] Every model-facing graph node has explicit model, tools, memory, timeout, interruption, and guardrail policy; deterministic nodes use `NONE`.
- [ ] Booking, payment, webhook, reschedule, cancellation, refund, staff, notification, and calendar flows pause/resume durably and remain idempotent.
- [ ] PostgreSQL remains authoritative; Redis remains a rebuildable projection; tenant/RLS boundaries remain enforced.
- [ ] `langgraph4j-core` is the only LangGraph runtime dependency until a separate bridge compatibility decision is approved.
- [ ] Focused tests pass after each slice; phase integration checks and the final enterprise gate are recorded with exact results.
- [ ] Deprecated duplicates are removed only after caller inventory and replacement evidence.
- [ ] All plan/document/code changes are committed and pushed to `feat/ai-platform-foundation`.

## 11. Reference Documentation

- [Durable workflow design](../specs/2026-09-04-emme-durable-conversational-workflows-design.md)
- [AI platform simplification blueprint](../specs/2026-09-01-ai-platform-simplification-blueprint.md)
- [Previous simplification implementation record](2026-09-03-ai-platform-simplification.md)
- [Spring AI modular RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- [Spring AI Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html)
- [Spring AI vector similarity thresholds](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- [Spring AI moderation](https://docs.spring.io/spring-ai/reference/api/moderation.html)
- [Spring AI evaluation](https://docs.spring.io/spring-ai/reference/api/testing.html)
- [LangGraph4j subgraphs](https://langgraph4j.github.io/langgraph4j/1.9/core/subgraph/)
- [LangGraph4j state and checkpoints](https://github.com/langgraph4j/langgraph4j/blob/main/langgraph4j-core/src/site/markdown/concepts/low_level.md)
