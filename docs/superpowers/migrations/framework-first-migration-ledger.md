# Framework-first Refactoring Migration Ledger

| Field | Value |
|---|---|
| Design | [`2026-09-03-repository-framework-first-refactoring-design.md`](../specs/2026-09-03-repository-framework-first-refactoring-design.md) |
| Plan | [`2026-09-04-repository-framework-first-refactoring.md`](../plans/2026-09-04-repository-framework-first-refactoring.md) |
| Status | Phase A guardrails, AI contract slice, tenancy membership/subscription slice, and Calendar tenant-context slice implemented; remaining waves pending |
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

## 1.1 LangGraph complexity record

The two current LangGraph4j definitions are retained because they model
durable, resumable workflows rather than simple status transitions:

| Workflow | Branch or interrupt | Checkpoint/resume evidence | Boundary rule |
|---|---|---|---|
| Conversation | Intent/decomposition/routing/extraction/retrieval/tool/validation chain; validation branches to success, rejection, failure, clarification, or approval | Approval and clarification states pause at named gates and resume from the persisted checkpoint without re-running completed capabilities | Graph nodes receive application capabilities; authorization, repository access, external calls, and durable state remain outside the graph definition |
| Design quote | Required-slot extraction and calculation branch to staff review or response composition; staff review interrupts | `WAITING_FOR_STAFF` is checkpointed and resumes through `approval_gate` to `QUOTE_READY` | The graph coordinates workflow state only; quote calculation, authorization, persistence, and provider calls remain application/domain responsibilities |

Linear operations that only update lifecycle state and publish an internal
event are not graph candidates. They belong in an application service with the
existing Modulith event boundary. A future graph change must preserve the
checkpoint identity tuple (tenant, workflow, conversation, principal/actor,
namespace) and add a topology/resume test before implementation changes.

