# Repository-wide Framework-first Refactoring Design

| Field | Detail |
|---|---|
| Date | 2026-09-04 |
| Scope | Entire repository: applications, business modules, libraries, database, build logic, tools, deployment, infrastructure, scripts, tests, and documentation |
| Status | Draft for user review; no implementation authorized by this document |
| Related first-wave design | [AI contracts, AI platform, and assistant simplification design](2026-09-03-ai-contracts-platform-assistant-simplification-design.md) |
| Current build baseline | Spring Boot `4.1.0`, Spring Modulith `2.1.0`, Spring AI `2.0.1`, Java 25-compatible Gradle build |
| Migration style | Gradual, behavior-preserving, one capability at a time |

## 1. Decision summary

The repository should move toward a framework-first modular monolith with
explicit enterprise boundaries. The goal is not to replace every adapter with
the newest library. The goal is to remove code that only repeats mechanics
already provided by Spring, PostgreSQL, Redis, Kafka, or a supported provider,
while preserving Emme-specific policy, tenant isolation, authorization,
idempotency, audit, observability, and operational behavior.

The default decisions are:

1. Use Spring Data JPA for entity-backed CRUD, derived queries, projections,
   locking, transactions, and aggregate persistence where the relational model
   is stable.
2. Use Spring `JdbcClient` for small, direct SQL operations when SQL is clearer
   than an entity model, for atomic claims/leases, JSONB/vector/graph queries,
   PostgreSQL-specific functions, or dynamic tenant identifiers.
3. Keep lower-level `Connection`/Liquibase access in one tenancy/bootstrap
   infrastructure boundary. Do not pretend dynamic schema creation is ordinary
   repository CRUD.
4. Use Spring AI `ChatClient`, advisors, tool calling, structured output, and
   `VectorStore` for model/RAG mechanics. Keep model admission, tenant policy,
   prompt policy, abstention, and audit as application policy.
5. Use Spring Data Redis for cache, TTL, disposable live state, and narrowly
   defined coordination. PostgreSQL remains authoritative durable state.
6. Use Spring Modulith events for in-process module boundaries and durable
   publication/retry. Externalize to Kafka only where an external consumer or
   independent delivery boundary justifies Kafka's operational cost.
7. Use Spring `RestClient` and typed HTTP Service Clients for ordinary provider
   HTTP integrations. Retain an official provider SDK only when it materially
   reduces authentication, signing, serialization, or protocol risk.
8. Keep domain models and application ports framework-neutral. Framework types
   belong in adapters and configuration, not in `libraries:ai-contracts` or
   business domain packages.

This design is an audit and migration contract. It deliberately does not
change source code, rename files, remove dependencies, or alter database
migrations. Those changes require an approved execution plan and a separate
test-first slice.

## 2. Why this design is needed

The current code already has many good boundaries: domain models are generally
separate from JPA entities, most entity modules use Spring Data repositories,
package conventions protect module internals, and the AI area already has
architecture tests for canonical Spring AI adapters. The main problem is
inconsistent application of those patterns.

The audit found mechanics duplicated at several levels:

- custom JDBC adapters coexist with Spring Data repositories for similar
  entity-backed operations;
- provider clients repeatedly build OkHttp requests, parse maps, check status,
  and translate errors;
- Spring AI, legacy AI contracts, graph, tool, RAG, and provider wrappers
  overlap;
- test fixtures import concrete feature modules and expose feature-specific
  infrastructure to every module;
- Gradle conventions already provide testing, Modulith, persistence, and
  platform dependencies, but individual projects repeat plugins and
  dependencies;
- operational scripts, Liquibase, tenant provisioning, and runtime adapters
  each know pieces of schema/bootstrap behavior;
- historical names and compatibility tests still describe retired module
  boundaries.

These are not all the same problem. Some are safe deletion candidates; others
are intentional infrastructure exceptions. The migration therefore classifies
each candidate by responsibility, risk, and replacement evidence rather than
using a blanket `JdbcTemplate` or “rewrite everything” rule.

## 3. Audit evidence and scope

The inventory was derived from the Gradle project graph, version catalog,
source/test trees, dependency conventions, architecture tests, database
migrations, deployment files, and targeted searches for JDBC, JPA, HTTP,
Redis, vector, AI, event, build, and provider patterns.

### 3.1 Repository shape

| Area | Current inventory | Design treatment |
|---|---|---|
| Application | `applications/emme-platform` | Keep as composition/deployment root; remove feature mechanics from it |
| Business modules | `shared`, `tenancy`, `identity`, `clients`, `staffing`, `services`, `appointments`, `salon`, `subscriptions`, `documents`, `catalog`, `booking`, `calendar`, `notification`, `payment`, `assistant`, `audit`, `ai-platform` | Keep module ownership; migrate internals by capability |
| Libraries | `functional`, `kernel`, `testing`, `test-containers`, `ai-contracts`, `observability-support` | Reduce generic abstractions and feature coupling; preserve stable primitives |
| Database | Liquibase changelogs and PostgreSQL migrations | Keep database-owned constraints, RLS, extensions, generated columns, vector and graph objects |
| Build/platform | `platform`, `build-logic`, `build-logic-settings`, `gradle` | Centralize conventions and remove repeated project declarations |
| Tools | `tools/e2e-provisioner`, `tools/ai-evaluation` | Keep provider/environment mechanics at tool edges; share contracts only where stable |
| Operations | `deployment`, `infra`, `database/docker`, `performance`, `scripts`, `.github` | Standardize startup, migration, health, security, and final verification gates |
| Documentation | `docs`, `tasks`, architecture/package metadata | Make ownership, decisions, migration status, and stale names explicit |

### 3.2 Measured hotspots

The counts below are triage signals, not a claim that every matching file must
change.

| Hotspot | Observed signal | Interpretation |
|---|---:|---|
| Main Java sources | approximately 1,791 | Large enough that broad rewrites would be unsafe; use waves |
| JDBC-related production files | assistant 37, tenancy 8, ai-platform 6, shared 5, subscriptions 1 | Highest persistence simplification risk and highest opportunity |
| JPA repository files | concentrated in identity, assistant, tenancy, calendar, salon, services, and most entity modules | Existing framework-first path can be expanded instead of invented |
| Manual HTTP/OkHttp files | notification 13, calendar 9, payment 8, identity 3, assistant 3, ai-platform 2 | Repeated transport mechanics should be centralized; provider policy remains local |
| Entity-backed persistence adapters | present in most business modules | Preserve mapping where it protects domain/module boundaries; simplify only thin wrappers |
| Spring AI/Redis/vector-related files | assistant approximately 60, ai-platform approximately 13 | AI is the first high-value consolidation wave, not the only wave |
| Repeated Gradle declarations | duplicate `kernel` in booking/catalog; duplicate security-test in assistant; repeated convention plugins | Build logic itself is a source of avoidable maintenance |
| Shared test fixture coupling | `libraries:testing` imports identity, salon, subscriptions, tenancy, shared, JPA, Redis, OAuth2, OkHttp, Modulith | Fixtures are a repository-wide dependency bottleneck |

## 4. Non-negotiable target principles

### 4.1 Framework first, policy explicit

Frameworks own generic mechanics: persistence lifecycle, repository execution,
HTTP transport, serialization, model calls, vector retrieval, cache TTL,
transactional event publication, retries where correctly configured, and test
container wiring. Emme code owns business decisions: who may do what, which
tenant is active, what counts as idempotent, when a model may be used, what
data may be retrieved, how a payment state changes, and which failures are
retryable.

### 4.2 One owner per durable fact

PostgreSQL and its Liquibase schema are authoritative for durable business,
workflow, audit, and publication state. Redis is a cache/live/coordination
optimization and must tolerate eviction or restart. Kafka is a delivery/log
boundary, not a second source of truth for relational aggregates. Vector and
graph indexes are projections or specialized query surfaces unless a future
decision explicitly makes them authoritative.

### 4.3 Keep exceptions narrow and named

Remaining direct SQL must have an owning adapter and a documented reason. Names
should state intent, for example `TenantSchemaProvisioner`,
`AiJobClaimStore`, or `HybridSearchRepository`, rather than exposing an
incidental technology in an application port. A technology name is appropriate
at an infrastructure boundary (`JdbcTenantSchemaMigrationAdapter`), not as a
generic domain service name.

### 4.4 Keep provider substitution behind stable ports

Application ports and cross-module contracts are canonical. They name the
capability being requested, such as `AiJobStatusStore`,
`AiToolIdempotencyStore`, `KnowledgeRetriever`, or
`WorkflowCheckpointStore`; they do not name PostgreSQL, Redis, Kafka, JPA,
Spring AI, or a provider vendor. Concrete mechanism adapters may retain a
`Jdbc`, `Jpa`, `Redis`, `SpringAi`, or library-specific name inside an adapter or
configuration package when that detail is useful for wiring, but it is never a
required application-facing type. Provider selection belongs in the
composition root. Replacing an adapter or adding a second implementation must
not require changes to use cases, domain code, public APIs, or event contracts.

Do not rename an adapter merely to advertise the current provider. Rename it
only when the capability or responsibility is genuinely unclear, and keep the
stable port unchanged.

### 4.5 Preserve module boundaries

