# AI Contracts, AI Platform, and Assistant Simplification Design

| Field | Detail |
|---|---|
| Date | 2026-09-03 |
| Scope | `libraries:ai-contracts`, `modules:ai-platform`, `modules:assistant`, and required cross-module persistence/event integration points |
| Status | Design approved in principle; pending written-spec review |
| Primary goal | Reduce custom code by delegating mechanics to existing Spring and provider capabilities while preserving Emme policy and enterprise guarantees |
| Current platform baseline | Spring Boot `4.1.0`, Spring Modulith `2.1.0`, Spring AI `2.0.1`, Java 25-compatible Gradle build |

## 1. Objective

Simplify the AI platform without weakening tenant isolation, authorization,
durability, idempotency, auditability, observability, or performance. The
implementation will standardize names, remove duplicate contracts, prefer
existing framework features, and choose persistence technology per operation.

The default persistence choice is Spring Data JPA. `JdbcClient` is retained
only for operations that require dynamic PostgreSQL identifiers, bootstrap
access before normal JPA is available, RLS/session behavior, atomic claims,
JSONB/pgvector/AGE operations, or where a small direct SQL adapter is clearly
less code and safer than an equivalent JPA implementation.

## 2. Guiding rules

```text
Framework capability first.
Emme policy and security remain explicit.
JPA first for clear entity-backed persistence.
JdbcClient only when JPA is impossible, unsafe, or materially more complex.
PostgreSQL is authoritative durable state.
Redis is temporary/cache/coordination/live state.
Spring Modulith is the internal event boundary.
Kafka is reserved for external durable event boundaries.
LangGraph4j and AGE are optional, narrow capabilities.
Every deletion requires caller search, tests, compilation, and architecture evidence.
```

Spring's `JdbcClient` is the modern fluent API for normal JDBC query/update
operations, but Spring documents that advanced operations can still require
lower-level JDBC APIs. JPA repositories provide derived queries, projections,
locking, and custom repository fragments. These capabilities define the
JPA-first decision rather than a blanket migration rule.

