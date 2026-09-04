# Framework-first Refactoring Migration Ledger

| Field | Value |
|---|---|
| Design | [`2026-09-03-repository-framework-first-refactoring-design.md`](../specs/2026-09-03-repository-framework-first-refactoring-design.md) |
| Plan | [`2026-09-04-repository-framework-first-refactoring.md`](../plans/2026-09-04-repository-framework-first-refactoring.md) |
| Status | Phase A guardrails, AI contract slice, and tenancy membership/subscription slice implemented; remaining waves pending |
| Owner | Backend architecture / `eng` |
| Rule | Every replacement must preserve behavior, pass focused tests, and satisfy its deletion condition before the old path is removed |

## 1. Ledger rules

This file is the source of truth for the gradual repository-wide migration. A
row may move through these states:

```text
Inventory → Classified → Replacement tested → Callers migrated → Deletable → Deleted
```

No row may move to `Deleted` until caller search, configuration search, build
dependency search, focused tests, affected compilation, architecture tests,
and the relevant integration test are green. Database migrations already
deployed to an environment are never edited in place.

## 2. Active Gradle projects

Every active project is assigned an owner wave, even when it is intentionally
minimal today.

| Gradle project | Owner wave | Baseline disposition |
|---|---|---|
| `:platform` | H | Keep BOM/version constraints; narrow broad constraints after dependency analysis |
| `:applications:emme-platform` | A/H/I | Keep composition root and repository-wide architecture tests |
| `:modules:shared` | A/D/H | Keep specialized connection/search boundaries; reduce shared fixture and dependency leakage |
| `:modules:tenancy` | E | Keep bootstrap/schema JDBC; move business CRUD/policy to owned ports/JPA |
| `:modules:identity` | F/G | JPA-first identity state; typed Keycloak client and Redis rate-limit review |
| `:modules:clients` | G | JPA-first client aggregate persistence |
| `:modules:staffing` | G | Keep minimal boundary; no speculative persistence/web capabilities |
| `:modules:services` | G | JPA-first service/artist aggregate persistence |
| `:modules:appointments` | G | JPA-first with database-backed concurrency proof for collision invariant |
| `:modules:salon` | G | JPA-first profile/hours/policy persistence |
| `:modules:subscriptions` | E/G | JPA-first subscription state; tenancy-owned provisioning boundary |
| `:modules:documents` | G | JPA metadata; Spring AI/vector projection at adapter edge |
| `:modules:catalog` | G/H | JPA catalog state; specialized PostgreSQL search projection only where required |
| `:modules:booking` | H | Minimal package/API boundary; remove unused heavy conventions |
| `:modules:calendar` | F/G | JPA sync state; typed Google client and OAuth boundary |
| `:modules:notification` | F/G | JPA delivery state; typed provider senders and retry classification |
| `:modules:payment` | F/G | JPA payment/webhook state; provider gateway and state-transition audit |
| `:modules:assistant` | B/C/D/E/H | Spring AI, LangGraph, AI policy, tools, RAG, cache, and AI persistence |
| `:modules:audit` | G/H | Keep narrow durable audit boundary; avoid speculative dependencies |
| `:modules:ai-platform` | B/D | Spring AI provider/admission infrastructure and learning stores |
| `:libraries:functional` | H | Reduce one-use checked-exception wrappers |
| `:libraries:kernel` | H | Keep context/structured concurrency primitives; standardize bridges |
| `:libraries:testing` | H | Generic fixtures only; move feature fixtures to owners |
| `:libraries:test-containers` | H | Keep reusable provider container setup; use service connections when simpler |
| `:libraries:ai-contracts` | B | Framework-neutral capability contracts only |
| `:database` | I | Liquibase/PostgreSQL authority, RLS, extensions, indexes, migration contracts |
| `:libraries:observability-support` | H | Small shared observation conventions only |
| `:tools:e2e-provisioner` | F/I | Typed environment/provisioning boundary; no runtime-internal dependency |

`tools/ai-evaluation` is a repository tool but is not a Gradle project. It is
assigned to wave B/H for stable AI contract consumption and offline evaluation.

## 3. Production JDBC inventory

The following paths are every production Java file currently matching
`JdbcTemplate`, `JdbcClient`, `JdbcOperations`,
`NamedParameterJdbcTemplate`, or `BootstrapConnectionExecutor` under modules and
libraries. Configuration files are included because they establish the
boundary, even when they do not execute SQL themselves.

### 3.1 `modules/ai-platform`

| File | Status | Target / reason |
|---|---|---|
| `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/JdbcLearningCandidateEvaluationStore.java` | Classified | Review JPA for stable evaluation records; retain `JdbcClient` only for atomic transitions |
| `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/JdbcLearningCandidateStateStore.java` | Classified | Review JPA for state history; retain atomic claim/update SQL if required |
| `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/JdbcLearningCandidateStore.java` | Classified | Review JPA aggregate mapping; retain SQL only for proven concurrency/JSON behavior |

### 3.2 `modules/assistant`