Stable ports are the canonical names in this ledger. Current and future
JPA/JdbcClient/Redis/Spring AI/provider adapters are implementation details
selected by configuration and composition-root wiring. Do not rename an
adapter merely to advertise its current provider, and do not make a provider
name part of a use-case, domain, API, or event contract.

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
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/graph/JdbcAgeGraphClient.java` | Keep | `AgeGraphStore` is the stable port; retain the current adapter because AGE is specialized |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiJobStatusStore.java` | Classified | `AiJobStatusStore` is the stable port; retain the current adapter for atomic claim/lease SQL |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiToolIdempotencyStore.java` | Classified | `AiToolIdempotencyStore` is the stable port; retain the current adapter for atomic idempotency claim/replay |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiTraceRecorder.java` | JdbcClient survivor: JSONB upsert, measured lower complexity | Three trace tables use redaction plus JSONB upserts; JPA would add three mappings without reducing the write path |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcConversationWorkflowReviewAuditAdapter.java` | JdbcClient survivor: append-only JSONB write | One tenant-scoped audit insert is smaller and clearer than a JPA entity/repository mapping |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcDesignImageMetadataRepository.java` | JdbcClient survivor: simple metadata write | Two small metadata statements do not benefit from JPA entity/repository indirection |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteArtifactRepository.java` | JdbcClient survivor: atomic JSONB upsert | Three-table extraction → draft → review-task write path; JPA natural-key lookup plus save adds entities, FK ordering, and a read/write race |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteReviewRepository.java` | JdbcClient survivor: atomic review transition | Conditional version update and decision append remain one transaction |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteWorkflowRepository.java` | JdbcClient survivor: atomic idempotency/version | Idempotent insert and versioned state update require database-native conflict handling |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcSemanticCacheAdapter.java` | Classified | Split durable metadata/hits from Redis/vector hot projection |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcSemanticReferenceSearchAdapter.java` | Keep | `SemanticReferenceSearchPort` is the stable port; retain the current adapter while pgvector reference search is specialized |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/JdbcLangGraphCheckpointSaver.java` | Keep | `WorkflowCheckpointStore` is the stable port; retain the current adapter for the JSONB/upsert/library contract |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/AiJobExecutorConfiguration.java` | Classified | Keep JDBC only through named job-state adapter; remove feature-level template wiring |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiAgeConfiguration.java` | Classified | Keep optional AGE wiring; no generic JDBC exposure |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfiguration.java` | Classified | Keep optional checkpoint bean; simplify graph composition |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiLearningConfiguration.java` | Classified | Keep learning store wiring behind application ports |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiTenantJdbcConfiguration.java` | Keep | Tenant-aware AI JDBC boundary; no application service injection |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiToolConfiguration.java` | Classified | Keep `JdbcClient` only behind idempotency adapter; one production bean path |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiTraceConfiguration.java` | Classified | Keep named `JdbcClient` trace adapter; JPA adds mappings without reducing the write path |

### 3.3 `modules/shared`, `modules/subscriptions`, and `modules/tenancy`

| File | Status | Target / reason |
|---|---|---|
| `modules/shared/src/main/java/com/emme/shared/persistence/jdbc/BootstrapConnectionExecutor.java` | Replacement tested | Lower-level managed connection callback is limited to bootstrap and tenant lifecycle callers; `JdbcTemplate` remains because `ConnectionCallback` is required |
| `modules/shared/src/main/java/com/emme/shared/persistence/jdbc/package-info.java` | Keep | Documents the narrow connection boundary |
| `modules/shared/src/main/java/com/emme/shared/search/HybridSearch.java` | Keep as port | Provider-neutral hybrid-search capability; application modules depend on this interface and do not see JDBC or PostgreSQL types |
| `modules/shared/src/main/java/com/emme/shared/search/postgres/PostgresHybridSearch.java` | JdbcClient survivor: PostgreSQL search | Exact pgvector/FTS/RRF query and embedding maintenance remain direct SQL until an equivalent measured framework path exists |
| `modules/subscriptions/src/main/java/com/emme/subscriptions/adapter/in/messaging/consumer/SubscriptionProvisioningListener.java` | Replacement tested | Calls `EnsureTenantSubscriptionUseCase` under `TenantContextHolder`; JPA repository owns duplicate check and operational failures propagate |
| `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/DatabaseRegistryAdapter.java` | Keep/Classified | Verify entity-manager cycle; retain bootstrap connection only if JPA cannot initialize safely |
| `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/LiquibaseTenantSchemaMigrationAdapter.java` | Keep | Dynamic schema/Liquibase boundary; JPA is not applicable |
| `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantIdentifierResolver.java` | JdbcClient boundary | Hibernate bootstrap resolver may execute before normal JPA routing; its scalar registry lookup uses the bootstrap-scoped `JdbcClient` |
| `modules/tenancy/src/main/java/com/emme/tenancy/application/service/EnsureTenantMembershipService.java` | Deleted | Duplicate removed; Identity owns the existing role/membership JPA model and now implements the tenancy provisioning use case |
| `modules/tenancy/src/main/java/com/emme/tenancy/configuration/BootstrapJdbcConfiguration.java` | Keep | Explicit bootstrap URL uses a dedicated data source; the normal profile reuses the primary core data source. `JdbcClient` serves scalar lookups while `JdbcTemplate` remains only for the managed raw-connection callback |

## 3.4 Detailed AI persistence classification

The rows below are the implementation gate for the AI persistence waves. `JPA
candidate` means the record has stable relational CRUD and should be migrated
to a module-private entity plus Spring Data repository when the mapping tests
pass. `JdbcClient survivor` means the SQL expresses an invariant that would be
split or obscured by JPA. `Lower-level boundary` is reserved for provider or
connection lifecycle APIs that are not CRUD repositories.

| File | Category | Data shape | Concurrency / transaction | Tenant / security behavior | Stable port / current adapter | Equivalence test |
|---|---|---|---|---|---|---|
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/graph/JdbcAgeGraphClient.java` | Lower-level boundary: AGE | AGE graph nodes/edges plus `ai_age_graph_registry` projection metadata | AGE extension load, graph creation, Cypher `MERGE`, and registry upsert execute in one projection transaction | Graph name and all node/edge predicates are tenant-scoped; AGE search path is local to the transaction | `AgeGraphStore` / `JdbcAgeGraphClient` | `AgeGraphIntegrationTest` |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiJobStatusStore.java` | JdbcClient survivor: atomic claim | `ai_job_state` lifecycle rows and JSON/text job payload | Conditional `UPDATE ... RETURNING`, lease recovery, and `FOR UPDATE SKIP LOCKED` must remain one atomic claim transaction | Tenant predicate plus `current_tenant_id()` and database RLS protect every state transition | `AiJobStatusStore` / `JdbcAiJobStatusStore` | `AiJobReconciliationClaimIntegrationTest` |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiToolIdempotencyStore.java` | JdbcClient survivor: atomic idempotency, JSONB | `ai_tool_idempotency` status, lease, and JSONB result payload | Unique-key insert/expired-lease reclaim, completion, and release are conditional transitions | Tenant and principal are part of every key and predicate; RLS remains authoritative | `AiToolIdempotencyStore` / `JdbcAiToolIdempotencyStore` | `JdbcAiToolIdempotencyStoreTest`, `QuoteWorkflowIdempotencyIntegrationTest` |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiTraceRecorder.java` | JdbcClient survivor: JSONB upsert, measured lower complexity | `ai_model_execution`, `ai_tool_call`, and `ai_semantic_execution` trace records with redacted JSONB | Three independent durable upserts keep redaction and JSONB serialization in one small adapter; JPA would add mappings without simplifying writes | Active execution context and database RLS provide tenant/principal isolation | `AiTraceRecorder` / `JdbcAiTraceRecorder` | `JdbcAiTraceRecorderTest`; add a live JSONB gate only if a smaller JPA design is proposed |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcConversationWorkflowReviewAuditAdapter.java` | JdbcClient survivor: append-only JSONB, measured lower complexity | Append-only `ai_conversation_workflow_review_decision` audit row with clarification JSONB | Single parameterized insert; no claim invariant and no entity lifecycle to manage | Explicit tenant/workflow/conversation/reviewer identity match plus RLS | `ConversationWorkflowReviewAuditPort` / `JdbcConversationWorkflowReviewAuditAdapter` | `JdbcConversationWorkflowReviewAuditAdapterTest`; add a live JSONB gate if mapping is reconsidered |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteArtifactRepository.java` | JdbcClient survivor: atomic JSONB upsert | `ai_extraction_result`, `quote_draft`, and `quote_review_task` records with JSONB attributes | Extraction and draft use tenant/workflow `ON CONFLICT` upserts; draft and review task depend on prior FK rows; JPA read-then-save would widen the race window and require custom conflict handling | Tenant is resolved from the active AI context and every record is RLS-protected | `QuoteArtifactRepository` / `JdbcQuoteArtifactRepository` | `JdbcQuotePersistenceAdapterTest`; retain a PostgreSQL concurrency gate for any future JPA reconsideration |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteReviewRepository.java` | JdbcClient survivor: atomic review transition | `quote_review_task` plus append-only `quote_review_decision` rows | Conditional reviewer update on task version and decision append are one `@Transactional` operation; JPA would require custom conditional update or widen the race window | Tenant and reviewer identity are validated before resolve/decision writes | `QuoteReviewRepository` / `JdbcQuoteReviewRepository` | `JdbcQuotePersistenceAdapterTest` plus reviewer ownership/version tests |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteWorkflowRepository.java` | JdbcClient survivor: atomic idempotency/version | `ai_workflow_run` lifecycle and JSONB state | Unique idempotency insert and expected-version update must remain atomic; JPA would require custom conflict handling to preserve semantics | Tenant, principal, and conversation predicates remain mandatory | `QuoteWorkflowRepository` / `JdbcQuoteWorkflowRepository` | `JdbcQuoteWorkflowRepositoryTest` plus workflow idempotency/version tests |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcSemanticCacheAdapter.java` | JdbcClient survivor: pgvector/JSONB | Expiring `ai_semantic_cache` vector rows, response JSONB, and hit counters | Vector nearest-neighbor query, idempotent upsert, and hit increment are database-native operations | Tenant/principal/context identity, model dimensions, expiry, and RLS are part of the query contract | `SemanticCachePort` / current adapter plus optional Spring AI/Redis hot adapter | `JdbcSemanticCacheAdapterTest`, `PgVectorSemanticIntegrationTest` |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcSemanticReferenceSearchAdapter.java` | JdbcClient survivor: pgvector/FTS/RRF family | Tenant reference embeddings from `ai_intent_reference` and `ai_tool_reference` | Vector distance, dimension/model predicates, and authorized-tool allowlist are clearer as parameterized SQL | Tenant and authorized tool keys are enforced in the query; table/column names come only from constants | `SemanticReferenceSearchPort` / `JdbcSemanticReferenceSearchAdapter` | `JdbcSemanticReferenceSearchAdapterTest`, `PgVectorSemanticIntegrationTest` |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcDesignImageMetadataRepository.java` | JdbcClient survivor: simple metadata CRUD, measured lower complexity | Stable `ai_design_image` metadata; binary content remains external | Ordinary parameterized insert/delete with database unique `(tenant_id, workflow_id)` constraint; no claim or vector invariant | Tenant/workflow/storage-key predicates remain explicit in SQL; RLS remains database authority | `DesignImageMetadataRepository` / `JdbcDesignImageMetadataRepository` | `JdbcDesignImageMetadataRepositoryTest` |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/JdbcLangGraphCheckpointSaver.java` | JdbcClient survivor: LangGraph checkpoint, JSONB | `ai_workflow_checkpoint` snapshots and `ai_workflow_run` JSONB state | LangGraph saver contract, checkpoint upsert, and workflow state update must stay compatible and ordered | Tenant, conversation, principal/staff reviewer access checks are explicit and RLS-backed | `WorkflowCheckpointStore` / `JdbcLangGraphCheckpointSaver` | `JdbcLangGraphCheckpointSaverTest`, `ConversationWorkflowCheckpointIntegrationTest` |
| `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/JdbcLearningCandidateStore.java` | JdbcClient survivor: JSONB/idempotent insert | `ai_learning_candidate` candidate record with JSONB evidence | Unique fingerprint/model upsert returns the existing identity without a read-then-write race | Tenant and principal are bound to the AI execution context and RLS | `LearningCandidateStore` / `JdbcLearningCandidateStore` | `JdbcLearningCandidateStoreTest` |
| `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/JdbcLearningCandidateStateStore.java` | JdbcClient survivor: atomic claim | Versioned status transition on `ai_learning_candidate` | Expected-version conditional update is one atomic state transition; a JPA read-modify-write would widen the race window | Candidate identity is checked against the bound tenant context before transition | `LearningCandidateStateStore` / `JdbcLearningCandidateStateStore` | `JdbcLearningCandidateStateStoreTest` |
| `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/JdbcLearningCandidateEvaluationStore.java` | JdbcClient survivor: JSONB/idempotent insert | `ai_learning_candidate_evaluation` versioned evidence and metrics JSONB | FK-guarded unique evaluation upsert avoids duplicate evaluation rows | Tenant is supplied from the bound context and protected by RLS | `LearningCandidateEvaluationStore` / `JdbcLearningCandidateEvaluationStore` | `JdbcLearningCandidateEvaluationStoreTest` |

The Salon notification-preference entity and repository remain an inventory
item, but are currently dormant: repository search found no application port,
adapter, service, or test caller. They are not deleted in this wave because the
database table and decomposition ADR still describe the capability; deletion
requires an explicit product/schema decision rather than a persistence-query
cleanup. If activated under tenant-schema routing, its `(tenant_id, channel)`
lookup should become a schema-local `findByChannel` contract.

## 4. Other framework-first inventories

### 4.0.1 Externalized event replay decisions

| Boundary | Status | Decision |
|---|---|---|
| Identity appointment membership consumer | Replacement tested | Consume `customerId` and `tenantId` from the externalized `AppointmentCreated` fact; do not require request-local `SecurityContext`. The existing membership use case provides idempotent duplicate handling. |
| Calendar staff synchronization listener | Replacement tested | Restore the event tenant context before schema-local JPA access and keep failure marking inside that context. |

### 4.0.2 Redis client/template decisions

| Boundary | Status | Decision and reason | Date |
|---|---|---|---|
| Tenancy HTTP rate-limit interceptor | Replacement tested | Use Spring Data `StringRedisTemplate` as the project standard for string keys/values, counters, TTLs, and simple Redis scripts. The previous broad `RedisTemplate<String, String>` accepted incompatible serializer configurations and was inconsistent with the other Redis adapters. | 2026-09-05 |
| AI operational state and live events | Classified | Keep `StringRedisTemplate`; Spring manages connection and serialization, while adapter ports keep providers replaceable from application code. | 2026-09-05 |
| Identity login-attempt limiter | Replacement tested | Keep `StringRedisTemplate` and its atomic Lua counter; return a rejected decision on `RedisConnectionFailureException` so the security boundary fails closed during a distributed-store outage. | 2026-09-05 |
| Spring AI semantic vector store and hot projection | Classified | Keep the isolated, Spring-managed Jedis `RedisClient` because the official Spring AI `RedisVectorStore` and native set/index invalidation require it. Do not introduce a second generic template solely for standardization. | 2026-09-05 |
| Spring AI semantic-cache metadata contract | Replacement tested | Centralize Redis metadata names and vector field types in `RedisSemanticCacheMetadata`; both the vector-store composition root and hot-store adapter consume the same contract. Provider-neutral cache ports remain free of Spring AI and Redis types. | 2026-09-05 |
| Tenant full-context test fixture | Replacement tested | `BaseTenantModuleTest` now owns only tenant creation/request/JWT mechanics; `EntitledTenantModuleTest` owns subscription and feature-flag setup, while Identity tests own role/membership setup. Unused salon, membership, and role collaborators were removed from the generic base. | 2026-09-05 |

### 4.0.3 LangGraph persistence wiring

| Boundary | Status | Decision and reason | Date |
|---|---|---|---|
| LangGraph checkpoint saver | Replacement tested | Qualify the composition-root dependency as `tenantJdbcClient`. Checkpoints are tenant-schema state and must not resolve ambiguously against the control-plane `coreJdbcClient`; the provider-neutral workflow ports remain unchanged. | 2026-09-05 |

### 4.0 Tenant-qualified lookup decisions

| Boundary | Status | Decision |
|---|---|---|
| Assistant conversation and pending-action aggregate reads | Replacement tested | Use connection-scoped `findById`; retain tenant IDs in commands/domain state and explicit child/list operations. Active pending actions use a schema-local status query ordered by `created_at, id` so API ordering is deterministic. Existing updates load managed JPA entities so inherited `@Version` remains effective. |
| Subscription existing aggregate save | Replacement tested | Use connection-scoped `findById`; retain tenant-keyed singleton lookup for provisioning and reads. |
| Identity membership | Keep explicit scope | Shared `emme_core` persistence and cross-tenant authorization require `tenant_id` in the lookup contract. |
| Calendar event links by appointment | Replacement tested | Use schema-local `findByAppointmentIdAndProvider`; tenant is selected at connection checkout and the forward `034-calendar-event-link-cardinality.sql` migration enforces one row per tenant/appointment/provider. Keep `findByAppointmentId` for operations that intentionally handle multiple providers. |
| Calendar sync state by provider | Replacement tested | Use schema-local `findByProvider`; the current tenant connection is selected before JPA access, while tenant identity remains domain/entity data for creation, response mapping, and RLS. |
| Calendar OAuth tokens | Replacement tested | Durable staff sync restores the event tenant context and enumerates schema-local tokens with JPA `findAll()`; interactive user/persona selection remains explicit. |
| Calendar spreadsheet links | Keep explicit scope | Tenant/spreadsheet business keys select external resources. |

### 4.1 Provider HTTP candidates

#### Provider HTTP transport policy

The provider HTTP migration is executed through the focused plan in
`docs/superpowers/plans/2026-09-05-external-provider-http-clients.md`.
Production provider adapters use one capability-scoped Spring `RestClient`
bean, while application ports and domain code remain provider-neutral. The
migration does not introduce a universal HTTP wrapper and does not change
provider-specific authentication, signing, idempotency, retry, timeout, or
error semantics.

Provider contract tests use `MockRestServiceServer` and assert the externally
visible request and response contract. A deliberately small `MockWebServer`
matrix remains for real transport behavior such as socket timeout, disconnect,
pooling, and selected wire-level behavior. `UserSession` remains an independent
OkHttp black-box E2E client. Test fixtures and transport-only tests may retain
OkHttp where that is their explicit purpose; generated `build/` output is never
part of the inventory.

| Candidate family | Current paths | Target |
|---|---|---|
| Payment transport | `modules/payment/**/PaymentHttpClient.java`, `adapter/out/provider/{stripe,paypal,conekta,mercadopago}/**` | Typed `{Provider}PaymentGateway` plus `RestClient`/HTTP interface or justified SDK |
| Notification transport | `modules/notification/**/NotificationHttpClient.java`, `adapter/out/provider/{email,push,sms}/**` | `{Provider}{Channel}Sender` with typed DTOs and explicit retry/error policy |
| Google transport | `modules/calendar/**/GoogleHttpClient.java`, `adapter/out/google/client/**` | `GoogleCalendarGateway`/`GoogleSheetsGateway`; Spring OAuth/client or official auth SDK where safer |
| Keycloak transport | `modules/identity/**/KeycloakAdminClient.java` | `KeycloakIdentityGateway`; typed Spring client or official admin SDK after comparison |
| AI/WhatsApp transport | `modules/assistant/adapter/out/client/whatsapp/WhatsAppReplyAdapter.java` | WhatsApp now uses a qualified provider-scoped Spring `RestClient`; Spring AI owns model transport, and no universal `AiHttpClient` wrapper remains |

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
| `model.ChatModel` | `model.AiChatCompletion` for policy-facing use; Spring AI `ChatModel` remains provider-internal | Deleted after caller search proved the library alias was only inherited by compatibility/provider adapters; see compatibility readiness below |
| `model.EmbeddingModel` | `embedding.EmbeddingService` | Deleted after caller search proved the library alias was only inherited by the deprecated composite provider; Spring AI's `EmbeddingModel` remains provider-internal |
| `assistant.application.port.out.ChatCompletionPort` | `model.AiChatCompletion` | Keep until Task 4 migrates selector/composition callers and fallback tests |
| `assistant.application.port.out.EmbeddingModelPort` | `embedding.EmbeddingService` | Deleted after all semantic callers migrated without losing model/version/dimension checks; see compatibility readiness below |
| `rag.KnowledgeSearch` | `rag.KnowledgeRetriever` | No callers remain after the assistant retrieval/configuration migration; deleted in Task 3 |
| `tool.*` contracts in `libraries:ai-contracts` | assistant-owned `AiToolDefinition`, `AiToolGateway`, `AiToolInvocation`, and `AiToolResult` | Removed in Task 3 after repository caller search found no production callers |
| `workflow.WorkflowRuntime` | `ConversationWorkflow` and `QuoteWorkflow` | Removed in Task 3 after repository caller search found no production callers |

### 4.3 Build and fixture candidates

| Candidate | Current location | Target |
|---|---|---|
| Repeated `kernel` dependencies | `modules/booking/build.gradle.kts`, `modules/catalog/build.gradle.kts` | One declaration per project |
| Repeated security test dependency | `modules/assistant/build.gradle.kts` | One declaration |
| Repeated shared test-fixture dependency | Modules applying `emme.testing` | Convention owns the normal test suite; integration-test fixtures remain explicit |
| Repeated `emme.testing` plugin application | Seven Spring modules | `emme.spring-module` → `emme.java-library` already applies the testing convention |
| Empty `emme.test-fixtures` application | `modules/subscriptions/build.gradle.kts` | Removed; the module has no fixture sources or consumers |
| Repeated Spring Boot test dependency | `modules/ai-platform/build.gradle.kts`, `modules/shared/build.gradle.kts` | Removed; `emme.testing` owns the standard test dependency |
| Unused shared dependency in generic testing | `libraries/testing/build.gradle.kts` | Removed from main and test-fixture configurations; no source usage exists |
| Repeated Modulith application | `applications/emme-platform/build.gradle.kts` | Removed; `emme.spring-application` owns Modulith and its test convention |
| Deterministic persistence/event verification | Tenancy, Identity, Subscriptions, Documents, Catalog, Calendar, Notification, Payment, Assistant, Appointments | Repository/adapter and event/listener suites pass; live provider replay/routing gates await Docker |
| Over-provisioned persistence plugin | `build-logic/src/main/kotlin/emme.persistence.gradle.kts` and placeholder modules | Split only if dependency analysis proves benefit |
| Feature fixture coupling | `libraries/testing/build.gradle.kts` and `src/testFixtures/java/**` | Move feature fixtures to owning modules |
| Dependency-analysis Java 25 compatibility | `gradle/libs.versions.toml`, `gradle/verification-metadata.xml` | Upgraded the analysis plugin to `3.18.0`; representative assistant/booking/catalog bytecode and advice tasks now pass |
| Spring AI OpenAI Swagger duplicate | `modules/ai-platform/build.gradle.kts` | Excluded legacy `swagger-annotations` from Spring AI OpenAI; Springdoc's Jakarta annotation artifact is now the sole runtime provider |
| Unused AI Platform aggregate starter | `modules/ai-platform/build.gradle.kts` | Removed the broad `spring-boot-starter` from the reusable library; deployable applications own the aggregate starter and AI Platform keeps specific APIs |
| Spring AI PgVectorStore versus hybrid ranking | `modules/shared/src/main/java/com/emme/shared/search/postgres/PostgresHybridSearch.java` | Retain the provider-neutral `HybridSearch` port and specialized SQL adapter; Spring AI covers vector KNN/metadata filters but not the required single-query FTS + pgvector + RRF projection |

## 5. Baseline verification commands

Run these before the first implementation slice and record the exact result in
the task report:

```bash
./gradlew :applications:emme-platform:test --tests com.emme.RepositoryFrameworkFirstInventoryTest --no-parallel --no-configuration-cache
./gradlew compileJava --no-parallel --no-configuration-cache
./gradlew test --no-parallel --no-configuration-cache
./gradlew :modules:assistant:computeActualUsageMain :modules:assistant:computeAdvice \
  :modules:booking:computeActualUsageMain :modules:booking:computeAdvice \
  :modules:catalog:computeActualUsageMain :modules:catalog:computeAdvice \
  --no-daemon --no-parallel --no-configuration-cache
git diff --check
```

There is no root `dependencyAnalysis` task in this Gradle build. The
dependency-analysis plugin provides per-project `computeActualUsage*` and
`computeAdvice` tasks. The plugin is pinned to `3.18.0`, with verification
metadata for its artifact graph. Representative assistant, booking, and
catalog tasks now complete on the Java 25 toolchain, including bytecode
analysis and advice generation. Generated advice is reviewed per module before
any dependency or convention declaration is changed; source inventory tests
remain the deterministic duplicate-declaration guard.

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

### 6.1 Compatibility deletion readiness

The status is intentionally explicit so a future deletion can be automated and
reviewed. `Pending` means callers or a prerequisite migration remain; `Ready`
means the inventory test must find no repository references before deletion;
`Deleted` means the implementation path and all repository references must be
gone.

| Implementation path | Status | Blocking condition or deletion evidence |
|---|---|---|
| `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiModelProvider.java` | Pending | Embedding and image compatibility callers still depend on the composite provider contract; legacy chat and document retrieval now use canonical capability ports, so migrate the remaining callers before deletion |
| `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/model/ChatModel.java` | Deleted | Library alias removed in `8009b011` on 2026-09-05; Spring AI's provider-internal `ChatModel` and the Assistant compatibility port remain distinct |
| `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/model/EmbeddingModel.java` | Deleted | Library alias removed in `9361a31a` on 2026-09-05; Spring AI's provider-internal `EmbeddingModel` remains distinct |
| `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/EmbeddingModelPort.java` | Deleted | Canonical `EmbeddingService` migration completed in `42e60a4a` on 2026-09-05; production/test caller searches are clean and Assistant focused tests pass |
| `modules/payment/src/main/java/com/emme/payment/configuration/PaymentHttpClient.java` | Deleted | HTTP-13 completed in `e976a397` on 2026-09-05; RestClient transport tests pass and no production/build references remain |
| `modules/notification/src/main/java/com/emme/notification/configuration/NotificationHttpClient.java` | Deleted | HTTP-13 completed in `e976a397` on 2026-09-05; RestClient transport tests pass and no production/build references remain |
| `modules/calendar/src/main/java/com/emme/calendar/configuration/GoogleHttpClient.java` | Deleted | HTTP-13 completed in `e976a397` on 2026-09-05; RestClient transport tests pass and no production/build references remain |

## Calendar event-link cardinality slice — 2026-09-05

- [x] Add a test-driven migration contract for the singular
      appointment/provider lookup.
- [x] Add a duplicate-data preflight and unique constraint covering tenant,
      appointment, and provider.
- [x] Include the migration in the studio Liquibase changelog.
- [x] Run the focused database migration contracts and catalog resolution test.
- [ ] Run the migration against PostgreSQL/Testcontainers and verify existing
      deployment data has no duplicate appointment/provider links.

The application port returns one Calendar event link for an appointment and
provider. The new forward migration protects that contract while still allowing
one link for each future provider. A duplicate preflight fails deployment with
an actionable error instead of silently selecting an arbitrary row.

## Assistant pending-action ordering slice — 2026-09-05

- [x] Add adapter coverage for deterministic active-action ordering.
- [x] Use Spring Data derived ordering by inherited `createdAt` and `id`.
- [x] Keep the application port provider-neutral and unchanged.
- [x] Run the focused Assistant persistence test.
- [ ] Add a composite index only after a production query-plan measurement.

The previous status query left row order unspecified. The ordered derived query
removes that future failure mode without introducing SQL or changing the domain
contract; expiration scans remain separate because they are operational claims,
not user-facing ordered history.

## Assistant managed-update slice — 2026-09-05

- [x] Add adapter coverage for existing Conversation and PendingAction updates.
- [x] Load existing records by ID and mutate managed JPA entities.
- [x] Keep new aggregate IDs null until JPA persist, matching the foundational
      JPA aggregate pattern.
- [x] Run the focused Assistant tests and full module check.
- [ ] Run live PostgreSQL optimistic-lock conflict coverage when Docker is
      available.

Rebuilding an existing versioned entity from a domain object without carrying
the persistence version can make Spring Data treat it as new. The managed update
path avoids that failure while keeping optimistic locking in the shared mapped
superclass and keeping JPA types out of the application contract.

## Notification and Payment managed-update slice — 2026-09-05

- [x] Add adapter regressions for existing Notification and Payment updates.
- [x] Keep new aggregate IDs null until JPA persistence assigns them.
- [x] Update managed JPA entities instead of rebuilding existing versioned rows.
- [x] Run both affected module checks, including Spotless and Checkstyle.
- [x] Re-run the repository-wide `check` with all managed-update slices applied.
- [ ] Run live PostgreSQL optimistic-lock conflict coverage when Docker is
      available.

Notification and Payment previously mapped every save to a new entity. Because
both entities inherit `@Version`, that detached reconstruction could lose the
version state and produce an insert/update conflict under real JPA behavior. The
adapters now use the same provider-neutral, managed-update pattern already
adopted by Assistant aggregates; no JPA types cross the application ports.

## CatalogItem managed-update slice — 2026-09-05

- [x] Add an adapter regression for an existing CatalogItem status update.
- [x] Keep new CatalogItem IDs null until JPA persistence assigns them.
- [x] Load existing items by ID and mutate the managed entity before saving.
- [x] Run the full Catalog check, including Spotless and Checkstyle.
- [x] Re-run the repository-wide `check` with all managed-update slices applied.
- [ ] Run live PostgreSQL optimistic-lock conflict coverage when Docker is
      available.

CatalogItem previously reconstructed a versioned `TenantOwnedEntity` with a
domain-assigned ID for every save. The adapter now follows the same managed
update boundary as the other mutable tenant aggregates. Catalog images are
create/delete-only and were deliberately not expanded with unused update code.