References: [Spring JDBC](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html), [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa.html), [Spring Data JPA custom repositories](https://docs.spring.io/spring-data/jpa/reference/repositories/custom-implementations.html).

## 3. Target architecture

```text
ai-contracts
  framework-neutral records, ports, policy values, and errors
        ↓
ai-platform
  provider adapters, model admission, observations, learning/evaluation
        ↓
assistant
  conversation orchestration, routing, tools, RAG policy, workflows
        ↓
adapters and configuration
  Spring AI, JPA, JdbcClient, Redis, VectorStore, Modulith, Kafka, AGE, LangGraph4j
```

Application services depend on capability contracts and application-owned
ports. Framework classes remain in adapters and configuration. Business
modules remain authoritative for appointments, services, payments, identity,
and tenancy.

Spring AI owns model transport, `ChatClient`, structured output, tool calling,
advisors, RAG mechanics, observations, and vector-store mechanics. Spring
Modulith owns internal event publication and selected Kafka externalization.
Redis is not a replacement for durable PostgreSQL state.

References: [Spring AI API](https://docs.spring.io/spring-ai/reference/api/), [Spring AI advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html), [Spring AI vector stores](https://docs.spring.io/spring-ai/reference/api/vectordbs.html), [Spring Modulith events](https://docs.spring.io/spring-modulith/reference/events.html), [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/redis.html).

## 4. Naming standard

Names expose capability and intent, not historical implementation details.

| Role | Standard | Examples |
|---|---|---|
| Framework-neutral capability | Capability noun | `ChatModel`, `EmbeddingModel`, `KnowledgeSearch`, `GraphSearch` |
| Application policy | Policy name | `SemanticRouter`, `AuthorizedToolGateway`, `ModelAdmissionPolicy` |
| Spring AI adapter | `SpringAi` + capability | `SpringAiChatModel`, `SpringAiEmbeddingModel`, `SpringAiKnowledgeSearch` |
| PostgreSQL-specific adapter | `Postgres`/`Jdbc` + capability | `PostgresQuoteWorkflowRepository`, `JdbcAiJobStore` |
| Redis adapter | `Redis` + capability | `RedisSemanticCache`, `RedisLiveEventPublisher` |
| Modulith adapter | `SpringModulith` + capability | `SpringModulithAiJobPublisher` |
| Configuration | Capability + `Configuration` | `AiModelConfiguration`, `AiRagConfiguration`, `AiPersistenceConfiguration` |
| Avoid | Generic or duplicated nouns | `Helper`, `Manager`, `Utils`, `ProviderProvider`, `AdapterAdapter` |

## 5. Persistence selection matrix

| Scenario | Preferred mechanism | Explicit reason |
|---|---|---|
| Entity-backed assistant aggregates | Spring Data JPA | Repository CRUD, mapping, transactions, locking, projections |
| Simple AI metadata CRUD | Spring Data JPA if an entity is justified | Less mapping code than manual SQL |
| Small read projection | JPA projection first; `JdbcClient` if the projection is materially simpler | Avoid unnecessary entity loading without making SQL the default |
| AI job lease/claim/retry | `JdbcClient` | Atomic PostgreSQL update/claim semantics |
| Learning candidate JSON/RLS lifecycle | `JdbcClient` unless JPA is proven equally clear | JSONB, RLS, idempotency, and optimistic transitions |
| Workflow checkpoints | `JdbcClient` unless a proven JPA entity model reduces code | Serialized checkpoints and versioned resume semantics |
| pgvector and AGE | Spring AI `VectorStore` for vector mechanics; `JdbcClient` for AGE or PostgreSQL-only queries | Framework support where available; direct SQL where database-specific |
| Tenant registry metadata | Spring Data JPA | Existing entity/repository model is appropriate |
| Schema creation and Liquibase | JDBC/Liquibase boundary | Schema may not exist and Liquibase requires a JDBC connection |
| Tenant schema resolution during Hibernate bootstrap | Dedicated bootstrap JDBC client/boundary | JPA routing is not available safely during resolver initialization |
| Cache/TTL/lock/live state | Spring Data Redis or Spring AI Redis VectorStore | Redis is temporary operational state, not durable truth |

Each retained `JdbcClient` adapter must record why JPA was rejected. No new
Spring Data JDBC persistence model will be introduced as a general default.

## 6. File inventory

The following is the implementation inventory. `Review` means the file is
inspected and changed only if the capability audit proves it is redundant or
misplaced. `Delete after verification` requires caller search, compilation,
focused tests, integration tests, and architecture tests.

### 6.1 `libraries/ai-contracts`

| File | Action | Decision |
|---|---|---|
| `src/main/java/com/emme/ai/contracts/model/AiModelProvider.java` | Delete after migration | Compatibility composite only; callers move to canonical capabilities |
| `src/main/java/com/emme/ai/contracts/model/ChatCompletionPort.java` | Delete after migration | Duplicate of canonical chat capability |
| `src/main/java/com/emme/ai/contracts/model/ChatModel.java` | Keep/refine | Canonical chat contract |
| `src/main/java/com/emme/ai/contracts/model/ChatRequest.java` | Keep/refine | Stable request value |
| `src/main/java/com/emme/ai/contracts/model/ChatResponse.java` | Keep/refine | Stable response value |
| `src/main/java/com/emme/ai/contracts/model/EmbeddingModel.java` | Keep/refine | Canonical version-aware embedding contract |
| `src/main/java/com/emme/ai/contracts/model/EmbeddingPort.java` | Merge/delete | Consolidate with `EmbeddingModel` |
| `src/main/java/com/emme/ai/contracts/model/ModelCapability.java` | Review | Keep only if admission policy still needs the value |
| `src/main/java/com/emme/ai/contracts/model/ModelExecutionScheduler.java` | Keep/refine | Boundary for bounded model admission |
| `src/main/java/com/emme/ai/contracts/extraction/NailDesignExtractionPort.java` | Rename | `DesignExtractor` |
| `src/main/java/com/emme/ai/contracts/graph/KnowledgeGraphProjector.java` | Rename | `GraphProjectionWriter` |
| `src/main/java/com/emme/ai/contracts/graph/KnowledgeGraphRetriever.java` | Rename | `GraphSearch` |
| `src/main/java/com/emme/ai/contracts/graph/Graph*.java` | Review | Keep stable graph values; remove unused values after caller search |
| `src/main/java/com/emme/ai/contracts/rag/KnowledgeSearch.java` | Keep | Canonical RAG search capability |
| `src/main/java/com/emme/ai/contracts/rag/KnowledgeQuery.java` | Keep/refine | Stable query value |
| `src/main/java/com/emme/ai/contracts/rag/RetrievedDocument.java` | Keep/refine | Stable retrieval result |
| `src/main/java/com/emme/ai/contracts/semantic/*.java` | Review | Keep model identity/cache policy values; remove duplicate transport concepts |
| `src/main/java/com/emme/ai/contracts/tool/*.java` | Keep/refine | Canonical tool definitions, context, request, result, and gateway |
| `src/main/java/com/emme/ai/contracts/workflow/*.java` | Keep/refine | Canonical workflow capability and stable values |
| `src/main/java/com/emme/ai/contracts/learning/*.java` | Keep/refine | Durable learning lifecycle contracts |
| `src/main/java/com/emme/ai/contracts/context/*.java` | Keep | Framework-neutral context values |
| `src/main/java/com/emme/ai/contracts/image/*.java` | Review | Normalize naming to `ImageCaptioner`/image capability where callers permit |
| `src/test/java/com/emme/ai/contracts/ContractValidationTest.java` | Extend | Reject framework imports and duplicate contracts |
| `src/test/java/com/emme/ai/contracts/PlatformContractTest.java` | Extend | Verify canonical capability surface |
| `src/test/java/com/emme/ai/contracts/graph/GraphContractsTest.java` | Update | Verify renamed graph contracts |
| `src/test/java/com/emme/ai/contracts/rag/RagContractTest.java` | Extend | Verify framework-neutral RAG boundary |
| `src/test/java/com/emme/ai/contracts/learning/*.java` | Keep/update | Preserve lifecycle invariants |

### 6.2 `modules/ai-platform`

| File | Action | Decision |
|---|---|---|
| `src/main/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiChatModel.java` | Refactor/rename | Thin adapter over Spring AI chat APIs |
| `src/main/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiEmbeddingModel.java` | Refactor/rename | Thin adapter over Spring AI embeddings |
| `src/main/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiVisionModel.java` | Refactor | Thin image capability adapter |
| `src/main/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiModelProvider.java` | Delete after migration | Composite compatibility provider |
| `src/main/java/com/emme/ai/platform/adapter/out/provider/mock/MockModelProvider.java` | Refactor | Deterministic test adapter only |
| `src/main/java/com/emme/ai/platform/adapter/out/capability/AiCaptionImageAdapter.java` | Rename/refactor | Align image capability naming |
| `src/main/java/com/emme/ai/platform/adapter/out/capability/AiEmbeddingAdapter.java` | Refactor | Keep only if it adds application policy not supplied by Spring AI |
| `src/main/java/com/emme/ai/platform/configuration/AiProviderConfiguration.java` | Refactor | One Spring AI composition root with explicit provider conditions |
| `src/main/java/com/emme/ai/platform/configuration/AiProviderProperties.java` | Review | Remove duplicate provider configuration fields |
| `src/main/java/com/emme/ai/platform/configuration/ModelAdmissionProperties.java` | Keep/refine | Typed capacity/fairness configuration |
| `src/main/java/com/emme/ai/platform/configuration/SpringAiObservationConventions.java` | Refactor | Use framework observations; keep only Emme fields |
| `src/main/java/com/emme/ai/platform/model/BoundedModelExecutionScheduler.java` | Keep/refine | Custom fairness/admission policy only |
| `src/main/java/com/emme/ai/platform/model/ModelCapacityProfile.java` | Keep/refine | Explicit capacity policy |
| `src/main/java/com/emme/ai/platform/model/ModelAdmission*.java` | Keep/refine | Typed policy failures, not transport wrappers |
| `src/main/java/com/emme/ai/platform/learning/JdbcLearningCandidateStore.java` | Review JPA vs `JdbcClient` | Retain SQL only for JSONB/RLS/idempotency complexity |
| `src/main/java/com/emme/ai/platform/learning/JdbcLearningCandidateStateStore.java` | Review JPA vs `JdbcClient` | Evaluate state transitions and optimistic locking |
| `src/main/java/com/emme/ai/platform/learning/JdbcLearningCandidateEvaluationStore.java` | Review JPA vs `JdbcClient` | Evaluate report persistence and idempotency |
| `src/main/java/com/emme/ai/platform/learning/LearningCandidate*.java` | Review/refactor | Keep governance policy; remove redundant orchestration |
| `src/test/java/com/emme/ai/platform/adapter/out/provider/**` | Update | Verify Spring AI delegation and provider identity |
| `src/test/java/com/emme/ai/platform/configuration/**` | Update | Verify mutually exclusive composition and optional startup |
| `src/test/java/com/emme/ai/platform/learning/JdbcLearningCandidate*Test.java` | Extend | Compare JPA candidate and preserve SQL-only invariants |
| `src/test/java/com/emme/ai/platform/model/**` | Keep/update | Verify capacity, fairness, timeout, and interruption |
| `src/test/java/com/emme/ai/platform/adapter/out/provider/mock/**` | Keep | Verify deterministic test provider |
| `build.gradle.kts` | Review | Remove dependencies made obsolete by Spring AI delegation |

### 6.3 `modules/assistant` — application contracts and policy

| File/group | Action | Decision |
|---|---|---|
| `src/main/java/com/emme/assistant/ai/application/provider/ChatModelSelector.java` | Keep/refactor | One ordered fallback/admission policy |
| `src/main/java/com/emme/assistant/ai/application/provider/EmbeddingModelSelector.java` | Keep/refactor | One embedding selection policy |
| `src/main/java/com/emme/assistant/ai/application/provider/RagAnswerProviderChain.java` | Simplify/rename | Replace with direct Spring AI RAG composition where no policy is added |
| `src/main/java/com/emme/assistant/ai/application/provider/TracingChatCompletionPort.java` | Delete or reduce | Keep only fields missing from Spring AI observations |
| `src/main/java/com/emme/assistant/ai/application/provider/TracingEmbeddingModelPort.java` | Delete or reduce | Same rule as chat tracing |
| `src/main/java/com/emme/assistant/ai/application/port/out/IdentifiedChatCompletionPort.java` | Review/delete | Temporary provider-selection detail |
| `src/main/java/com/emme/assistant/ai/application/service/ChatService.java` | Refactor | Depend on one canonical chat capability |
| `src/main/java/com/emme/assistant/ai/application/service/RagQueryService.java` | Refactor | Depend on `KnowledgeSearch`; no framework mechanics |
| `src/main/java/com/emme/assistant/ai/application/service/DetectIntentService.java` | Refactor | Routing policy stays in assistant, not provider transport |
| `src/main/java/com/emme/assistant/ai/application/semantic/*.java` | Review/refactor | Keep thresholds, tenant scope, model identity, and abstention; delegate retrieval/cache mechanics |
| `src/main/java/com/emme/assistant/ai/application/tool/AiTool*.java` | Merge/delete | Use canonical `ai-contracts.tool` values |
| `src/main/java/com/emme/assistant/ai/application/tool/AuthorizedAiToolGateway.java` | Rename/refactor | `AuthorizedToolGateway`; retain security, confirmation, idempotency, audit |
| `src/main/java/com/emme/assistant/ai/application/tool/SemanticProactiveToolRouter.java` | Rename/refactor | `SemanticToolRouter` |
| `src/main/java/com/emme/assistant/ai/application/trace/*.java` | Review/refactor | Keep redaction and business outcome fields; delegate transport observations |
| `src/main/java/com/emme/assistant/ai/application/service/ProcessConversationService.java` | Review | Remove provider-specific fallback and framework leakage |
| `src/main/java/com/emme/assistant/ai/application/service/ProcessDesignQuoteService.java` | Review | Keep domain/application policy; simplify model composition |
| `src/main/java/com/emme/assistant/ai/application/service/ResumeConversationWorkflowService.java` | Review | Keep trusted context restoration and durable resume policy |
| `src/main/java/com/emme/assistant/ai/application/service/ConversationWorkflowFinalizationService.java` | Review | Keep durable finalization and idempotency |
| `src/main/java/com/emme/assistant/ai/application/job/**` | Review | Consolidate job orchestration around Modulith and durable state |

### 6.4 `modules/assistant` — adapters and configuration

| File/group | Action | Decision |
|---|---|---|
| `src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiNailDesignExtractor.java` | Rename/refactor | `SpringAiDesignExtractor`; use structured output |
| `src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiEmbeddingModelAdapter.java` | Refactor/delete | Keep only one embedding translation layer |
| `src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/TenantScopedDocumentRetriever.java` | Keep/reduce | Spring AI detail with trusted tenant context |
| `src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiToolCallbackProvider.java` | Keep/refactor | Thin adapter from authorized gateway to Spring AI callbacks |
| `src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/advisor/*.java` | Keep/refactor | Tenant/security/prompt policy advisors only |
| `src/main/java/com/emme/assistant/ai/adapter/out/persistence/DocumentKnowledgeRetrievalAdapter.java` | Rename/refactor | `SpringAiKnowledgeSearch` or direct VectorStore adapter |
| `src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcSemanticReferenceSearchAdapter.java` | Review | Replace mechanics with VectorStore where possible |
| `src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcSemanticCacheAdapter.java` | Review | Keep PostgreSQL authority; simplify query/mapping |
| `src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiJobStatusStore.java` | Review | Use `JdbcClient` for claims; remove unnecessary template/transaction duplication |
| `src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiToolIdempotencyStore.java` | Review | Keep atomic claim semantics; standardize name to `JdbcToolIdempotencyStore` |
| `src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiTraceRecorder.java` | Review | Keep only durable/redacted persistence not supplied by observations |
| `src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteWorkflowRepository.java` | Review JPA first | Convert to JPA if aggregate mapping is simpler; retain SQL for atomic transitions |
| `src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteReviewRepository.java` | Review JPA first | Same rule |
| `src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteArtifactRepository.java` | Review JPA first | Same rule |
| `src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcDesignImageMetadataRepository.java` | Review JPA first | Entity CRUD likely candidate for JPA |
| `src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcConversationWorkflowReviewAuditAdapter.java` | Review | JPA or append-only `JdbcClient` based on actual schema/query complexity |
| `src/main/java/com/emme/assistant/ai/adapter/out/graph/JdbcAgeGraphClient.java` | Keep/refactor | AGE is PostgreSQL-specific; rename only for clarity |
| `src/main/java/com/emme/assistant/ai/adapter/out/graph/AgeGraphAdapter.java` | Keep/refactor | Optional recommendation boundary |
| `src/main/java/com/emme/assistant/ai/adapter/out/workflow/JdbcLangGraphCheckpointSaver.java` | Review | Keep SQL if checkpoint claims/versioning are simpler than JPA |
| `src/main/java/com/emme/assistant/ai/adapter/out/workflow/TenantAwareCheckpointSaver.java` | Keep/refactor | Security and tenant binding boundary |
| `src/main/java/com/emme/assistant/ai/adapter/out/workflow/LangGraph*.java` | Review | Restrict LangGraph to durable/HITL workflows |
| `src/main/java/com/emme/assistant/ai/adapter/out/redis/RedisAiOperationalStateAdapter.java` | Keep/refactor | Spring Data Redis with Lua only for atomic operational state |
| `src/main/java/com/emme/assistant/ai/adapter/out/redis/RedisAiLiveEventPublisher.java` | Keep/refactor | Redis live state only |
| `src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/RedisSemanticCacheHotStore.java` | Keep/refactor | Spring AI Redis VectorStore projection; safe miss on failure |
| `src/main/java/com/emme/assistant/ai/adapter/in/event/*.java` | Refactor | Spring Modulith internal event adapters and listeners |
| `src/main/java/com/emme/assistant/ai/adapter/in/messaging/AiJobListener.java` | Review | Use Modulith listener and durable job state |
| `src/main/java/com/emme/assistant/ai/adapter/in/messaging/AiJobReconciliationPoller.java` | Review | Retain only recovery work not supplied by publication registry/outbox |
| `src/main/java/com/emme/assistant/ai/configuration/*.java` | Consolidate | Rename configuration by capability and remove duplicate bean roots |
| `src/main/java/com/emme/assistant/ai/configuration/AiClientConfiguration.java` | Refactor/delete | Replace raw HTTP client where Spring AI or `RestClient` supports it |
| `src/main/java/com/emme/assistant/ai/configuration/AiJobExecutorConfiguration.java` | Refactor | Spring-managed executor, `JdbcClient` only if required |
| `src/main/java/com/emme/assistant/ai/configuration/SpringAiTenantJdbcConfiguration.java` | Rename/refine | Explicit bootstrap/tenant client ownership |
| `src/main/java/com/emme/assistant/ai/configuration/SpringAiSemanticConfiguration.java` | Refactor | One vector/search composition root |
| `src/main/java/com/emme/assistant/ai/configuration/SpringAiRedisSemanticConfiguration.java` | Refactor | One Redis vector projection composition root |
| `src/main/java/com/emme/assistant/ai/configuration/SpringAiChatProviderRegistry.java` | Refactor | Delegate provider construction to Spring AI; keep ordering policy |
| `src/main/java/com/emme/assistant/ai/configuration/SpringAiEmbeddingProviderRegistry.java` | Refactor | Same rule for embeddings |
| `src/main/java/com/emme/assistant/ai/configuration/SpringAiRagConfiguration.java` | Refactor | Use advisors/VectorStore; preserve tenant policy |
| `src/main/java/com/emme/assistant/ai/configuration/SpringAiToolConfiguration.java` | Refactor | Register callbacks without duplicate gateway models |
| `src/main/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfiguration.java` | Review | Optional workflow capability only |
| `src/main/java/com/emme/assistant/ai/configuration/SpringAiAgeConfiguration.java` | Review | Optional AGE and JDBC-only composition |
| `src/main/java/com/emme/assistant/ai/configuration/SpringAiTraceConfiguration.java` | Refactor | Prefer Spring observations; retain durable trace adapter only when needed |
| `build.gradle.kts` | Review | Remove raw `okhttp` and unused Spring/provider dependencies after migration |

### 6.5 Tests and verification files

| File/group | Action | Purpose |
|---|---|---|
| `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiAdapterConsolidationArchitectureTest.java` | Extend | Enforce one adapter per framework capability |
| `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/ChatCompositionArchitectureTest.java` | Keep/extend | Reject duplicate chat composition and legacy fallback |
| `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/EmbeddingModelContractTest.java` | Extend | Enforce one embedding contract |
| `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAi*ConfigurationTest.java` | Update | Verify optional, qualified, mutually exclusive wiring |
| `modules/assistant/src/test/java/com/emme/assistant/ai/application/provider/*Test.java` | Update/delete | Preserve fallback only where policy requires it |
| `modules/assistant/src/test/java/com/emme/assistant/ai/application/semantic/*Test.java` | Update | Preserve thresholds, model identity, tenant/principal scope |
| `modules/assistant/src/test/java/com/emme/assistant/ai/application/tool/*Test.java` | Update | Verify authorization, confirmation, idempotency, and callbacks |
| `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/persistence/*Test.java` | Update | Verify JPA/`JdbcClient` decision per adapter |
| `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/*Test.java` | Update | Verify Spring AI delegation and advisor behavior |
| `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/redis/*Test.java` | Update | Verify TTL, safe misses, tenant scope, and serialization |
| `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/*Test.java` | Update | Verify checkpoint, resume, authorization, and idempotency |
| `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/PgVectorSemanticIntegrationTest.java` | Extend | Real PostgreSQL/pgvector evidence |
| `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/RedisSemanticIntegrationTest.java` | Extend | Real Redis projection evidence |
| `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/ConversationWorkflowCheckpointIntegrationTest.java` | Extend | Durable workflow evidence |
| `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/AiJobReconciliationClaimIntegrationTest.java` | Extend | Atomic job claim/recovery evidence |
| `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/adapter/out/graph/AgeGraphIntegrationTest.java` | Keep optional | AGE recommendation evidence |
| `libraries/ai-contracts/src/test/java/com/emme/ai/contracts/**` | Extend | Framework-neutral contract gate |
| `modules/ai-platform/src/test/java/com/emme/ai/platform/**` | Extend | Provider/admission/learning gates |
| `applications/emme-platform/src/test/java/com/emme/**` | Extend | Modulith, ArchUnit, dependency, and naming gates |
| `database/src/test/java/com/emme/database/Ai*MigrationContractTest.java` | Update | Migration and PostgreSQL invariant gates |

### 6.6 Related cross-module files requiring compatibility review

These files are not part of the first AI-only implementation slice, but they
must be checked before changing shared tenant/JDBC contracts:

| File/group | Review reason |
|---|---|
| `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/LiquibaseTenantSchemaMigrationAdapter.java` | Schema creation must remain JDBC/Liquibase-based |
| `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantIdentifierResolver.java` | Bootstrap lookup occurs during Hibernate tenant resolution |
| `modules/tenancy/src/main/java/com/emme/tenancy/configuration/BootstrapJdbcConfiguration.java` | Candidate for a clearly named bootstrap `JdbcClient` bean |
| `modules/tenancy/src/main/java/com/emme/tenancy/application/service/EnsureTenantMembershipService.java` | Direct JDBC currently sits in an application service; evaluate JPA after core-context proof |
| `modules/subscriptions/src/main/java/com/emme/subscriptions/adapter/in/messaging/consumer/SubscriptionProvisioningListener.java` | Dynamic tenant schema insertion may remain JDBC; verify JPA alternative |
| `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/adapter/TenantProvisioningPersistenceAdapter.java` | Already JPA; preserve as the metadata persistence baseline |
| `modules/identity/src/main/java/com/emme/identity/adapter/in/messaging/consumer/TenantRealmProvisioningListener.java` | Provisioning event and membership timing dependency |
| `modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/messaging/consumer/*.java` | Modulith event ordering and retry compatibility |

## 7. Migration phases

### Phase 0 — Baseline

- Preserve all unrelated dirty-worktree changes.
- Record current dependency, source, test, migration, and architecture state.
- Add a machine-checkable inventory of duplicate contracts and framework
  leakage.

### Phase 1 — Contracts

- Consolidate chat, embedding, graph, tool, extraction, and RAG contracts.
- Migrate callers one capability at a time.
- Keep compatibility types only until repository-wide caller search is clean.

### Phase 2 — Provider and Spring AI composition

- Make Spring AI the supported provider transport.
- Remove duplicate raw HTTP/provider wrappers after integration evidence.
- Retain fallback, admission, tenant policy, and deterministic mocks.

### Phase 3 — RAG, vectors, and Redis

- Use Spring AI `VectorStore` and advisors for retrieval mechanics.
- Keep tenant filtering, model identity, thresholds, and abstention custom.
- Keep PostgreSQL authoritative and Redis disposable.

### Phase 4 — Persistence review

- Review every `Jdbc*` adapter against the JPA-first matrix.
- Convert clear entity CRUD to Spring Data JPA.
- Retain `JdbcClient` only for proven PostgreSQL-specific or simpler SQL.
- Document the reason for every remaining JDBC adapter.

### Phase 5 — Events and workflows

- Use Spring Modulith for internal AI events and publication recovery.
- Keep Kafka only for selected externalized events.
- Restrict LangGraph4j to complex durable workflows and AGE to optional
  recommendation queries.

### Phase 6 — Safe deletion and final verification

- Delete duplicate contracts, raw providers, redundant wrappers, and unused
  dependencies only after all replacement evidence passes.
- Run the final enterprise matrix.

## 8. Verification model

### Per slice

1. Write the focused failing test.
2. Implement the minimum behavior.
3. Run focused tests and affected-module compilation.
4. Run affected Spotless/Checkstyle and architecture tests before committing.

### Per phase

- Module tests and compilation.
- Relevant PostgreSQL, Redis, or Kafka Testcontainers tests.
- Spring Modulith module verification.
- ArchUnit dependency and framework-leakage checks.
- Migration contract tests.

### Final gate

- Full compilation.
- Full unit and integration test suites with zero failures/skips.
- PostgreSQL/pgvector, Redis, and Kafka evidence.
- Modulith, architecture, naming, and dependency checks.
- Provider-offline startup and full application startup.
- E2E, coverage, performance baseline, `Spotless`, Checkstyle, and
  `git diff --check`.

## 9. Performance and failure constraints

- Measure query count, latency percentiles, connection-pool usage, JPA flush
  behavior, vector search latency, Redis hit rate, model admission wait, and
  event publication backlog before optimizing hot paths.
- JPA is preferred for normal CRUD; use projections, fetch plans, batching,
  and read-only transactions to avoid N+1 and excess entity loading.
- `JdbcClient` is preferred for atomic claim/lease SQL and PostgreSQL-specific
  operations where it reduces round trips or complexity.
- Redis failure produces a safe miss or temporary-state degradation.
- PostgreSQL failure fails durable operations and never returns false success.
- Missing or conflicting tenant/principal context fails closed.
- Model fallback occurs only for explicitly retryable provider-unavailable
  failures.
- Kafka or Modulith publication failure leaves durable publication pending for
  retry; no local queue duplication is added.

## 10. Definition of done

- One canonical application-facing contract exists per AI capability.
- Names clearly express capability, policy, or technology boundary.
- Spring AI owns supported model, tool, advisor, RAG, and vector mechanics.
- Spring Data JPA handles every suitable entity-backed persistence case.
- Every remaining `JdbcClient` use has a documented technical justification.
- Redis, PostgreSQL, Modulith, Kafka, AGE, and LangGraph4j responsibilities
  match their durability and operational guarantees.
- No framework types leak into `ai-contracts`.
- Tenant isolation, authorization, idempotency, audit, and observability are
  preserved.
- Focused, phase-level, and final enterprise gates pass.
- All changes are committed in logical units and pushed without bundling the
  existing unrelated dirty worktree.