Do not collapse every JPA entity, repository, or mapper into a shared package.
The existing rule that entities and repositories are module-private is useful:
it prevents persistence coupling and makes future storage changes local. A
thin adapter is acceptable when it protects that boundary; it is a deletion
candidate only when the port adds no policy or module isolation value.

### 4.6 No compatibility layer without an exit condition

Compatibility aliases are temporary migration tools. Each must record callers,
replacement, deprecation milestone, deletion condition, and owner. A wrapper
that only renames or forwards calls should not become a permanent abstraction.

## 5. Technology selection matrix

| Scenario | Preferred mechanism | Keep custom code when | Main tradeoff / guard |
|---|---|---|---|
| Stable aggregate CRUD | Spring Data JPA repository + transaction | Entity graph cannot model the operation clearly or query is materially simpler in SQL | Watch N+1, flush timing, optimistic locking, and tenant filter behavior |
| Read-only list/detail | JPA projection, DTO query, fetch plan | PostgreSQL-specific operator or result shape dominates | Do not load full entities for read models |
| Optimistic/pessimistic state transition | JPA `@Version` or repository `@Lock` | Atomic conditional update/claim is clearer as one SQL statement | Test contention and retry semantics |
| Dynamic tenant schema/table identifier | `JdbcClient` or lower-level JDBC boundary | Never use JPA if schema is chosen dynamically before EMF routing | Validate identifiers against trusted registry; never bind identifiers as values |
| Tenant bootstrap / Hibernate resolver / Liquibase | Dedicated bootstrap `DataSource` + connection callback | This is a lifecycle constraint, not ordinary CRUD | Keep one boundary; do not leak bootstrap JDBC into services |
| Atomic claim/lease/idempotency | `JdbcClient` conditional SQL or JPA modifying query | SQL is a single atomic state transition and JPA would read-then-write | Cover duplicate/concurrent callers with PostgreSQL integration tests |
| JSONB, pgvector, FTS, RRF, AGE | `JdbcClient` named adapter; Spring AI `VectorStore` for standard vector search | Exact operators/index hints/result fusion are PostgreSQL-specific | Keep SQL small, typed at the port, and migration-backed |
| Standard vector retrieval/RAG | Spring AI `VectorStore` + advisors | Hybrid ranking, custom tenant policy, or projection semantics are not supported | Retrieval authorization/filtering remains application-owned |
| Cache and TTL | Spring Cache / `RedisCacheManager` / Spring Data Redis | Live event streams or atomic Redis primitives are required | Cache miss is safe; avoid using cache as durable state |
| Internal module event | Spring Modulith application event + publication registry | Synchronous call is required by invariant or transaction boundary | Define event schema and idempotent listeners |
| External durable event | Spring Modulith Kafka externalization or explicit Kafka adapter | Independent consumers, replay, partitioning, or throughput justify Kafka | Operate schema compatibility, retries, DLQ, and observability |
| Provider HTTP API | `RestClient` or typed `@HttpExchange` client | Official SDK materially reduces signing/auth/protocol errors | Keep provider-specific DTOs and error mapping at edge |
| OAuth2 client credentials/user delegation | Spring Security authorized-client manager/interceptor | Provider flow is non-standard or service-account signing requires provider library | Test token expiry, scopes, tenant isolation, and retries |
| Model transport/tool loop | Spring AI `ChatClient`, `ToolCallback`, advisors | Domain policy, admission, fallback, or deterministic evaluation is custom | Never let an LLM bypass authorization or tool policy |
| Durable complex workflow | Spring Modulith events plus explicit state, or LangGraph4j only for graph semantics | Workflow requires graph checkpoints/branching not simpler with state machine/events | Keep checkpoints durable and operations observable |
| Concurrency/context propagation | Java 25 `ScopedValue`/structured concurrency where supported | Legacy thread-bound framework APIs require a controlled bridge | Never silently lose tenant, principal, correlation, or tracing context |

Spring documents `JdbcClient` as the unified fluent JDBC API while retaining
lower-level callbacks for advanced operations. Spring Data JPA supplies
repository queries, projections, and locking. These documented capabilities
support the matrix; they do not justify converting dynamic-schema or
PostgreSQL-specialized code into JPA.

Sources: [Spring JDBC](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html), [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa.html), [Spring Data JPA query methods](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html), [Spring Data JPA projections](https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html), [Spring Data JPA locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html).

## 6. AI framework assessment

### 6.0 Version policy

“Use the latest implementation” means use the latest compatible stable patch
that is verified by this repository, not automatically import a milestone or
pre-release into a production refactor. The repository currently pins Spring
Boot `4.1.0`, Spring Modulith `2.1.0`, Spring AI `2.0.1`, and LangGraph4j
`1.8.25`. The official Spring Boot system-requirements page currently lists
`4.1.1` as the latest stable line, while the Spring AI compatibility page
supports the `2.0.x` line with Spring Boot `4.0.x` and `4.1.x`. Therefore:

1. Keep the refactor on the current tested major/minor lines.
2. Upgrade `4.1.0` to the latest stable compatible patch in a small platform
   maintenance change if the repository's compatibility gate passes.
3. Do not combine a Spring Boot, Spring AI, Modulith, database driver, or
   LangGraph major/minor upgrade with a behavior refactor.
4. Treat LangGraph4j `1.9.0-beta1` and any other pre-release as an evaluation
   candidate only; production remains on the latest verified stable release.
5. Record dependency upgrades in `platform`/the version catalog and verify
   API changes against official release notes before implementation.