| File | Status | Target / reason |
|---|---|---|
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/graph/JdbcAgeGraphClient.java` | Keep | Rename to `PostgresAgeGraphStore` if all callers are graph storage; AGE is specialized |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiJobStatusStore.java` | Classified | `PostgresAiJobStateStore`; retain atomic claim/lease SQL |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiToolIdempotencyStore.java` | Classified | `PostgresAiToolIdempotencyStore`; retain atomic idempotency claim/replay |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiTraceRecorder.java` | Classified | Try JPA durable trace records; keep SQL for JSONB/bulk append only if simpler |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcConversationWorkflowReviewAuditAdapter.java` | Classified | Review JPA audit records; retain direct SQL only for append/JSONB semantics |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcDesignImageMetadataRepository.java` | Classified | JPA metadata repository unless projection/query shape proves SQL simpler |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteArtifactRepository.java` | Classified | JPA quote artifact aggregate; retain JSONB SQL only if mapping adds complexity |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteReviewRepository.java` | Classified | JPA review task with optimistic locking |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteWorkflowRepository.java` | Classified | JPA workflow lifecycle with versioning; JSONB remains an evaluated exception |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcSemanticCacheAdapter.java` | Classified | Split durable metadata/hits from Redis/vector hot projection |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcSemanticReferenceSearchAdapter.java` | Keep | Rename to `PostgresHybridKnowledgeRetriever`; pgvector/FTS/RRF is specialized |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/JdbcLangGraphCheckpointSaver.java` | Keep | Rename to `PostgresLangGraphCheckpointStore`; JSONB/upsert/library contract |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/AiJobExecutorConfiguration.java` | Classified | Keep JDBC only through named job-state adapter; remove feature-level template wiring |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiAgeConfiguration.java` | Classified | Keep optional AGE wiring; no generic JDBC exposure |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfiguration.java` | Classified | Keep optional checkpoint bean; simplify graph composition |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiLearningConfiguration.java` | Classified | Keep learning store wiring behind application ports |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiTenantJdbcConfiguration.java` | Keep | Tenant-aware AI JDBC boundary; no application service injection |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiToolConfiguration.java` | Classified | Keep `JdbcClient` only behind idempotency adapter; one production bean path |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiTraceConfiguration.java` | Classified | Keep named trace adapter; JPA candidate assessed in Task 10 |

### 3.3 `modules/shared`, `modules/subscriptions`, and `modules/tenancy`

| File | Status | Target / reason |
|---|---|---|
| `modules/shared/src/main/java/com/emme/shared/persistence/jdbc/BootstrapConnectionExecutor.java` | Replacement tested | Lower-level managed connection callback is limited to bootstrap and tenant lifecycle callers; `JdbcTemplate` remains because `ConnectionCallback` is required |
| `modules/shared/src/main/java/com/emme/shared/persistence/jdbc/package-info.java` | Keep | Documents the narrow connection boundary |
| `modules/shared/src/main/java/com/emme/shared/search/HybridSearch.java` | Keep | Rename to `PostgresHybridKnowledgeRetriever` after callers migrate; exact RRF/FTS/pgvector query is specialized |
| `modules/subscriptions/src/main/java/com/emme/subscriptions/adapter/in/messaging/consumer/SubscriptionProvisioningListener.java` | Replacement tested | Calls `EnsureTenantSubscriptionUseCase` under `TenantContextHolder`; JPA repository owns duplicate check and operational failures propagate |
| `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/DatabaseRegistryAdapter.java` | Keep/Classified | Verify entity-manager cycle; retain bootstrap connection only if JPA cannot initialize safely |
| `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/LiquibaseTenantSchemaMigrationAdapter.java` | Keep | Dynamic schema/Liquibase boundary; JPA is not applicable |
| `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantIdentifierResolver.java` | Keep | Hibernate bootstrap resolver may execute before normal JPA routing |
| `modules/tenancy/src/main/java/com/emme/tenancy/application/service/EnsureTenantMembershipService.java` | Deleted | Duplicate removed; Identity owns the existing role/membership JPA model and now implements the tenancy provisioning use case |
| `modules/tenancy/src/main/java/com/emme/tenancy/configuration/BootstrapJdbcConfiguration.java` | Keep | Dedicated bootstrap data source; no general feature access |

## 4. Other framework-first inventories

### 4.1 Provider HTTP candidates

| Candidate family | Current paths | Target |
|---|---|---|
| Payment transport | `modules/payment/**/PaymentHttpClient.java`, `adapter/out/provider/{stripe,paypal,conekta,mercadopago}/**` | Typed `{Provider}PaymentGateway` plus `RestClient`/HTTP interface or justified SDK |
| Notification transport | `modules/notification/**/NotificationHttpClient.java`, `adapter/out/provider/{email,push,sms}/**` | `{Provider}{Channel}Sender` with typed DTOs and explicit retry/error policy |
| Google transport | `modules/calendar/**/GoogleHttpClient.java`, `adapter/out/google/client/**` | `GoogleCalendarGateway`/`GoogleSheetsGateway`; Spring OAuth/client or official auth SDK where safer |
| Keycloak transport | `modules/identity/**/KeycloakAdminClient.java` | `KeycloakIdentityGateway`; typed Spring client or official admin SDK after comparison |
| AI transport | `modules/assistant/**/AiHttpClient.java` | Spring AI model/client path; retain raw HTTP only for an unsupported provider capability |

### 4.2 AI contract candidates

| Candidate family | Current paths | Target |
|---|---|---|
| Chat | `libraries/ai-contracts/.../model/{ChatModel,ChatCompletionPort}.java`, `modules/assistant/.../application/port/out/{ChatCompletionPort,IdentifiedChatCompletionPort}.java` | One policy-facing chat capability; Spring AI `ChatClient` at adapter edge |
| Embedding | `libraries/ai-contracts/.../{embedding,model}/**`, `modules/assistant/.../EmbeddingModelPort.java` | One embedding contract selected by caller search |
| Tools | `libraries/ai-contracts/.../tool/**`, `modules/assistant/.../application/tool/**` | One assistant-owned tool catalog/gateway converted to Spring AI callbacks |
| RAG | `libraries/ai-contracts/.../rag/**`, assistant retrieval/answer ports | Separate `KnowledgeRetriever` from `RagAnswerService` |
| Workflow/graph | `libraries/ai-contracts/.../{workflow,graph}/**`, assistant workflow ports | Keep business workflow ports; hide LangGraph types in adapters |
| Semantic cache | library semantic contracts plus assistant semantic ports | Keep separate durable/hot ports only when durability differs |

### 4.2.1 Canonical contract migration status

| Compatibility name | Canonical replacement | Current callers / deletion condition |
|---|---|---|
| `embedding.EmbedTextUseCase` | `embedding.EmbeddingService` | No callers remain after the catalog and AI-platform embedding migration; deleted in Task 3 |
| `model.ChatModel` | `model.AiChatCompletion` for policy-facing use; Spring AI `ChatModel` remains provider-internal | Keep until Task 4 moves assistant callers behind the `ChatClient` router |
| `assistant.application.port.out.ChatCompletionPort` | `model.AiChatCompletion` | Keep until Task 4 migrates selector/composition callers and fallback tests |
| `assistant.application.port.out.EmbeddingModelPort` | `embedding.EmbeddingService` for raw embedding use; retain metadata-bearing semantic value internally until Task 6 | Keep until semantic callers are migrated without losing model/version/dimension checks |
| `rag.KnowledgeSearch` | `rag.KnowledgeRetriever` | No callers remain after the assistant retrieval/configuration migration; deleted in Task 3 |
| `tool.*` contracts in `libraries:ai-contracts` | assistant-owned `AiToolDefinition`, `AiToolGateway`, `AiToolInvocation`, and `AiToolResult` | Removed in Task 3 after repository caller search found no production callers |
| `workflow.WorkflowRuntime` | `ConversationWorkflow` and `QuoteWorkflow` | Removed in Task 3 after repository caller search found no production callers |

### 4.3 Build and fixture candidates

| Candidate | Current location | Target |
|---|---|---|
| Repeated `kernel` dependencies | `modules/booking/build.gradle.kts`, `modules/catalog/build.gradle.kts` | One declaration per project |
| Repeated security test dependency | `modules/assistant/build.gradle.kts` | One declaration |
| Repeated `emme.testing` application | module build files | Rely on `emme.java-library`/`emme.spring-module` convention chain |
| Repeated Modulith application | `applications/emme-platform/build.gradle.kts` | Rely on `emme.spring-application` once |
| Over-provisioned persistence plugin | `build-logic/src/main/kotlin/emme.persistence.gradle.kts` and placeholder modules | Split only if dependency analysis proves benefit |
| Feature fixture coupling | `libraries/testing/build.gradle.kts` and `src/testFixtures/java/**` | Move feature fixtures to owning modules |

## 5. Baseline verification commands

Run these before the first implementation slice and record the exact result in
the task report:

```bash
./gradlew :applications:emme-platform:test --tests com.emme.RepositoryFrameworkFirstInventoryTest --no-parallel --no-configuration-cache
./gradlew compileJava --no-parallel --no-configuration-cache
./gradlew test --no-parallel --no-configuration-cache
./gradlew dependencyAnalysis --no-parallel --no-configuration-cache
git diff --check
```

Container-backed PostgreSQL/Redis/Kafka tests require a working Docker
environment. Provider-offline startup and E2E tests require their configured
environment; a blocked environment gate must be recorded with the exact
failure rather than marked as passed.

## 6. Deletion checklist

Before deleting any ledger row's old implementation:

- [ ] `rg` finds no source caller, bean, import, reflection, test, or build dependency.
- [ ] Replacement unit tests pass.
- [ ] Replacement integration/migration tests pass when persistence/network behavior is involved.
- [ ] Architecture and module-boundary tests pass.
- [ ] Serialization/event/workflow compatibility is verified.
- [ ] Performance/query/latency baseline is not regressed.
- [ ] Rollback composition or feature flag is available until the release gate.
- [ ] Ledger status is changed to `Deleted` with commit SHA and date.