Sources: [Spring Boot system requirements](https://docs.spring.io/spring-boot/4.2/system-requirements.html), [Spring AI getting started and compatibility](https://docs.spring.io/spring-ai/reference/getting-started.html), [LangGraph4j releases](https://github.com/langgraph4j/langgraph4j/releases).

The repository already uses Spring AI and LangGraph4j. The implementation
should consolidate around those existing capabilities instead of adding another
AI abstraction layer. The following classifications are intentionally specific:

`REPLACE` means the framework should own the mechanic after a compatibility
slice. `KEEP` means the code expresses Emme policy or a necessary integration
boundary. `REVIEW` means the code may be reduced, but only after behavior and
performance evidence. `DELETE` means a duplicate or compatibility artifact is
removed after caller migration.

### 6.1 Spring AI capability mapping

| Existing area | Current code | Classification | Target implementation |
|---|---|---|---|
| Chat transport | `modules/ai-platform/.../SpringAiChatModel.java`, `SpringAiModelProvider.java`, `modules/assistant/.../SpringAiChatConfiguration.java` | REVIEW → REPLACE mechanics | Configure provider `ChatModel` beans once; use `ChatClient` as the application-facing orchestration edge. Retain a small `ChatCompletionPort` only if it carries admission, fallback, tenant policy, or provider identity that `ChatClient` does not represent. |
| Provider registry | `SpringAiChatProviderRegistry`, `ChatModelSelector`, `IdentifiedChatCompletionPort` | KEEP policy, simplify construction | Rename selector to `AiChatClientRouter` if it selects named `ChatClient`s; move provider map/property parsing into configuration. The router owns model admission/fallback, not prompt transport. |
| Prompt and tenant policy | `PromptVersionAdvisor`, `TenantSecurityAdvisor` | KEEP | Keep as ordered Spring AI advisors. Make order explicit, fail closed when tenant/principal context is absent, and test that retrieval and tools receive the same policy context. |
| Tool invocation | `SpringAiToolCallbackProvider`, `AuthorizedAiToolGateway`, tool handlers | REPLACE registration mechanics; KEEP authorization | Expose Spring AI `ToolCallback`/`ToolCallbackProvider` from existing definitions. Keep `AuthorizedAiToolGateway` as the policy gate, idempotency boundary, audit, and result authority check. Do not expose raw business services as LLM tools. |
| Tool search | `ToolSearchToolCallingAdvisor`, `VectorToolIndex`, Redis tool vector store | REVIEW | Prefer Spring AI's tool-search advisor when tool count or semantic discovery justifies it. Keep explicit allow-lists and authorization before invocation. Disable vector tool search for small stable tool sets because direct callback registration is simpler and faster. |
| Structured extraction | `SpringAiNailDesignExtractor`, quote extraction configuration | REPLACE prompt/codec boilerplate; KEEP domain validation | Use `ChatClient.prompt().call().entity(NailDesignFeatures.class)` or the current supported structured-output API. Keep enum normalization, confidence/abstention rules, and validation in a domain/application component. |
| RAG answer flow | `SpringAiRagConfiguration`, `TenantScopedDocumentRetriever`, `RagAnswerProviderChain`, `RagQueryService` | REVIEW | Use `RetrievalAugmentationAdvisor` for retrieval augmentation and `ChatClient` for answer generation. Keep `TenantScopedDocumentRetriever` only for tenant authorization/filtering and bounded query policy. Delete `RagAnswerProviderChain` if it only forwards retrieval and completion calls; retain it if it implements explicit fallback/abstention policy and rename it `RagAnswerPolicy`. |
| Vector retrieval | `SpringAiRedisSemanticConfiguration`, `RedisVectorStore`, `RedisSemanticCacheHotStore` | REPLACE mechanics; REVIEW storage | Use Spring AI `VectorStore` for standard similarity search. Keep PostgreSQL `HybridSearch` for exact full-text + pgvector + reciprocal-rank fusion until an equivalent framework path is proven. Redis vector search remains an optional hot projection, never the durable semantic record. |
| Advisors | `PromptVersionAdvisor`, `TenantSecurityAdvisor`, retrieval/tool-search advisors | KEEP, consolidate | Compose one ordered advisor list per use case. Do not create a custom chain wrapper that duplicates `ChatClient` advisor sequencing. Add observation names and tests for ordering, context, and failure behavior. |
| Observability | `SpringAiObservationConventions`, `AiTraceRecorder`, Micrometer AI observers | REVIEW | Use Spring AI/Micrometer observations for model, embedding, vector store, and tool call metrics. Retain `AiTraceRecorder` only for durable business trace/audit data not covered by observations. |
| Semantic cache | `SemanticChatCache`, `JdbcSemanticCacheAdapter`, `RedisSemanticCacheHotStore`, invalidation publishers | KEEP policy; REPLACE storage plumbing where safe | PostgreSQL durable cache metadata/authorization stays explicit; Redis hot cache uses Spring Data Redis/Spring AI vector store. Simplify overloaded constructors to one production constructor with properties/policy records and explicit optional stores. |
| Embeddings | `SpringAiEmbeddingModel`, `SpringAiEmbeddingModelAdapter`, `AiEmbeddingAdapter`, `EmbeddingPort`, `EmbeddingModelPort` | DELETE duplicate contract layers after migration | Keep one application capability contract, preferably `EmbeddingModelPort` in the owning assistant boundary or a framework-neutral contract only when consumed across modules. The adapter wraps Spring AI `EmbeddingModel`; do not maintain both `EmbeddingPort` and `EmbeddingModelPort` for the same operation. |
| Image/vision | `SpringAiVisionModel`, `AiCaptionImageAdapter`, `NailDesignExtractionPort` | REVIEW | Keep one extraction port for domain behavior. Use Spring AI multimodal `ChatClient`/model configuration for transport; remove a provider wrapper if it adds no policy or output normalization. |

Spring AI provides a fluent `ChatClient`, structured entity mapping, advisors,
tool callbacks/tool calling, retrieval augmentation, vector-store integrations,
and observations. Those features should replace hand-written orchestration
mechanics, not policy. Sources: [Spring AI ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html), [Spring AI tools](https://docs.spring.io/spring-ai/reference/api/tools.html), [Spring AI advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html), [Spring AI RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html), [Spring AI vector stores](https://docs.spring.io/spring-ai/reference/api/vectordbs.html), [Spring AI observability](https://docs.spring.io/spring-ai/reference/observability/index.html).

### 6.2 AI contract consolidation and exact names

The current contracts contain parallel names and framework-neutral types that
represent overlapping responsibilities. This table is the proposed end state;
the old names remain only during a controlled migration.

| Current name(s) | Canonical name | Action and reason |
|---|---|---|
| `ChatModel`, `ChatCompletionPort`, assistant `ChatCompletionPort`, `IdentifiedChatCompletionPort` | `AiChatCompletion` for policy-facing application use; Spring AI `ChatClient` at adapter edge | Keep one application port only if callers need provider selection/admission/fallback. Delete the duplicate assistant contract and make provider identity a request/result value, not a second interface. |
| `EmbeddingModel`, `EmbeddingPort`, assistant `EmbeddingModelPort`, `EmbedTextUseCase` | `EmbeddingService` or `EmbeddingPort` (choose one based on cross-module usage) | Keep one verb-oriented contract. Prefer `EmbeddingService` when it is an application use case; prefer `EmbeddingPort` when it is strictly an outbound adapter. Do not keep both with identical signatures. |
| `AiModelProvider`, `SpringAiModelProvider`, `SpringAiChatModel`, `SpringAiVisionModel` | `SpringAiModelAdapter` only where a boundary is needed | Delete composite provider compatibility wrapper after callers move to capability-specific beans. Composite “provider” types hide which capability is used and force unnecessary implementations. |
| `KnowledgeQuery`, `KnowledgeSearch`, `SemanticReferenceSearchPort`, `RagAnswerPort`, `KnowledgeDocument` | `KnowledgeRetriever` for retrieval; `RagAnswerService` for answer policy | Separate retrieval from answer generation. Use Spring AI `Document` only in the adapter; keep a framework-neutral result at the domain boundary when required for authorization/audit. |
| `ToolDefinition`, assistant `AiToolDefinition`, `ToolGateway`, assistant `AiToolGateway`, `ToolPolicy`, `ToolRisk`, assistant `AiToolRisk` | `AiToolDefinition`, `AiToolGateway`, `AiToolRisk` | Keep one set in the owning assistant contract. Convert to Spring AI `ToolCallback` at the adapter edge. Delete library copies after repository-wide caller migration. |
| `WorkflowRuntime`, `ConversationWorkflowPort`, `QuoteWorkflowResumePort`, graph contracts | `ConversationWorkflow` and `QuoteWorkflow` application ports | Keep business workflow ports; LangGraph4j types must stay in the adapter/configuration package. Delete generic graph contracts that only mirror LangGraph4j state/checkpoint APIs. |
| `SemanticCache`, `SemanticResponseCache`, `SemanticCachePort`, `SemanticCacheHotStore` | `SemanticResponseCache` plus `SemanticCacheHotStore` only if two-tier behavior is real | Keep a durable policy port and a hot-store port only when their durability semantics differ. Remove aliases with identical methods. |
| `NailDesignExtractionPort`, assistant quote extraction ports, duplicate nail feature records | `NailDesignExtractor` and one canonical feature record | Keep domain extraction and validation in assistant; move shared record to `ai-contracts` only if another module genuinely consumes it. |

Recommended package names after migration:

```text
libraries/ai-contracts/.../model       # only cross-module, framework-neutral values
modules/ai-platform/.../provider        # Spring AI provider adapters and admission
modules/assistant/.../ai/application    # Emme policy, use cases, and ports
modules/assistant/.../ai/adapter/in     # HTTP/events/tool callbacks entering assistant
modules/assistant/.../ai/adapter/out    # Spring AI, JPA/JdbcClient, Redis, AGE, LangGraph
modules/assistant/.../ai/configuration  # composition only; no business decisions
```

The implementer must perform caller search before each deletion. A proposed
rename is not permission to change public API behavior or serialized event
names without compatibility tests and a migration note.

### 6.3 Spring AI configuration simplification

The existing configuration has many package-private overloads used to support
tests and optional beans. That makes the composition root look like multiple
production construction paths. The target is one production path and test
fixtures that inject collaborators directly.

| File | Current issue | Target change |
|---|---|---|
| `modules/assistant/.../configuration/SpringAiChatConfiguration.java` | Repeated overloads for registry and completion construction; manual `ChatClient` map assembly | Keep one bean method per production bean. Extract `AiChatProperties`/`AiExecutionProperties` records. Build named `ChatClient`s once, then pass an immutable ordered advisor list to `AiChatClientRouter`. Test with direct constructors rather than package-private overloads. |
| `modules/assistant/.../configuration/SpringAiRagConfiguration.java` | Rebuilds a provider registry and completion chain separate from chat configuration | Inject the canonical `AiChatClientRouter`/chat capability. Build only the retrieval advisor and RAG policy bean here. Do not create a second provider selection path. |
| `modules/assistant/.../configuration/SpringAiToolConfiguration.java` | Overloaded gateway/idempotency factory and direct JDBC optionality | Keep one gateway bean. Model idempotency as an explicit `AiToolIdempotencyStore`; provide a no-op only for a documented local/test profile. Move `JdbcClient` construction into a named persistence adapter configuration. |
| `modules/assistant/.../configuration/SpringAiRedisSemanticConfiguration.java` | Direct Jedis client plus Spring AI Redis vector store and custom hot-store plumbing | Prefer Boot/Spring Data Redis connection configuration. Keep a direct Redis client only if a required atomic primitive has no clean template/connection API. Separate vector-store projection from key/value hot cache. |
| `modules/assistant/.../configuration/SpringAiLangGraphConfiguration.java` | Multiple graph beans, generic `CompiledGraph<AgentState>` qualifiers, and direct JDBC checkpoint wiring | Keep one opt-in workflow configuration. Name beans by capability (`conversationWorkflowGraph`, `quoteWorkflowGraph`, `workflowCheckpointStore`). Keep LangGraph types inside this configuration and adapter package. |
| `modules/assistant/.../semantic/SemanticChatCache.java` | Many overloaded constructors and mixed durable/hot policy | One constructor; inject `SemanticCachePolicy`, `Clock`, `SemanticCacheStore`, optional hot store, and metrics through a small composition record or explicit collaborators. Separate cache-key/policy calculation from storage. |
| `modules/ai-platform/.../SpringAiModelProvider.java` | Composite compatibility provider duplicates capability adapters | Migrate callers to capability-specific adapters and delete after architecture test confirms no usage. |

### 6.4 Target Spring AI bean and advisor flow

The target composition is one framework path per capability:

```text
provider properties
        ↓
Spring AI ChatModel / EmbeddingModel beans
        ↓
named ChatClient beans (one per admitted provider)
        ↓
AiChatClientRouter
  ├─ model admission / timeout / fallback policy
  └─ selected ordered advisors
       1. TenantSecurityAdvisor
       2. PromptVersionAdvisor
       3. RetrievalAugmentationAdvisor (RAG use cases only)
       4. ToolCallingAdvisor or ToolSearchToolCallingAdvisor (tool use cases only)
        ↓
ChatClient.prompt().call().content() / entity(...) / stream()
```

Implementation rules for this flow:

- `ChatModel` and `EmbeddingModel` are provider mechanics. They do not become
  application-wide business services.
- `ChatClient` is the fluent request boundary. Use its structured entity output
  for typed extraction rather than maintaining a parallel JSON parsing loop.
- Use ordinary Spring AI tool callback registration for a small, stable tool
  set. Use `ToolSearchToolCallingAdvisor` only when semantic tool discovery is
  worth the Redis/vector index and additional latency. For ordinary tool loops,
  use Spring AI's tool-calling support and keep the Emme gateway as the
  authorization/idempotency callback.
- Build the advisor list once per use-case configuration. Do not append advisors
  in several layers and accidentally duplicate security, retrieval, or tracing.
- The tenant advisor may enrich or reject a request, but it must not be the
  only authorization check: tool handlers and retrieval adapters re-check the
  application context at their own boundary.
- Retrieval augmentation may supply context; it does not decide whether a
  document is authorized. `TenantScopedDocumentRetriever` remains only if that
  policy cannot be represented by the vector store metadata filter alone.
- Use `entity(...)` for extraction only after validating model output and domain
  invariants. A syntactically valid DTO is not a trusted business result.
- Use streaming only for user-visible incremental responses. Durable workflow,
  audit, and payment/tool state still use completed, idempotent commands.
- Use Spring AI observations for model/embedding/vector/tool telemetry and
  keep durable `AiTraceRecorder` writes for business audit or replay fields
  that observations intentionally do not persist.

The current configuration already contains the essential advanced pieces, but
constructs some of them through repeated overloads and a second RAG provider
chain. The first Spring AI slice should therefore be composition cleanup and
caller migration, not a new AI feature.

## 7. LangGraph4j assessment

LangGraph4j is already used for conversation and quote workflows, including
compiled graphs, conditional edges, interrupts, resume behavior, and a
tenant-aware PostgreSQL checkpoint saver. It is therefore not an unused
dependency to remove. It is also not a replacement for every application
service or event flow.

### 7.1 What LangGraph should own

- graph topology and node/edge transitions;
- resumable interrupt points for clarification, approval, or staff review;
- workflow thread/checkpoint identity;
- graph execution and state continuation;
- graph-specific retry/interrupt behavior where supported by the library.

### 7.2 What LangGraph must not own

- appointment authorization, payment transitions, tenant resolution, or
  repository access;
- direct provider HTTP calls;
- durable business truth outside the checkpoint/workflow adapter;
- tool authorization or idempotency;
- generic event publication that Spring Modulith already handles.

The current `ConversationWorkflowGraph` and `QuoteWorkflowGraph` follow the
correct direction: nodes coordinate boundaries and do not calculate prices or
authorize users. Preserve that separation.

### 7.3 LangGraph file-level target

| Existing file | Target action | Reason |
|---|---|---|
| `modules/assistant/.../workflow/ConversationWorkflowGraph.java` | `REVIEW`; rename to `ConversationWorkflowDefinition` only if it clarifies that it compiles a graph | Keep topology. Move repeated status/key constants to a typed workflow state key class. Do not wrap every graph method in another abstraction. |
| `modules/assistant/.../workflow/QuoteWorkflowGraph.java` | `REVIEW`; possibly `QuoteWorkflowDefinition` | Keep because the approval interrupt and resume path are graph semantics. Keep quote calculation/authorization outside nodes. |
| `modules/assistant/.../workflow/LangGraphConversationWorkflowAdapter.java` | Rename to `LangGraphConversationWorkflow` if it is the sole adapter | Keep boundary translation, identity validation, resume authorization, and error mapping. Remove duplicate exception wrapping only when it loses no context. |
| `modules/assistant/.../workflow/LangGraphQuoteWorkflowCapability.java` | `KEEP` as `LangGraphQuoteWorkflow` | It is the capability adapter that invokes the quote graph; retain only if the application port is still needed. |
| `modules/assistant/.../workflow/LangGraphQuoteWorkflowResumeAdapter.java` | Merge into the workflow adapter if it shares no independent policy | Avoid two classes for one graph lifecycle; keep separate only when start and resume have different ownership or deployment boundaries. |
| `modules/assistant/.../workflow/JdbcLangGraphCheckpointSaver.java` | Keep the current adapter behind `WorkflowCheckpointStore` | JPA is not suitable for LangGraph's checkpoint contract, JSONB state, atomic upsert, graph keys, and tenant-aware security predicates. `JdbcClient` is the correct narrow boundary until an equivalent implementation is proven. |
| `modules/assistant/.../workflow/TenantAwareCheckpointSaver.java` | Keep as policy decorator; consider merging validation with store only if tests remain clearer | It enforces tenant/context identity before library execution. Framework/library code cannot infer Emme authorization. |
| `modules/assistant/.../configuration/SpringAiLangGraphConfiguration.java` | Simplify to one opt-in configuration | Prevent duplicate graph construction and bean ambiguity. Keep `app.ai.langgraph.enabled` and quote feature gating. |

### 7.4 Decision rule for replacing LangGraph with Spring mechanisms

Before retaining or expanding a graph, compare it against a simple state
machine plus Modulith events:

| Workflow characteristic | Use simple application state + Modulith | Use LangGraph4j |
|---|---|---|
| Linear command with a few durable statuses | Yes | No |
| One transactional state transition and one event | Yes | No |
| Human approval/clarification with resumable execution | Maybe; use explicit state if only one pause | Yes when multiple branches/interrupts exist |
| Conditional multi-step AI/tool workflow | Maybe; measure complexity | Yes |
| Long-running graph with checkpoint/resume and branches | No | Yes |
| External integration retry or delivery | Modulith/Kafka/outbox semantics | No; graph adds unnecessary machinery |
| Pure deterministic business calculation | Domain service | No |

The current quote and conversation workflows meet the “keep, but constrain”
case. Any new graph must demonstrate branching, interruption, or checkpoint
value in its design and integration test; otherwise use a domain service plus
Spring Modulith event.

## 8. Repository-wide change inventory

This is the implementer-facing inventory. `Review` means inspect every matching
class in the named path; it does not mean mechanically rewrite every file.
`Keep` means no migration unless a later slice proves duplication. `Delete`
means delete only after caller, test, architecture, and runtime evidence.

### 8.1 Module matrix

| Project | Primary files/areas | Planned change | Priority |
|---|---|---|---|
| `modules/shared` | `persistence/jdbc/BootstrapConnectionExecutor.java`, `search/HybridSearch.java`, shared test fixtures and `build.gradle.kts` | Keep one low-level connection boundary for Liquibase/bootstrap; keep `HybridSearch` as PostgreSQL-specific until equivalent vector/FTS/RRF support exists; reduce shared dependencies and test-fixture coupling | P0/P1 |
| `modules/tenancy` | `BootstrapJdbcConfiguration.java`, `EnsureTenantMembershipService.java`, `TenantIdentifierResolver.java`, `DatabaseRegistryAdapter.java`, `LiquibaseTenantSchemaMigrationAdapter.java`, `adapter/out/persistence/*`, `build.gradle.kts` | Move bootstrap JDBC behind `TenantBootstrapStore`/`TenantSchemaProvisioner`; keep dynamic schema and resolver JDBC; convert registry/membership CRUD to JPA where lifecycle permits; validate identifiers; stop swallowing provisioning failures; remove unnecessary direct JDBC from application services | P0 |
| `modules/identity` | `KeycloakAdminClient.java`, `KeycloakUserAuthenticationAdapter.java`, JPA adapters/repositories, Redis rate limiter, `IdentityClientConfiguration.java` | Replace manual ordinary Keycloak HTTP mechanics with typed `RestClient`/HTTP interface or official SDK after auth/error comparison; keep identity policy and provider mapping; review Redis limiter against Spring Data Redis atomic operations; keep JPA aggregates | P1 |
| `modules/clients` | `adapter/out/persistence/*`, application ports/services, `build.gradle.kts` | Review thin JPA mapping adapters; retain module-private entities and repositories; remove redundant service support only when it has no policy | P1 |
| `modules/staffing` | package/API boundary and `build.gradle.kts` | Keep intentionally minimal; do not add persistence/web/integration plugins until implementation exists; define future owner before sharing entities | P2 |
| `modules/services` | `ServicePersistenceAdapter.java`, `ArtistPersistenceAdapter.java`, `SpringData*Repository.java`, application services | JPA-first baseline; use projections/derived queries and transactions; remove only pass-through ports/adapters after module boundary test proves safe | P1 |
| `modules/appointments` | `AppointmentCollisionAdapter.java`, `AppointmentPersistenceAdapter.java`, `SpringDataAppointmentRepository.java`, mutation services | Replace load-and-check collision logic with an indexed existence query and/or PostgreSQL exclusion/atomic constraint where required; use JPA first, SQL/constraint only for concurrency proof; preserve authorization in service | P0/P1 |
| `modules/salon` | booking policy/profile/hours adapters and repositories | JPA-first; consolidate repeated “find or throw/map” support only if it is module-local and readable; do not move entities to shared | P1 |
| `modules/subscriptions` | `SubscriptionPersistenceAdapter.java`, `SpringDataSubscriptionRepository.java`, `SubscriptionProvisioningListener.java` | JPA for subscription state; make provisioning listener idempotent and failure-visible; replace direct bootstrap JDBC injection with tenancy-owned provisioning port/event | P0/P1 |
| `modules/documents` | document persistence, content/storage ports, AI retrieval integration | JPA for document metadata; Spring AI `Document` at retrieval adapter edge; keep blob/storage provider mechanics in adapter; standardize embedding/index projection events | P1 |
| `modules/catalog` | `CatalogItemPersistenceAdapter.java`, repositories, `build.gradle.kts` | JPA for catalog entities; retain PostgreSQL/vector SQL only for search projection; remove repeated `kernel` dependencies and unnecessary persistence/web plugins if code does not use them | P1 |
| `modules/booking` | package/API placeholders and `build.gradle.kts` | Keep minimal boundary; remove repeated `kernel`; remove persistence/web/integration plugins until real capability requires them; do not create speculative repositories | P0 |
| `modules/calendar` | Google clients/adapters, `GoogleClientConfiguration.java`, persistence adapters/repositories | JPA for sync/link/token state; typed `RestClient`/HTTP interface or Google SDK for provider transport; keep OAuth/service-account policy and sync idempotency; remove generic `GoogleHttpClient` after migration | P1 |
| `modules/notification` | provider classes under `adapter/out/provider`, `NotificationHttpClient.java`, configuration, delivery services | Introduce one typed HTTP transport convention using `RestClient`; keep provider-specific DTO/auth/error mapping; consolidate retry/timeout/observability policy; do not hide provider differences behind a giant generic client | P1 |
| `modules/payment` | Stripe/PayPal/Conekta/MercadoPago providers, `PaymentHttpClient.java`, webhook persistence and services | JPA for payment/webhook state; typed provider adapters; verify placeholder `authorize`/`capture` behavior before refactoring; model idempotency and webhook signatures explicitly; retain SDK only when it reduces protocol risk | P0/P1 |
| `modules/assistant` | `ai/**`, conversation JPA adapters/repositories, tool handlers, Redis, graph, 37 JDBC-related files, `build.gradle.kts` | Execute AI consolidation first; JPA for conversation/participant/event/action aggregates; JdbcClient for atomic AI jobs/tools/checkpoints/JSONB/pgvector; Spring AI ChatClient/advisors/tools/RAG; LangGraph only for complex resumable workflows; remove duplicate contracts/wrappers | P0 |
| `modules/audit` | package/API boundary and build metadata | Keep as a narrow durable audit boundary; use Modulith events and owning persistence when implementation is added; avoid speculative framework dependencies | P2 |
| `modules/ai-platform` | Spring AI provider adapters/configuration, three JDBC learning stores, admission scheduler | Keep provider/admission policy; simplify Spring AI capability beans; use JPA only if learning records become stable aggregates; retain JdbcClient for atomic candidate claims/state transitions if JPA cannot express one-statement semantics; document each survivor | P0/P1 |

### 8.2 AI contracts, libraries, and test infrastructure

| Project | Files/areas | Planned change |
|---|---|---|
| `libraries/ai-contracts` | `model/*`, `embedding/*`, `rag/*`, `tool/*`, `workflow/*`, `graph/*`, `semantic/*`, duplicate extraction/value records | Make the library framework-neutral and small. Consolidate overlapping model, embedding, tool, RAG, semantic-cache, and workflow interfaces using the names in section 6.2. Remove Spring AI/LangGraph/JPA/Redis types and generic graph APIs that merely mirror third-party APIs. |
| `libraries/kernel` | `TenantContext`, `CorrelationId`, `TenantExecutionContextScope`, `AiExecutionContextScope`, `StructuredParallelTaskRunner` | Keep context and structured concurrency primitives. Standardize one context propagation API; maintain an explicit bridge only where Spring/MDC/legacy thread-local integrations require it. Test virtual-thread, async, scheduled, and event listener propagation. |
| `libraries/functional` | Throwing and unchecked functional interfaces plus `Throwables` | Search all callers. Replace one-use wrappers with JDK/Spring callback types where semantics are identical. Keep a small set only where checked-exception composition is materially clearer, with interruption preservation tests. |
| `libraries/observability-support` | support package and build metadata | Make it a small cross-cutting observation convention library. Do not move feature-specific metrics here. Prefer Spring Boot/Micrometer/Spring AI observations and retain custom conventions only for stable business dimensions. |
| `libraries/test-containers` | container abstractions and lifecycle helpers | Keep provider-specific container setup here; use Spring Boot Testcontainers service connections where they remove custom property wiring. Keep reusable PostgreSQL/Redis/Kafka fixtures only when they are used by multiple modules. |
| `libraries/testing` | `BaseSpringModuleTest.java`, `BaseWebTest.java`, `MockKeycloakAdminClientConfig.java`, `TestBootstrapJdbcConfig.java`, `TestSecurityConfig.java`, feature-specific fixture dependencies | Split generic fixtures from feature fixtures. Move identity/tenancy/salon/subscription fixtures to owning modules. Replace concrete-client subclassing and broad mocks with protocol fakes. Keep bootstrap JDBC fixture only for tenancy/bootstrap tests. |
| `tools/e2e-provisioner` | provisioning client/configuration and tests | Reuse the same tenant provisioning contract and typed HTTP client conventions; keep it as an environment boundary, not a reason to expose runtime internals. |
| `tools/ai-evaluation` | Python evaluation scripts/configuration | Keep evaluation independent from runtime provider mechanics; consume stable request/result contracts and add compatibility tests when AI contract names change. |

### 8.3 Build logic and dependency declarations

| File/area | Planned change | Why |
|---|---|---|
| `build-logic/src/main/kotlin/emme.java-library.gradle.kts` | Keep automatic testing convention; document that feature modules must not reapply `emme.testing` | The convention already applies it transitively; repeated application obscures ownership and can create duplicate configuration. |
| `emme.spring-module.gradle.kts` | Keep common Spring/Modulith baseline; verify whether every placeholder module needs Modulith events | Preserve consistent modules, but avoid forcing unused capabilities where a minimal boundary is intentional. |
| `emme.spring-application.gradle.kts` | Keep app-level Boot starters and Modulith; remove feature-specific configuration from application code over time | Composition root should wire modules, not own business mechanics. |
| `emme.persistence.gradle.kts` | Split into `emme.jpa-persistence` and `emme.liquibase-persistence` only if usage analysis proves the current bundle over-provisions projects | Current plugin adds JPA, Liquibase, PostgreSQL runtime, and Testcontainers together. Splitting reduces placeholder-module cost but adds convention names. |
| `emme.messaging.gradle.kts` | Keep Kafka/Modulith Kafka opt-in; add a lighter `emme.modulith-events` path if needed | Kafka should not be pulled into every event-producing module. |
| `emme.testing.gradle.kts` | Keep common unit test dependencies; stop exporting feature-specific fixtures from the global convention | The current convention makes every module depend on `libraries:testing`, increasing build and architectural coupling. |
| `emme.integration-testing.gradle.kts` | Keep explicit integration suite; use service connections and module-owned fixtures | Make container dependencies explicit and avoid every integration test inheriting unrelated infrastructure. |
| `emme.quality.gradle.kts` | Keep final quality gates; add targeted module gates and architecture checks | Preserve Spotless/Checkstyle/JaCoCo/Sonar while making gradual slices fast and failures attributable. |
| `platform/build.gradle.kts` | Keep BOM centralization; change broad `api` constraints to the narrowest appropriate scope after dependency analysis; audit OkHttp, JDBC-template ShedLock provider, and unused SDKs | Version centralization is good, but broad constraints hide actual ownership and keep obsolete mechanics available. |
| `settings.gradle.kts` | Keep explicit project graph; add convention validation for placeholder modules and dependency cycles | Project ownership should be visible and machine-checked. |
| module `build.gradle.kts` files | Remove duplicate declarations; remove plugins not used by source; use capability-specific dependency aliases | Concrete known duplicates: booking/kernel x7, catalog/kernel x3, assistant/security-test x2, repeated `emme.testing`, app-level repeated Modulith application. |

### 8.4 Database, deployment, and operational files

| Area | Planned change | Guard |
|---|---|---|
| `database/src/main/resources/db/changelog/**` | Keep Liquibase as schema authority. Add ownership metadata and migration contract tests; consolidate only truly duplicate tables/migrations | Never rewrite deployed migrations. New changes require forward migration and rollback/repair procedure. |
| PostgreSQL RLS, tenant registry, dynamic schemas | Keep database enforcement and trusted schema validation | Application checks are defense in depth, not a substitute for RLS/policies. |
| pgvector, generated `tsvector`, HNSW, AGE objects | Keep database-specific indexes/extensions and explicit adapters | JPA/VectorStore must not erase important index/operator behavior. |
| `database/docker/run-migrations.sh` | Keep environment bootstrap script; centralize slug/schema validation and make failure states visible | Runtime and script must share the same rules without duplicating unsafe interpolation. |
| `deployment/compose/**` | Remove stale `.bak` files after validation; standardize profiles for app/PostgreSQL/Redis/Kafka/observability | Verify local, CI, and E2E behavior before deletion. |
| `infra/kubernetes/**` | Keep health probes, migration jobs, secret references, and resource boundaries; remove duplicate environment wiring | Validate startup ordering and failure recovery in a disposable environment. |
| `.github/workflows/**` | Standardize fast PR checks, phase integration checks, security/dependency checks, and final release checks | Do not make every PR wait on the full enterprise matrix if affected-module gates are sufficient. |
| `performance/locust/**` | Establish before/after baselines for JPA, JdbcClient, Redis, vector retrieval, provider HTTP, and workflows | Performance claims require measurements, not a framework preference. |
| `docs/**`, `tasks/**`, package-info and architecture tests | Correct stale module names (`studio`, `customer`, `workforce` where active names differ), maintain ADR/migration ledger, and align tests with actual boundaries | Historical migration tests remain only when they protect supported compatibility. |

## 9. Persistence refactoring rules and file naming

### 9.1 Standard persistence shape

For a normal module-owned aggregate, use this shape:

```text
<module>/...
  domain/<Aggregate>.java
  application/port/out/<Aggregate>Repository.java
  application/service/<Verb><Aggregate>Service.java
  adapter/out/persistence/entity/<Aggregate>Entity.java
  adapter/out/persistence/repository/SpringData<Aggregate>Repository.java
  adapter/out/persistence/adapter/<Aggregate>PersistenceAdapter.java
  adapter/out/persistence/mapper/<Aggregate>PersistenceMapper.java  # only when mapping is non-trivial
```

The following naming rules remove ambiguity:

| Avoid | Use | Rule |
|---|---|---|
| `JdbcFooRepository` for ordinary CRUD | `SpringDataFooRepository` | Repository technology is explicit only inside the adapter package |
| `FooPersistenceAdapter` that only delegates one method | `FooRepositoryAdapter` or remove it | Keep the port boundary only when it protects module policy/ownership |
| `FooManager`, `FooHelper`, `FooUtils` | `FooPolicy`, `FooValidator`, `FooMapper`, `FooFactory`, or a verb-oriented service | Name the responsibility, not the size of the class |
| `*HttpClient` with raw OkHttp calls | `StripePaymentGateway`, `GoogleCalendarGateway`, `TwilioSmsSender`, etc. | Application code depends on capability; provider transport stays in the named adapter |
| `JdbcConnectionExecutor` used everywhere | `BootstrapConnectionExecutor` | Make the exceptional lifecycle purpose visible and prevent general reuse |
| `EnsureTenantMembershipService` injecting bootstrap JDBC | `TenantMembershipService` + `TenantMembershipRepository` | Application service expresses membership policy; persistence adapter owns SQL/JPA |
| `JdbcAgeGraphClient` | `AgeGraphStore` port plus the current adapter | Keep AGE-specific SQL local; do not rename solely because the current database is PostgreSQL |
| `JdbcSemanticReferenceSearchAdapter` | `SemanticReferenceSearchPort` plus the current adapter | Expose retrieval intent; retain `JdbcClient` only while the measured PostgreSQL query is needed |
| `JdbcAiJobStatusStore` | `AiJobStatusStore` port plus the current adapter | Contract states capability; the mechanism remains replaceable behind the port |
| `JdbcLangGraphCheckpointSaver` | `WorkflowCheckpointStore` port plus the current adapter | Keep the library interface hidden from application packages without prescribing a provider |
| `SpringAiModelProvider` | capability-specific `SpringAiChatClientAdapter`, `SpringAiEmbeddingAdapter`, `SpringAiVisionAdapter` | Avoid one composite type that forces unrelated capabilities together |

### 9.2 JPA-first review procedure for every JDBC class

For each production JDBC-related file, the implementer records a decision in a
small migration ledger before changing it:

| Question | If yes | If no |
|---|---|---|
| Is the table a stable module-owned aggregate with an entity mapping? | Try Spring Data JPA repository, projection, `@Version`, or `@Lock` | Continue JDBC assessment |
| Is the operation ordinary create/read/update/delete or a simple list? | JPA is the default | Continue |
| Does the operation require a dynamic schema/table identifier before JPA can route? | Keep a named JDBC/bootstrap adapter | Continue |
| Does correctness depend on one atomic conditional SQL statement, claim, lease, or upsert? | Keep `JdbcClient` if a JPA modifying query is less clear or cannot return required state | Try JPA modifying query/locking |
| Does it use JSONB, pgvector, generated FTS, AGE, RRF, or PostgreSQL-only syntax? | Keep `JdbcClient` in a specialized adapter | Try JPA/VectorStore |
| Is the JDBC implementation longer than a clear JPA repository plus mapping? | Prefer JPA after query-count/performance tests | Keep SQL if still clearer |
| Does tenant/RLS behavior depend on connection session setup? | Keep connection-aware adapter and integration tests | Standard JPA transaction path is eligible |

The expected outcome is not “zero JDBC files”; it is a short, justified list of
JDBC boundaries. The design baseline expects these categories to survive:

- tenant bootstrap data source, Hibernate tenant resolver, database registry
  cycle breaking, and Liquibase schema provisioning;
- LangGraph checkpoint upsert/load with JSONB and tenant predicates;
- atomic AI job/tool/idempotency/learning candidate claims;
- PostgreSQL hybrid retrieval, pgvector, FTS/RRF, and AGE traversal;
- any proven RLS/session initialization that cannot safely occur through the
  normal entity manager lifecycle.

### 9.3 Specific persistence migrations

| Current path | First target | Why this is the right first experiment |
|---|---|---|
| `modules/assistant/.../JdbcAiJobStatusStore.java` | Keep `AiJobStatusStore` as the stable port; retain the current adapter with `JdbcClient` only for atomic claim/state transitions; assess JPA for ordinary lookup/history | Job claiming is concurrency-sensitive; a read-then-write JPA rewrite could create duplicate work. Split the operation by semantics rather than by provider name. |
| `modules/assistant/.../JdbcAiToolIdempotencyStore.java` | Keep `AiToolIdempotencyStore` as the stable port; retain the current adapter with `JdbcClient` for the atomic transition; assess JPA for non-atomic history if present | Idempotency requires one unique/conditional operation and authoritative replay behavior. |
| `modules/assistant/.../JdbcAiTraceRecorder.java` | Keep `AiTraceRecorder` as the stable port; assess a module-private JPA adapter if trace records are stable entities; retain `JdbcClient` only for measured bulk/JSONB append | Durable trace rows are likely entity-backed; do not retain JDBC just because the payload is AI-related. |
| `modules/assistant/.../JdbcQuote*Repository.java` | Keep quote workflow/artifact/review ports stable; assess JPA adapters after mapping review; retain SQL for JSONB document payloads only where projection is materially simpler | Quote lifecycle, versioning, and reviewer ownership benefit from entity state and repository locking. |
| `modules/assistant/.../JdbcSemanticCacheAdapter.java` | Keep `SemanticCachePort` stable; assess JPA for durable metadata and Spring AI/Redis for hot similarity lookup; retain specialized SQL for the measured hybrid query | Separates durable facts from retrieval optimization without coupling callers to a store. |
| `modules/ai-platform/.../JdbcLearningCandidate*Store.java` | Keep learning ports stable; assess JPA for candidate/evaluation records and retain `JdbcClient` only for atomic claim/update if required | Candidate state resembles an aggregate, but worker concurrency may require a single SQL transition. |
| `modules/shared/search/HybridSearch.java` | Keep the `KnowledgeRetriever` port stable and retain `HybridSearch` as the specialized adapter until an equivalent is proven | Spring AI `VectorStore` does not automatically reproduce Spanish FTS + pgvector + reciprocal-rank fusion and tenant filters. |
| `modules/tenancy/.../EnsureTenantMembershipService.java` | `TenantMembershipRepository` backed by JPA or a tenancy-owned `JdbcClient` adapter | Removes database mechanics from application policy and makes new-tenant failure behavior testable. |
| `modules/tenancy/.../DatabaseRegistryAdapter.java` | JPA registry repository only after bootstrap/entity-manager cycle test; otherwise named bootstrap adapter | The current connection executor exists to break a real initialization cycle. Verify before removing it. |
| `modules/tenancy/.../LiquibaseTenantSchemaMigrationAdapter.java` | Keep `JdbcTenantSchemaMigrator` | Liquibase and dynamic schemas are infrastructure boundaries; JPA cannot replace them. |
| `modules/subscriptions/.../SubscriptionProvisioningListener.java` | Publish a typed tenancy provisioning command/event and call a tenancy-owned port | A subscription listener must not own schema SQL or swallow all exceptions. |

## 10. Provider HTTP and external integration simplification

### 10.1 Standard client boundary

The target provider shape is:

```text
application/port/out/PaymentGateway.java
adapter/out/provider/stripe/StripePaymentGateway.java
adapter/out/provider/stripe/StripeApi.java          # @HttpExchange interface, if suitable
adapter/out/provider/stripe/StripeProperties.java
configuration/StripeClientConfiguration.java
```

`StripePaymentGateway` owns payment semantics, idempotency keys, provider
status mapping, signature/error interpretation, and observability dimensions.
`StripeApi` owns typed HTTP serialization and transport. A shared low-level
HTTP wrapper is not introduced because it would merely hide differences while
preserving duplicated DTO/error logic.

### 10.2 Exact provider mappings

| Existing path | Stable port / adapter shape | Decision |
|---|---|---|
| `modules/payment/.../PaymentHttpClient.java` | Delete after callers move to provider-specific `*PaymentGateway` + typed API | It is a generic OkHttp forwarding wrapper with little policy value. |
| `modules/notification/.../NotificationHttpClient.java` | Delete after callers move to `SendGridEmailSender`, `TwilioSmsSender`, `VonageSmsSender`, etc. | Keep a shared request policy component only if it centralizes timeout/metrics without hiding provider DTOs. |
| `modules/calendar/.../GoogleHttpClient.java` | Delete after `GoogleCalendarApi`/`GoogleSheetsApi` typed clients are proven | Google-specific auth and response mapping remain in calendar adapters. |
| `modules/assistant/.../AiHttpClient.java` | Delete if Spring AI owns supported model transport; keep only for an unsupported provider protocol | Do not maintain raw AI HTTP beside Spring AI without a capability gap record. |
| `modules/identity/.../KeycloakAdminClient.java` | `KeycloakIdentityGateway` backed by typed `RestClient`/HTTP interface or official admin SDK | Compare token/admin endpoint coverage, retries, error mapping, and tenant realm behavior before selecting SDK. |
| `modules/calendar/.../GoogleCalendarClient.java` and `GoogleSheetsClient.java` | `GoogleCalendarGateway`, `GoogleSheetsGateway` | Preserve sync policy; replace request construction and parsing mechanics. |
| `modules/payment/.../{Stripe,PayPal,Conekta,MercadoPago}Provider.java` | `{Provider}PaymentGateway` | “Provider” is vague; “PaymentGateway” states the application capability. Verify unfinished authorize/capture behavior before renaming. |
| `modules/notification/.../provider/{email,push,sms}/*Provider.java` | `{Provider}{Channel}Sender` | Names distinguish delivery channel and make dependency injection unambiguous. |

Spring Framework provides fluent `RestClient`, reactive `WebClient`, and typed
HTTP Service Clients through `@HttpExchange`; Spring Security provides an
authorized-client manager/interceptor for supported OAuth2 flows. Use these as
the default transport/auth mechanics, then retain official SDKs only when they
reduce provider protocol risk. Sources: [Spring REST clients](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html), [Spring HTTP Service Clients](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface), [Spring Security OAuth2 authorized clients](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html).

### 10.3 Provider failure contract

Every gateway must distinguish invalid request, authentication/configuration,
rate limit, timeout/network, provider conflict/idempotent replay, and provider
server failure. Only explicitly retryable failures enter Spring Retry or the
module's delivery policy. A generic `catch (Exception)` that logs and returns
success is prohibited for payments, provisioning, audit, or durable events.

## 11. Events, Redis, and workflows

### 11.1 Spring Modulith first

For an in-process module interaction, publish a typed event after the owning
transaction. Use the Modulith publication registry for durable publication and
retry. Listener methods must be idempotent and must not directly reach into
another module's entities or repositories.

| Current pattern | Target |
|---|---|
| Direct cross-module service invocation for asynchronous work | Typed application event and listener |
| Manually duplicated publication/retry table logic | Spring Modulith event publication infrastructure |
| Kafka for every internal notification | Modulith event; Kafka only for external boundary |
| Redis pub/sub as durable workflow/event state | Redis live signal only; PostgreSQL/Modulith owns durability |
| Listener catches all exceptions and continues | Classify duplicate/no-op versus failure; retry or surface failure |

Spring Modulith documents a publication registry that records publications in
the business transaction and tracks completion; its Kafka integration can
externalize selected events. Source: [Spring Modulith events](https://docs.spring.io/spring-modulith/reference/events.html).

### 11.2 Redis responsibilities

| Data | Owner | Allowed Redis behavior |
|---|---|---|
| Semantic hot cache | PostgreSQL metadata/vector projection plus Redis hot copy | Evictable, TTL, safe miss, versioned key |
| Login attempt rate limit | Redis atomic counter/script or Spring Data Redis operation | Fail-closed or documented degraded mode; no silent unlimited access |
| AI live events | Redis stream/pub/sub if live delivery needs it | Reconnect/replay behavior must be explicit; not durable business truth |
| Workflow/job state | PostgreSQL | Redis may accelerate status reads but cannot be sole owner |
| Vector search | Spring AI Redis `VectorStore` only as configured projection | Tenant metadata filters and embedding contract must be verified |

Avoid direct Jedis construction when Boot/Spring Data Redis already supplies a
managed connection. Retain a native Redis client only for a proven primitive
that the Spring abstraction cannot express cleanly.

## 12. Findings, root causes, tradeoffs, and future failure modes

| ID | Finding | Root cause | If left unchanged | Preferred correction | Tradeoff |
|---|---|---|---|---|---|
| F-01 | JDBC is used for both ordinary persistence and genuine bootstrap/SQL-specialized work | No explicit selection matrix and historical adapter growth | More code paths, inconsistent transactions, harder tenant debugging, and unsafe read-then-write claims | JPA-first review ledger; retain named `JdbcClient` exceptions | Some JPA mappings and integration tests are required before deletion |
| F-02 | Tenant bootstrap JDBC leaks into application/listener code | Bootstrap lifecycle concerns were placed beside business policy | New tenants can fail ambiguously; schema SQL may be duplicated or interpolated unsafely | Tenancy-owned bootstrap ports and one JDBC infrastructure boundary | More explicit interfaces and event wiring up front |
| F-03 | Manual OkHttp clients repeat request/status/JSON mechanics | Provider integrations were implemented independently | Inconsistent timeout, auth, retry, error, and observability behavior | RestClient/HTTP interfaces or vetted SDK per provider | Typed DTOs still need provider-specific code; SDKs can add lock-in |
| F-04 | Spring AI and compatibility contracts overlap | Framework adoption occurred incrementally without a removal milestone | Every new AI feature must understand several interfaces and adapters | One capability contract, Spring AI at edge, caller-driven deletion | Temporary migration aliases and test updates |
| F-05 | Spring AI configuration has overloads that duplicate construction paths | Tests use package-private production factory overloads | Bean behavior differs from tests; optional wiring becomes difficult to reason about | One production composition path; constructor-injected fakes | Test setup becomes slightly more explicit |
| F-06 | LangGraph and ordinary application orchestration can overlap | Graphs were added before a simple-state comparison was documented | Unnecessary checkpoint/state complexity and harder operational recovery | Use graph only for branching/interruption/checkpoint value | Simple workflows lose a flexible graph escape hatch, intentionally |
| F-07 | Generic test fixtures depend on concrete feature modules | Shared testing library accumulated convenient imports | Build graph and architecture tests become coupled; changes ripple globally | Generic fixtures in `libraries/testing`; feature fixtures in owning modules | Some test imports move and local fixtures are duplicated temporarily |
| F-08 | Build conventions and module scripts repeat dependencies/plugins | Convention plugins are not treated as the single declaration owner | Dependency drift, slow builds, confusing capability ownership | Remove duplicates and validate plugin capability usage | Gradle convention refactor has its own compatibility risk |
| F-09 | Database, scripts, and runtime each know tenant migration rules | Operational bootstrap evolved separately from runtime | Environment-specific provisioning failures and schema drift | One documented rule set; database remains authority; scripts call it safely | Cross-language validation may need contract tests rather than shared code |
| F-10 | Historical names and migration tests remain in active architecture metadata | Renames were applied to code but not all docs/tests | New contributors use wrong module names; false architectural signals | Update metadata and retain only supported compatibility tests | Historical context must move to an ADR/migration ledger |
| F-11 | Payment provider methods include unfinished behavior risk | Refactoring inventory includes functional code and mechanics together | Renaming can hide a payment correctness defect | Separate behavior audit before transport refactor; add provider contract tests | Slower first payment wave, lower production risk |
| F-12 | Framework version updates are conflated with refactoring | “Latest” was treated as an implementation requirement | Unbounded upgrade blast radius and difficult regressions | Pin a tested platform patch; upgrade separately with release notes and compatibility gate | May temporarily remain one patch behind the newest release |

## 13. Gradual implementation sequence

The following sequence is the implementation order, not a request to execute it
in this design pass. Each phase must be independently reviewable and revertible.

### Phase 0 — Baseline and guardrails

Create `docs/superpowers/migrations/framework-first-migration-ledger.md` and
record each candidate's current callers, behavior, proposed owner, persistence
decision, replacement, deletion condition, and rollback. Add or confirm
architecture tests for:

- no framework imports in `libraries:ai-contracts` and domain packages;
- no cross-module entity/repository access;
- no direct `JdbcTemplate` in feature/application packages;
- no direct provider HTTP client in application packages;
- one owner for event publication, tenant bootstrap, and durable state.

Capture baseline test, compilation, startup, migration, query-count, provider,
and performance results before code movement.

### Phase 1 — AI contracts and naming

1. Choose canonical names from section 6.2 and record compatibility aliases.
2. Migrate callers in `assistant`, `ai-platform`, `documents`, `catalog`, and
   `tools:ai-evaluation` one capability at a time.
3. Make `ai-contracts` framework-neutral and remove unused graph/provider
   mirrors.
4. Delete aliases only after `rg` caller search, tests, compilation, and
   architecture checks are clean.

### Phase 2 — Spring AI composition

1. Collapse chat construction to one provider registry/router.
2. Make advisors the single policy insertion point for prompt version and
   tenant security.
3. Use Spring AI structured output for extraction and tool callbacks for
   registration.
4. Use retrieval augmentation for standard RAG; retain custom retriever and
   answer policy only for tenant filtering, fallback, and abstention.
5. Replace duplicate observation/trace mechanics where Spring AI already emits
   the required signal.

### Phase 3 — LangGraph boundary

1. Prove which conversation/quote paths need graph interruption and checkpoint.
2. Keep only the graph definitions, adapters, tenant policy decorator, and
   PostgreSQL checkpoint store required by those paths.
3. Compare any linear flow with Modulith events plus explicit state.
4. Keep graph library types out of application ports and contracts.

### Phase 4 — Tenancy and persistence safety

1. Move membership/provisioning policy away from bootstrap JDBC.
2. Verify the entity-manager/bootstrap cycle before changing registry access.
3. Convert safe tenant registry/membership CRUD to JPA.
4. Keep dynamic schema creation, Liquibase, resolver setup, and session/RLS
   setup in named JDBC infrastructure adapters.
5. Make provisioning events idempotent and failure-visible.

### Phase 5 — AI persistence and search

1. Review assistant and ai-platform JDBC stores using the JPA-first procedure.
2. Convert stable aggregate CRUD to JPA repositories/projections/locking.
3. Keep `JdbcClient` for atomic claims, JSONB, pgvector, FTS/RRF, AGE, and
   LangGraph checkpoints where tests prove the need.
4. Separate durable semantic metadata from Redis/vector hot projections.

### Phase 6 — Domain persistence waves

Process modules in dependency order:

```text
shared → tenancy → identity → clients/staffing → services/salon
       → appointments → subscriptions → documents/catalog
       → calendar/notification/payment → booking/audit
```

For each module: inventory entities/repositories/adapters, convert only safe
CRUD, add collision/idempotency/locking tests, remove redundant support code,
then update architecture and module package metadata.

### Phase 7 — Provider integrations

Migrate identity, calendar, notification, and payment provider clients in
separate slices. Each provider slice must include typed DTOs, timeout/auth
configuration, error classification, idempotency, contract tests, and an
offline failure test. Do not migrate payment transport while leaving an
unverified state transition hidden behind a renamed class.

### Phase 8 — Libraries, build logic, and operations

1. Split generic and feature-specific test fixtures.
2. Remove duplicate Gradle declarations and capability overprovisioning.
3. Audit platform constraints and dependency analysis output.
4. Align Liquibase ownership, migration scripts, compose/Kubernetes profiles,
   health checks, CI gates, and load tests.

### Phase 9 — Deletion and final enterprise gate

Delete compatibility contracts, generic HTTP wrappers, obsolete JDBC adapters,
unused dependencies, stale files, and historical tests only after their ledger
conditions are met. Run full verification and compare behavior/performance
against the Phase 0 baseline.

## 14. Verification and test design

### 14.1 Test-first rule per implementation slice

For every change: write the smallest failing test from the migration ledger,
implement the smallest replacement, refactor only after green, and commit the
slice. No source rename is considered harmless: serialization, bean selection,
transaction boundaries, SQL count, and exception behavior receive regression
coverage where applicable.

### 14.2 Required test categories

| Change | Required evidence |
|---|---|
| JPA conversion | Repository slice tests, mapping tests, transaction/locking test, PostgreSQL integration test for dialect-specific behavior, query-count/N+1 check |
| JdbcClient survivor | SQL parameter/row mapping unit test, PostgreSQL integration test, tenant/RLS test, concurrent claim/idempotency test |
| Tenant provisioning | New-tenant success, duplicate replay, invalid schema identifier, partial failure/retry, resolver/bootstrap cycle, migration status test |
| Spring AI | ChatClient/advisor ordering test, structured-output validation, tool authorization/idempotency, retrieval tenant filter, provider-offline/fallback test, observation contract |
| LangGraph | graph topology, interrupt/resume, unauthorized resume, checkpoint tenant isolation, duplicate invoke, failed node/recovery test |
| Redis | TTL/eviction safe miss, key tenant isolation, atomic rate limit, Redis unavailable behavior, projection rebuild |
| Modulith/Kafka | publication transaction, listener idempotency, retry/failure, event schema compatibility, Kafka externalization only where selected |
| HTTP/provider | typed request/response contract, auth/token expiry, timeout/rate-limit/server failure classification, idempotency/webhook signature |
| Build/platform | convention plugin functional test, dependency graph, module architecture, clean compilation, affected integration suite |
| Database/operations | Liquibase forward migration, RLS isolation, index/operator presence, startup/migration job health, compose/Kubernetes smoke |

### 14.3 Quality gates

Use fast gates during implementation and full gates at phase boundaries:

```text
slice: focused test → affected compile → affected architecture/style checks
phase: module tests → integration containers → architecture/dependency checks
final: full compile → all tests → migrations/startup/E2E → performance
       → Spotless/Checkstyle/coverage/Sonar/security/dependency checks
```

Spotless, lint, and full compilation are final gates for the repository-wide
change, but affected-module checks still run before each commit so regressions
are caught close to the edit. A pre-existing unrelated formatting failure must
be recorded rather than fixed opportunistically in a refactor slice.

## 15. Risks, rollback, and non-goals

### 15.1 Main risks

- JPA conversion can change flush timing, SQL shape, lazy-loading behavior,
  locking, or tenant session setup.
- Replacing provider clients can change retry, timeout, status, or signature
  behavior even when the public method name is unchanged.
- Consolidating AI contracts can change null/default semantics and serialized
  workflow/event payloads.
- Moving listeners to Modulith can change transaction timing and retry behavior.
- Redis/vector projections can become stale or use a mismatched embedding
  model/dimension.
- Removing test fixtures can reveal hidden coupling that the current build
  accidentally masks.

### 15.2 Rollback strategy

Each phase uses additive compatibility where needed, feature properties for
optional Spring AI/LangGraph/Redis paths, database-forward migrations only,
and small commits. A slice is rolled back by switching the composition bean or
feature property and retaining the old adapter until replacement evidence is
complete. No deployed Liquibase migration is edited in place.

### 15.3 Explicit non-goals

- No blanket conversion of every JDBC use to JPA.
- No blanket adoption of Kafka, Redis, LangGraph, AGE, or a provider SDK.
- No moving all entities into a shared library.
- No removal of tenant/RLS/database constraints in favor of application-only
  checks.
- No framework-version upgrade bundled with functional refactoring.
- No broad rename without caller, serialization, and compatibility analysis.

## 16. Acceptance criteria for this design

Before implementation planning begins, the user and implementer should agree
that:

- every repository area in section 8 has an owner and migration disposition;
- stable ports in sections 6.2 and 9.1 are the canonical application vocabulary;
  adapter names are implementation details and must remain replaceable;
- JPA-first and JdbcClient-exception rules in section 9 are mandatory review
  gates, not automatic rewrites;
- Spring AI owns supported AI mechanics while Emme owns policy;
- LangGraph is limited to demonstrably complex resumable workflows;
- each remaining custom adapter has a reason, test category, and rollback;
- implementation is gradual and test-first, with final formatting/lint/build
  gates at phase/repository boundaries;
- no implementation plan or code change proceeds until this written design is
  approved.

## 17. Source references

- Spring Boot system requirements and release compatibility:
  https://docs.spring.io/spring-boot/4.2/system-requirements.html
- Spring Framework JDBC and `JdbcClient`:
  https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html
- Spring Data JPA repositories, queries, projections, and locking:
  https://docs.spring.io/spring-data/jpa/reference/jpa.html
  https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html
  https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html
  https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html
- Spring AI getting started and supported Spring Boot line:
  https://docs.spring.io/spring-ai/reference/getting-started.html
- Spring AI ChatClient, tools, advisors, RAG, vector stores, and observability:
  https://docs.spring.io/spring-ai/reference/api/chatclient.html
  https://docs.spring.io/spring-ai/reference/api/tools.html
  https://docs.spring.io/spring-ai/reference/api/advisors.html
  https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html
  https://docs.spring.io/spring-ai/reference/api/vectordbs.html
  https://docs.spring.io/spring-ai/reference/observability/index.html
- Spring Data Redis cache, repositories, and expiration:
  https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html
  https://docs.spring.io/spring-data/redis/reference/repositories.html
  https://docs.spring.io/spring-data/redis/reference/redis/redis-repositories/expirations.html
- Spring Modulith events and Kafka externalization:
  https://docs.spring.io/spring-modulith/reference/events.html
- Spring Framework REST clients and HTTP interfaces:
  https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
- Spring Security authorized clients:
  https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html
