# Repository Framework-first Refactoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Every task uses checkbox tracking and strict Red → Green → Refactor TDD.

**Goal:** Gradually reduce repository-specific mechanics by using Spring, PostgreSQL, Redis, Kafka, Spring AI, LangGraph4j, and provider capabilities wherever they are simpler and behaviorally safe, while preserving tenant isolation and enterprise guarantees.

**Architecture:** Keep the modular monolith and module-private domain persistence boundaries. Use Spring Data JPA for stable entity-backed operations, `JdbcClient` only for bootstrap, atomic, dynamic-schema, JSONB, vector, FTS, RRF, AGE, and other proven PostgreSQL-specific work. Use Spring AI for model/RAG/tool mechanics, LangGraph4j only for complex resumable workflows, Spring Modulith for internal durable events, and Kafka only for selected external event boundaries.

**Tech Stack:** Java 25, Gradle, Spring Boot 4.1.x, Spring AI 2.0.x, Spring Modulith 2.1.x, Spring Data JPA, Spring JDBC `JdbcClient`, PostgreSQL/Liquibase/pgvector/AGE, Spring Data Redis, Kafka, LangGraph4j, JUnit 5, AssertJ, Mockito, ArchUnit, Testcontainers, Spotless, Checkstyle, JaCoCo, and the existing E2E/load-test tooling.

## Global Constraints

- The canonical design is [`2026-09-03-repository-framework-first-refactoring-design.md`](../specs/2026-09-03-repository-framework-first-refactoring-design.md); do not implement behavior outside its decision matrix.
- The first execution wave is `libraries:ai-contracts`, `modules:ai-platform`, and `modules:assistant`; later waves use the same rules across every repository project.
- JPA is the default for stable module-owned entity CRUD, projections, transactions, and locking.
- `JdbcClient` is retained only for dynamic tenant identifiers, bootstrap/entity-manager lifecycle constraints, atomic claims/idempotency, JSONB, pgvector, FTS/RRF, AGE, LangGraph checkpoints, or a measured simpler SQL operation.
- No direct `JdbcTemplate` remains in feature/application code after the relevant wave; lower-level JDBC is limited to the named bootstrap/connection boundary.
- `libraries:ai-contracts` remains free of Spring, Spring AI, JPA, Redis, JDBC, LangGraph4j, and provider SDK types.
- Spring AI owns model transport, `ChatClient`, structured output, advisors, tool callback mechanics, retrieval augmentation, vector-store mechanics, and observations; Emme code owns tenant/security/admission/idempotency/audit policy.
- LangGraph4j owns only graph topology, checkpointed interruption, and resume; it does not own authorization, repositories, payments, or generic event delivery.
- PostgreSQL remains the durable source of truth; Redis is disposable cache/live/coordination state; Kafka is not an aggregate database.
- Stable application ports and cross-module contracts are canonical. Provider and mechanism implementations remain replaceable behind those ports; do not make PostgreSQL, Redis, Kafka, JPA, Spring AI, or vendor-specific types part of use cases, domain code, public APIs, or event contracts. Configuration exposes ports, and provider selection stays in the composition root.
- Every implementation task writes the failing test first, runs the focused test, implements the minimum, refactors only after green, and commits one logical slice.
- Do not edit deployed Liquibase migrations in place; add forward migrations and migration-contract coverage.
- Do not combine dependency upgrades with behavioral refactors. Upgrade to the latest compatible stable patch in a separate platform task.
- Preserve unrelated worktree changes and stage only files belonging to the current task.
- Final repository-wide Spotless, Checkstyle, compilation, coverage, integration, startup, E2E, security, and performance gates run after the gradual waves; affected-module gates run before each slice commit.

## 1. Scope and execution strategy

This plan is the executable companion to the repository-wide design. It is
intentionally divided into a first wave and later waves. The first wave removes
the highest-risk duplication around AI contracts, Spring AI composition,
LangGraph boundaries, AI persistence, and tenant-safe context. The later waves
apply the same proven patterns to domain persistence and provider integrations.

The existing repository already contains substantial AI functionality and
earlier implementation plans. Before changing a class, the implementer must
search callers and compare current behavior with the design. A task marked
“consolidate” means migrate callers and then remove a duplicate; it does not
mean introducing a second compatibility abstraction.

### 1.1 Existing files deliberately preserved initially

These are not automatic deletion targets:

- `modules/shared/src/main/java/com/emme/shared/persistence/jdbc/BootstrapConnectionExecutor.java`
  because Liquibase/bootstrap operations require managed connection access;
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/LiquibaseTenantSchemaMigrationAdapter.java`
  because dynamic schema creation is not JPA CRUD;
- `modules/shared/src/main/java/com/emme/shared/search/HybridSearch.java`
  because its PostgreSQL FTS/pgvector/RRF query is specialized;
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/JdbcLangGraphCheckpointSaver.java`
  because LangGraph checkpoint state is JSONB/upsert/tenant-specific;
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/graph/JdbcAgeGraphClient.java`
  because AGE traversal is a specialized database boundary;
- `libraries/kernel/src/main/java/com/emme/kernel/context/TenantExecutionContextScope.java`,
  `AiExecutionContextScope.java`, and `StructuredParallelTaskRunner.java`
  because context propagation and structured concurrency are cross-cutting
  primitives, not duplicate business mechanics.

### 1.2 File ownership rule

The implementer must keep the following ownership after every task:

```text
ai-contracts       framework-neutral cross-module values and narrow contracts
ai-platform        provider adapters, model admission, and AI infrastructure
assistant          AI use cases, tenant policy, tools, RAG, and workflows
business modules   authoritative domain state and application services
shared             truly cross-module infrastructure only
application        composition root and deployment wiring
database           PostgreSQL schema, RLS, extensions, indexes, migrations
libraries/testing  generic test infrastructure, not feature fixtures
```

## 2. File map before implementation

| Responsibility | Current files | First target |
|---|---|---|
| Cross-module AI contracts | `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/{model,embedding,rag,tool,workflow,graph,semantic}/**` | One capability contract per operation; no framework types |
| Spring AI provider infrastructure | `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider/springai/**`, `configuration/**` | Capability-specific model adapters and one provider composition path |
| Spring AI chat/RAG/tools | `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAi*Configuration.java`, `adapter/out/provider/springai/**` | One `ChatClient` composition path with ordered advisors and controlled callbacks |
| AI workflow | `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/**` | Keep graph only for resumable complexity; hide LangGraph types behind ports |
| AI JDBC persistence | `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/Jdbc*.java`, `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/Jdbc*.java` | JPA for stable aggregates; named `Postgres*Store`/`Postgres*Repository` for survivors |
| Tenant bootstrap | `modules/tenancy/src/main/java/com/emme/tenancy/{configuration,adapter,out,application}/**` | One tenancy-owned bootstrap/provisioning boundary |
| Provider HTTP | `modules/{payment,notification,calendar,identity}/**/*HttpClient.java`, provider classes | Typed `RestClient`/HTTP interfaces or justified official SDK |
| Shared test infrastructure | `libraries/testing/src/testFixtures/java/**`, module test fixtures | Generic fixtures only; feature fixtures stay with owners |
| Build conventions | `build-logic/src/main/kotlin/**`, `platform/build.gradle.kts`, module `build.gradle.kts` | Capability-specific conventions and no duplicate declarations |

## 3. Phase A — Baseline, ledger, and architecture guardrails

### Task 1: Create the migration ledger and baseline report

**Files:**

- Create: `docs/superpowers/migrations/framework-first-migration-ledger.md`
- Modify: `tasks/todo.md`
- Inspect: `settings.gradle.kts`, `gradle/libs.versions.toml`, `build-logic/src/main/kotlin/**`, all module build files

**Acceptance criteria:**

- The ledger lists every project from `settings.gradle.kts`, each JDBC/HTTP/Spring AI/LangGraph/Redis/Kafka hotspot, its owner, target technology, reason, rollback, and deletion condition.
- The baseline records current version pins, focused test commands, full compile command, and known environment limitations.
- No source code changes are included in this task.

- [x] **Step 1: Write the failing ledger validation test**

Create `applications/emme-platform/src/test/java/com/emme/RepositoryFrameworkFirstInventoryTest.java` that loads the repository project list and asserts that the ledger contains each included Gradle project name and each production JDBC-related path returned by the repository inventory script.

- [x] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :applications:emme-platform:test --tests com.emme.RepositoryFrameworkFirstInventoryTest --no-parallel --no-configuration-cache
```

Expected result: FAIL because the ledger and validation test do not yet exist.

- [x] **Step 3: Write the minimum ledger and test implementation**

Add the ledger tables and make the test use fixed expected project names plus
the exact inventory paths from the design. Do not make the test depend on
network, Docker, or provider availability.

- [x] **Step 4: Run the focused test and refactor**

Run the command again. Expected result: PASS. Refactor only for readable
inventory grouping, then run `git diff --check`.

- [x] **Step 5: Commit**

```bash
git add docs/superpowers/migrations/framework-first-migration-ledger.md tasks/todo.md applications/emme-platform/src/test/java/com/emme/RepositoryFrameworkFirstInventoryTest.java
git commit -m "docs: add framework-first migration ledger"
```

### Task 2: Enforce framework leakage and direct JDBC boundaries

**Files:**

- Create: `applications/emme-platform/src/test/java/com/emme/RepositoryFrameworkBoundaryArchitectureTest.java`
- Modify: `modules/assistant/src/test/java/com/emme/assistant/AssistantPackageConventionTest.java`
- Modify: existing module package convention tests only when a rule is missing

**Acceptance criteria:**

- `libraries:ai-contracts` cannot import Spring, Spring AI, JPA, JDBC, Redis, Kafka, LangGraph4j, OkHttp, or provider SDK packages.
- Application/domain packages cannot import `JdbcTemplate`, `NamedParameterJdbcTemplate`, or raw provider HTTP clients.
- Existing valid bootstrap, specialized search, graph, and checkpoint boundaries are allow-listed by package and class name, not by broad module exclusion.

- [x] **Step 1: Write failing ArchUnit tests**

Add tests that import the production packages and assert the forbidden package
dependencies and class locations. The allowed JDBC classes must be exactly:

```text
com.emme.shared.persistence.jdbc.BootstrapConnectionExecutor
com.emme.tenancy.configuration.BootstrapJdbcConfiguration
com.emme.tenancy.adapter.out.client.database.*
com.emme.assistant.ai.adapter.out.workflow.JdbcLangGraphCheckpointSaver
com.emme.assistant.ai.adapter.out.graph.JdbcAgeGraphClient
com.emme.shared.search.HybridSearch
```

- [x] **Step 2: Run the tests to verify current violations are visible**

Run:

```bash
./gradlew :applications:emme-platform:test --tests com.emme.RepositoryFrameworkBoundaryArchitectureTest --no-parallel --no-configuration-cache
```

Expected result: FAIL with the current direct imports/leakage, or compile
failure if the test classes are not yet present.

- [x] **Step 3: Implement the minimum rule and source moves required by the test**

Do not migrate production classes in this task. Encode the rules and current
allow-list so later tasks get a precise failure when they introduce leakage.

- [x] **Step 4: Run focused architecture tests and commit**

Expected result: PASS for the baseline allow-list. Commit:

```bash
git add applications/emme-platform/src/test/java/com/emme/RepositoryFrameworkBoundaryArchitectureTest.java modules/assistant/src/test/java/com/emme/assistant/AssistantPackageConventionTest.java
git commit -m "test: guard framework ownership boundaries"
```

## 4. Phase B — AI contract and Spring AI consolidation

### Task 3: Select and lock canonical AI contracts

**Files:**

- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/model/ChatModel.java`
- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/model/ChatCompletionPort.java`
- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/model/EmbeddingModel.java`
- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/model/EmbeddingPort.java`
- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/embedding/EmbedTextUseCase.java`
- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/tool/**`
- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/rag/**`
- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/workflow/**`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/ChatCompletionPort.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/EmbeddingModelPort.java`
- Test: matching contract tests in `libraries/ai-contracts/src/test/**`, `modules/ai-platform/src/test/**`, and `modules/assistant/src/test/**`

**Canonical outputs:**

```text
AiChatCompletion       one application chat capability with provider/result metadata
EmbeddingPort           one outbound embedding capability, or EmbeddingService if caller search proves it is a use case
AiToolDefinition        one tool metadata record
AiToolGateway           one authorization/idempotency gateway
KnowledgeRetriever      retrieval only
RagAnswerService        answer policy only
ConversationWorkflow    application workflow capability
QuoteWorkflow           application quote workflow capability
```

**Acceptance criteria:**

- No duplicate interface with identical behavior remains after caller migration.
- Contracts contain records/enums/interfaces only and remain framework-neutral.
- Serialized event/workflow names remain compatible or have an explicit migration test.

- [x] **Step 1: Write failing contract tests**

Add one test per canonical capability asserting the method signature and one
architecture test asserting that Spring/JPA/Redis/LangGraph types are absent.
For example, the chat capability test must compile a request containing
message, tenant context, provider policy, and return a response containing
content plus provider metadata without importing `ChatClient`.

- [x] **Step 2: Run focused tests and record the duplicate failure**

```bash
./gradlew :libraries:ai-contracts:test :modules:ai-platform:test :modules:assistant:test --tests '*Contract*' --no-parallel --no-configuration-cache
```

Expected result: FAIL where duplicate contracts or old callers still exist.

- [x] **Step 3: Migrate one capability at a time**

Update imports/callers in this order: embedding, chat, tools, retrieval/RAG,
semantic cache, workflow. Keep a deprecated forwarding alias only while a
remaining caller is migrated; record the alias and deletion condition in the
ledger. Do not add a new generic `AiProvider` interface.

- [x] **Step 4: Run tests, refactor names, and commit**

Run the focused command again, then `./gradlew :libraries:ai-contracts:compileJava :modules:ai-platform:compileJava :modules:assistant:compileJava --no-parallel --no-configuration-cache`.
Commit:

```bash
git add libraries/ai-contracts modules/ai-platform modules/assistant
git commit -m "refactor(ai): consolidate capability contracts"
```

### Task 4: Collapse opt-in Spring AI chat composition to one path

**Files:**

- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiChatConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiChatProviderRegistry.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/provider/ChatModelSelector.java`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiModelProvider.java`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiChatModel.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiChatConfigurationTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiAdapterConsolidationArchitectureTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/application/provider/ChatModelSelectorTest.java`

**Target behavior:**

```text
ChatModel beans → ChatClient.Builder/Configurer → named ChatClient beans
               → ChatModelSelector(provider-neutral selection/admission/fallback)
               → ordered advisors → application chat capability
```

**Acceptance criteria:**

- One production construction path exists for named chat clients and the
  provider-neutral selector within the opt-in Spring AI chat configuration.
- Multiple provider clients retain Spring AI observability/customizers and use explicit qualifiers/primary selection.
- Fallback occurs only for the existing provider-unavailable error; security, validation, and persistence errors propagate.
- `SpringAiModelProvider` is deleted once caller search is clean; it is not retained as a permanent composite wrapper.

The legacy `AiProviderConfiguration` and the structured-extraction
`ChatClient` path remain compatibility paths until their capability migrations
are executed. They are tracked by the later contract, tools/extraction, and
compatibility-cleanup tasks; this slice does not delete them prematurely.

- [x] **Step 1: Write failing configuration and routing tests**

Cover disabled optional provider, named provider order, selected client,
provider-unavailable fallback, non-retryable error propagation, advisor order,
and scheduler admission. Assert that `ChatClientBuilderConfigurer` is used for
custom clients so observations/customizers are not bypassed.

- [x] **Step 2: Run the focused tests and capture failure**

```bash
./gradlew :modules:assistant:test --tests '*SpringAiChatConfigurationTest' --tests '*ChatModelSelectorTest' --tests '*SpringAiAdapterConsolidationArchitectureTest' --no-parallel --no-configuration-cache
```

- [x] **Step 3: Implement the minimum composition change**

- Remove package-private production overloads used only to construct test
  variants. Inject collaborators into tests. Use one immutable advisor list and
  the existing provider-neutral selector; keep admission/fallback policy in the
  selector and prompt/tool/RAG mechanics in Spring AI.

- [x] **Step 4: Run focused tests, compile, and commit**

```bash
./gradlew :modules:assistant:test :modules:ai-platform:test :modules:assistant:compileJava :modules:ai-platform:compileJava --no-parallel --no-configuration-cache
git add modules/assistant modules/ai-platform
git commit -m "refactor(ai): consolidate Spring AI chat composition"
```

**Current slice result:** The opt-in chat configuration now has one named
`ChatClient` construction path through `ChatClientBuilderConfigurer`, one
provider-neutral `ChatModelSelector` construction path, and no test-only
configuration overloads. The legacy provider composite and extraction client
remain intentionally pending their later migration tasks.

### Task 5: Use Spring AI advanced features for tools and structured extraction

**Files:**

- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiToolConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiToolCallbackProvider.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/tool/AuthorizedAiToolGateway.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiNailDesignExtractor.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiQuoteExtractionConfiguration.java`
- Test: tool gateway/callback tests under `modules/assistant/src/test/java/com/emme/assistant/ai/application/tool/**`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiNailDesignExtractorTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiToolConfigurationTest.java`

**Acceptance criteria:**

- Stable tool catalog uses Spring AI `ToolCallback`/`ToolCallbackProvider`; no custom model/tool loop is maintained.
- `AuthorizedAiToolGateway` remains the authorization, risk, confirmation, idempotency, trace, and authoritative-result gate.
- Structured extraction uses `ChatClient` entity mapping with schema/provider validation where supported, followed by domain validation and abstention.
- Semantic tool search is opt-in and used only when the configured tool catalog justifies vector-index cost and latency.

- [x] **Step 1: Write failing tests**

Test unauthorized role, confirmation-required tool, duplicate idempotency key,
authoritative versus informational result, callback argument conversion,
structured-output invalid schema, domain-invalid enum, and model unavailable.

- [x] **Step 2: Run tests to verify current custom mechanics fail the target contract**

```bash
./gradlew :modules:assistant:test --tests '*Tool*' --tests '*NailDesignExtractorTest' --no-parallel --no-configuration-cache
```

- [x] **Step 3: Implement the minimum framework delegation**

Keep the existing callbacks and gateway boundary because they already delegate
the tool loop to Spring AI while preserving authorization, confirmation,
idempotency, tracing, and tenant context. Keep structured extraction on
`ChatClient...call().entity(...)` with explicit schema/provider validation and
safe rejection. Simplify only the duplicate configuration and constructor
paths, and route all extraction client construction through the configured
Spring AI builder.

- [x] **Step 4: Run focused tests and commit**

```bash
./gradlew :modules:assistant:test :modules:assistant:compileJava --no-parallel --no-configuration-cache
git add modules/assistant
git commit -m "refactor(ai): delegate tools and extraction to Spring AI"
```

**Current slice result:** Spring AI owns the tool-calling loop and structured
entity mapping. The application gateway remains the security and durability
gate; extraction now has one explicit execution configuration and its named
client retains Spring AI builder customizers/observability.

### Task 6: Consolidate Spring AI RAG, advisors, cache, and vector boundaries

#### Current slice 6A — RAG ownership and semantic-cache construction

Completed in this slice:

- The opt-in Spring AI RAG path no longer performs application retrieval before invoking the completion pipeline.
- `RetrievalAugmentationAdvisor` is the single retrieval augmentation owner; its default contextual augmenter remains fail-closed for empty context.
- `RagAnswerProviderChain` was replaced by the clearer `RagAnswerPolicy`, which validates backend context and question input and delegates completion with an empty conversation context.
- `SemanticChatCache` now exposes one production constructor; test defaults are confined to test helpers and the Spring configuration has no test-only overloads.

Remaining Task 6 work is tracked by the steps below: Redis outage/eviction behavior, vector/cache ranking and metadata contracts, advisor-order coverage, and the measured `HybridSearch` decision.

#### Current slice 6B — Redis hot-projection hardening and construction simplification

Completed in this slice:

- `RedisSemanticCacheHotStore` now has one canonical production constructor accepting the provider-neutral `EmbeddingModelConfiguration`, clock, and optional Redis projection dependencies.
- Test-only construction variants were migrated to a local fixture helper, removing six ambiguous public entry points without changing the application port.
- Redis vector documents without a similarity score are ignored instead of being converted into a synthetic `0.0` candidate.
- Regression coverage confirms a Redis outage safely falls back to the durable PostgreSQL cache and that malformed hot documents fail closed.

The remaining Task 6 work is intentionally not collapsed into this slice: advisor-order assertions, complete metadata/model contract coverage, and measured `HybridSearch` alternatives still require separate evidence.

#### Current slice 6D — Hybrid search capability boundary

Completed in this slice:

- `HybridSearch` is now a provider-neutral capability interface with no Spring JDBC types.
- PostgreSQL pgvector/full-text/RRF SQL moved to `PostgresHybridSearch`, the infrastructure
  implementation selected by Spring component scanning.
- Catalog and Documents continue to depend only on `HybridSearch`; no application behavior or
  query semantics changed.
- The direct SQL implementation remains because no equivalent Spring AI vector-store path has
  been measured to preserve FTS, RRF, tenant predicates, and embedding maintenance semantics.

- [x] Add a failing ownership test requiring an interface contract and separate PostgreSQL adapter.
- [x] Move the existing SQL implementation behind `HybridSearch` without changing its API.
- [x] Verify shared, Catalog, Documents, and Spotless checks.
- [ ] Measure a future Spring AI/vector-store alternative before considering replacement.

#### Current slice 6C — Advisor precedence centralization

Completed in this slice:

- Spring AI advisor lists are normalized through one composition-root helper using
  Spring's `AnnotationAwareOrderComparator`.
- Chat and RAG use cases now share the same precedence behavior instead of
  depending on incidental list assembly order.
- Regression coverage verifies the security → prompt metadata → retrieval order
  required by the provider-neutral request contract.

- [x] Write the failing advisor-precedence regression test.
- [x] Implement the shared Spring ordering helper and use it in chat and RAG
      composition roots.
- [x] Run the focused advisor configuration test and compilation.

**Files:**

- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiRagConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/TenantScopedDocumentRetriever.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/provider/RagAnswerPolicy.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticChatCache.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiRedisSemanticConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcSemanticCacheAdapter.java`
- Modify: `modules/shared/src/main/java/com/emme/shared/search/HybridSearch.java`
- Modify/retain: `modules/shared/src/main/java/com/emme/shared/search/HybridSearch.java` behind the stable `KnowledgeRetriever` port
- Test: existing RAG, semantic, Redis, pgvector, and migration contract tests under `modules/assistant`, `modules/shared`, and `database`

**Acceptance criteria:**

- Standard RAG uses `RetrievalAugmentationAdvisor` and one canonical chat router.
- `TenantScopedDocumentRetriever` enforces tenant/principal authorization and bounded retrieval; it does not duplicate generic retrieval mechanics.
- `RagAnswerProviderChain` is deleted if it only forwards calls; otherwise it becomes `RagAnswerPolicy` and contains only fallback/abstention policy.
- PostgreSQL remains authoritative for durable cache metadata/hits; Redis remains an evictable hot projection.
- `HybridSearch` remains direct SQL until an equivalent Spring AI/vector-store path reproduces FTS, pgvector, RRF, model identity, and tenant filtering with equal or better measured behavior.

- [ ] **Step 1: Write failing tests**

Cover advisor order, retrieved-document tenant filter, empty retrieval,
provider fallback, unsafe answer abstention, top-1 threshold and margin,
embedding model/version/dimension mismatch, Redis safe miss, durable cache
hit confirmation, and hybrid search ranking.

- [ ] **Step 2: Run focused tests**

```bash
./gradlew :modules:assistant:test :modules:shared:test :database:test --tests '*Rag*' --tests '*Semantic*' --tests '*HybridSearch*' --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Implement the minimum consolidation**

Build one advisor list per use case, inject the canonical router, simplify the
cache constructor to one production path, and keep policy-specific code only
where the framework cannot express tenant/security/abstention rules. The
advisor composition now delegates ordering to Spring's comparator and keeps
the security and prompt advisors ahead of retrieval.

- [ ] **Step 4: Run focused tests, integration contracts, and commit**

```bash
./gradlew :modules:assistant:test :modules:shared:test :database:test :modules:assistant:compileJava --no-parallel --no-configuration-cache
git add modules/assistant modules/shared
git commit -m "refactor(ai): consolidate Spring AI RAG and semantic paths"
```

#### Current slice 6C — Preserve the provider-neutral RAG metadata contract

`TenantScopedDocumentRetriever` now forwards the metadata already present on
the provider-neutral `RetrievedDocument` contract into Spring AI `Document`
instances. The adapter still writes canonical `sourceId` and `score` values
after copying metadata, so provenance and filtering metadata survive the
framework boundary without allowing arbitrary metadata to replace those
authoritative fields.

- [x] Add a failing adapter test covering metadata preservation.
- [x] Copy `RetrievedDocument.metadata()` into Spring AI document metadata.
- [x] Preserve canonical `sourceId` and `score` adapter fields.
- [x] Run the focused adapter test and compilation.
- [ ] Complete measured hybrid-search and legacy compatibility cleanup slices.

#### Current slice 6D — Remove the orphaned local RAG document type

The assistant-local `KnowledgeDocument` had no production or test callers;
the shared `ai-contracts` `RetrievedDocument` is the canonical provider-neutral
value returned by `KnowledgeRetriever`. The local duplicate is removed and an
architecture test prevents it from being reintroduced.

- [x] Add a failing package-convention test for the duplicate type.
- [x] Remove `modules/assistant/.../application/port/out/KnowledgeDocument.java`.
- [x] Verify the canonical shared RAG contract remains the only assistant RAG document type.
- [ ] Complete measured hybrid-search and legacy compatibility cleanup slices.

## 5. Phase C — LangGraph4j and AI workflow boundary

### Task 7: Prove graph versus simple state-machine scope

**Files:**

- Modify: `docs/superpowers/migrations/framework-first-migration-ledger.md`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/ConversationWorkflowGraphTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/QuoteWorkflowGraphTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/LangGraphConversationWorkflowAdapterTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/LangGraphQuoteWorkflowResumeAdapterTest.java`

**Acceptance criteria:**

- Conversation and quote workflows have a written complexity record showing the branch, interrupt, checkpoint, and resume behavior that requires LangGraph4j.
- Linear operations that only change a status and publish an event are marked for application state plus Modulith, not a graph.
- Graph nodes do not perform authorization, repository access, payment transitions, or direct HTTP calls.

- [x] **Step 1: Write failing topology and boundary tests**

Assert conversation and quote graph node/edge topology, approval/clarification
interrupt points, resume behavior, and absence of direct domain repository
dependencies from graph definitions.

- [x] **Step 2: Run focused workflow tests**

```bash
./gradlew :modules:assistant:test --tests '*WorkflowGraphTest' --tests '*LangGraph*Test' --no-parallel --no-configuration-cache
```

The existing topology, interrupt, resume, and capability-count tests passed;
the complexity record is now captured in the migration ledger.

- [x] **Step 3: Record the decisions and implement only boundary corrections**

Keep `ConversationWorkflowGraph` and `QuoteWorkflowGraph` when their interrupt
and checkpoint behavior is proven. Rename to `ConversationWorkflowDefinition`
or `QuoteWorkflowDefinition` only if the name removes ambiguity and all callers
are migrated in the same slice. Do not introduce a generic graph facade.

- [x] **Step 4: Run tests and commit**

```bash
./gradlew :modules:assistant:test :modules:assistant:compileJava --no-parallel --no-configuration-cache
git add docs/superpowers/migrations/framework-first-migration-ledger.md modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow
git commit -m "test(ai): lock LangGraph workflow boundary"
```

### Task 8: Simplify LangGraph composition and checkpoint naming

#### Current slice 8A — Capability-qualified checkpoint bean

Completed in this slice:

- The tenant-aware checkpoint bean is now exposed as `workflowCheckpointStore`,
  matching the stable capability boundary rather than the implementation
  mechanism.
- Conversation and quote graph composition both consume that one qualified
  bean; no graph or application port was renamed.
- Configuration coverage verifies the qualifier and the focused LangGraph
tests remain green.

#### Current slice 8B — Checkpoint namespace validation

Completed in this slice:

- Added regression coverage for a thread ID with an empty namespace.
- The shared tenant-aware checkpoint boundary now rejects empty or nested
  namespaces before delegating to any provider adapter, preserving the
  tenant/workflow boundary for replaceable checkpoint implementations.
- Focused LangGraph, checkpoint, and compilation tests pass.

#### Current slice 13A — Name the shared tenant JDBC boundary

The tenant-scoped connection pool is shared by AI learning, tools, and AGE
adapters. Its composition-root bean is therefore named `tenantJdbcClient`, not
`aiTenantJdbcClient`; the rename does not change the data source, tenant schema
routing, application ports, or provider substitution boundary.

- [x] Add a failing configuration test for the capability-oriented bean name.
- [x] Rename the bean, factory method, qualifiers, and configuration guards.
- [x] Verify no `aiTenantJdbcClient` references remain.
- [x] Run the focused configuration tests and assistant compilation.

**Files:**

- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/JdbcLangGraphCheckpointSaver.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/TenantAwareCheckpointSaver.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/LangGraphConversationWorkflowAdapter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/LangGraphQuoteWorkflowCapability.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/LangGraphQuoteWorkflowResumeAdapter.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfigurationTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/JdbcLangGraphCheckpointSaverTest.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow/TenantAwareCheckpointSaverTest.java`

**Stable port and adapter boundaries:**

```text
workflowCheckpointStore       application-neutral checkpoint port
JdbcLangGraphCheckpointSaver  current mechanism adapter behind the application-neutral checkpoint port
LangGraphConversationWorkflow     conversation adapter
LangGraphQuoteWorkflow            quote adapter
```

**Acceptance criteria:**

- One opt-in LangGraph configuration creates each graph once and does not create ambiguous generic `CompiledGraph<AgentState>` beans.
- Checkpoint reads/writes enforce tenant, conversation, principal/actor, workflow, and namespace identity.
- Checkpoint upsert, JSONB state, and next-node semantics remain unchanged.
- JPA is explicitly rejected for this checkpoint adapter because the library contract and PostgreSQL-specific state/upsert behavior are clearer in `JdbcClient`.

- [ ] **Step 1: Write failing configuration/security tests**

Test disabled graph startup, one graph bean per capability, unauthorized
checkpoint read, cross-tenant workflow ID, duplicate checkpoint update,
malformed thread ID, resume without checkpoint, and authorized staff resume.

- [ ] **Step 2: Run tests to verify the current behavior**

```bash
./gradlew :modules:assistant:test --tests '*LangGraph*' --tests '*Checkpoint*' --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Implement the minimum composition/name cleanup**

Keep `JdbcClient` and the tenant-aware decorator. Move generic library types
behind the adapter, remove duplicate resume adapter logic only when the tests
prove no separate policy is lost, and use capability-qualified bean names.

- [ ] **Step 4: Run tests, compile, and commit**

```bash
./gradlew :modules:assistant:test :modules:assistant:compileJava --no-parallel --no-configuration-cache
git add modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfiguration.java modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfigurationTest.java modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow
git commit -m "refactor(ai): simplify LangGraph workflow composition"
```

## 6. Phase D — AI persistence with JPA-first decisions

### Task 9: Classify every AI JDBC adapter before changing implementation

**Files:**

- Modify: `docs/superpowers/migrations/framework-first-migration-ledger.md`
- Inspect: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/Jdbc*.java`
- Inspect: `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/Jdbc*.java`
- Inspect: `database/src/main/resources/db/changelog/**`
- Test: existing adapter tests under `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/persistence/**`
- Test: existing learning store tests under `modules/ai-platform/src/test/java/com/emme/ai/platform/learning/**`

**Acceptance criteria:**

- Every production JDBC adapter is classified as JPA candidate, `JdbcClient` survivor, or lower-level connection boundary.
- Each survivor has a concrete reason: dynamic identifier, atomic claim, atomic transition, JSONB, pgvector/FTS/RRF, AGE, LangGraph checkpoint, RLS/session lifecycle, or measured lower complexity.
- No implementation is changed in this task; the classification is reviewable and complete before migration.

- [x] **Step 1: Write a failing classification test**

Add `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/persistence/AiJdbcMigrationClassificationTest.java` and `modules/ai-platform/src/test/java/com/emme/ai/platform/learning/LearningJdbcMigrationClassificationTest.java`. Each test asserts that the ledger contains the class name and one allowed reason.

- [x] **Step 2: Run the tests**

```bash
./gradlew :modules:assistant:test :modules:ai-platform:test --tests '*MigrationClassificationTest' --no-parallel --no-configuration-cache
```

Expected result: FAIL until all current JDBC stores are recorded.

- [x] **Step 3: Complete the ledger from caller and schema searches**

For each class, record its tables, transaction assumptions, result shape,
concurrency behavior, tenant predicate/session behavior, stable port, current
adapter, and the test that proves equivalence. Keep mechanism/provider names
inside adapters and configuration only; never make them the application
contract.

- [x] **Step 4: Run tests and commit the classification**

```bash
./gradlew :modules:assistant:test :modules:ai-platform:test --tests '*MigrationClassificationTest' --no-parallel --no-configuration-cache
git add docs/superpowers/migrations/framework-first-migration-ledger.md modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/persistence/AiJdbcMigrationClassificationTest.java modules/ai-platform/src/test/java/com/emme/ai/platform/learning/LearningJdbcMigrationClassificationTest.java
git commit -m "docs(ai): classify JDBC persistence boundaries"
```

### Task 10: Convert stable AI aggregate CRUD to JPA

#### Current slice 10A — Design-image metadata decision

The initial JPA experiment was reverted after comparison with the original adapter:

- The original `JdbcDesignImageMetadataRepository` is restored.
- The JPA version added an entity, repository, validation, and package surface without reducing
  the two-statement CRUD boundary.
- Tenant/workflow/storage-key scoping and the database uniqueness boundary remain unchanged.

- [x] Compare the JPA mapping with the original adapter.
- [x] Restore the `JdbcClient` adapter and add focused SQL boundary tests.
- [x] Confirm the application port remains unchanged.

#### Current slice 10B — Conversation workflow review audit decision

The initial JPA experiment was reverted after comparison with the original adapter:

- The original `JdbcConversationWorkflowReviewAuditAdapter` is restored.
- The JPA version added an entity and repository for one append-only JSONB insert without reducing
  mapping or serialization code.
- Authenticated tenant/workflow/conversation checks and the unchanged application port remain.

- [x] Compare the JPA mapping with the original adapter.
- [x] Restore the `JdbcClient` adapter and add focused SQL boundary tests.
- [x] Confirm the application port remains unchanged.

**Evaluated files:**

- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiTraceRecorder.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteWorkflowRepository.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteArtifactRepository.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcQuoteReviewRepository.java`
- Existing application ports and adapter tests remain unchanged.

#### Decision gate: quote and trace persistence

The quote artifact candidate was evaluated against the JPA-first rule. It remains on
`JdbcClient` because one adapter owns three related JSONB writes with tenant/workflow
natural-key upserts and foreign-key ordering. Replacing those statements with JPA would
require three entities, three repositories, read-before-write upsert logic, and an explicit
concurrency strategy; that is more code and would be less atomic unless custom SQL were
reintroduced. The stable `QuoteArtifactRepository` port is unchanged.

- [x] Compare JPA mapping and Spring Data natural-key upserts against the existing adapter.
- [x] Retain `JdbcQuoteArtifactRepository` as the narrow `JdbcClient` survivor.
- [x] Record the decision in the migration ledger and architecture inventory.

The same gate applies to `JdbcAiTraceRecorder`, `JdbcQuoteReviewRepository`, and
`JdbcQuoteWorkflowRepository`. Their JSONB upserts, conditional version predicates,
idempotency conflict handling, and review-decision append must remain single SQL
operations or transactions. JPA would only be preferable if a future measured design
reduces code while preserving those invariants; no such design exists in the current
module.

- [x] Evaluate durable AI trace persistence; retain `JdbcClient` for JSONB upserts and redaction.
- [x] Evaluate quote review persistence; retain `JdbcClient` for conditional transition plus decision append.
- [x] Evaluate quote workflow persistence; retain `JdbcClient` for idempotent insert and versioned update.

**Acceptance criteria:**

- JPA is used only where it reduces implementation and maintenance cost.
- Atomic JSONB upserts, idempotency, version predicates, and reviewer ownership semantics remain
  intact behind the existing application ports.
- Every retained `JdbcClient` adapter has a concrete ledger reason and focused equivalence tests.

- [x] Compare the quote and trace mappings against JPA and Spring Data semantics.
- [x] Retain `JdbcClient` for trace, quote workflow, quote artifacts, and quote review boundaries.
- [x] Add focused tests for the restored simple metadata and append-only audit adapters.
- [x] Run the assistant test suite, compilation, and Spotless successfully.

### Task 11: Retain and simplify atomic AI stores with JdbcClient

#### Current slice 11A — AI job status store migration

Completed in this slice:

- `JdbcAiJobStatusStore` now depends on Spring JDBC `JdbcClient` and uses named parameters for enqueue, claim, retry, completion, lease recovery, and tenant-context setup.
- `AiJobExecutorConfiguration` exposes one qualified `coreJdbcClient` for the atomic job boundary; the previous feature-level `coreJdbcTemplate` bean was removed.
- The job store remains a `JdbcClient` survivor because claiming, `FOR UPDATE SKIP LOCKED`, retry recovery, and status transitions are one PostgreSQL transaction/invariant.
- Configuration and integration-test construction were migrated without changing the `AiJobStatusStore` application port.

The live PostgreSQL concurrency gate remains required; it was not runnable in this environment because Testcontainers could not find a Docker runtime.

#### Current slice 11B — AGE graph adapter JdbcClient boundary

The AGE adapter remains a deliberate `JdbcClient` survivor because Apache AGE
requires PostgreSQL extension commands, `ag_catalog.cypher`, graph-name
allow-listing, and transaction-local search-path control. The adapter no longer
accepts the legacy `JdbcOperations` abstraction and no longer converts it
internally. `SpringAiAgeConfiguration` now reuses the canonical qualified
`tenantJdbcClient` already shared by the learning, semantic, and tool
adapters, while the integration test keeps `JdbcTemplate` only for test-
database setup statements. This removes a duplicate client composition root
without pretending AGE traversal is a JPA concern.

Verification: the AGE configuration test, assistant compilation, and Spotless
pass. The live AGE Testcontainers gate remains environment-dependent on Docker.

The canonical tenant JDBC client is now `tenantJdbcClient`; AGE no longer
creates a second feature-specific tenant client bean. This keeps all tenant-scoped
AI SQL adapters on one Spring composition root and avoids ambiguous or
duplicated `JdbcClient` lifecycle configuration.

**Files:**

- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiJobStatusStore.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/JdbcAiToolIdempotencyStore.java`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/JdbcLearningCandidateStore.java`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/JdbcLearningCandidateStateStore.java`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/JdbcLearningCandidateEvaluationStore.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/AiJobExecutorConfiguration.java`
- Test: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/AiJobCoreJdbcConfigurationTest.java`
- Test: `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/AiJobReconciliationClaimIntegrationTest.java`
- Test: existing store unit tests and PostgreSQL concurrency integration tests under assistant/ai-platform integration test trees

**Stable port and adapter boundaries:**

```text
AiJobStatusStore          current `JdbcAiJobStatusStore` adapter behind the port
AiToolIdempotencyStore    current `JdbcAiToolIdempotencyStore` adapter behind the port
LearningCandidateStore    current `JdbcLearningCandidateStore` adapter behind the port
LearningCandidateStateStore       current `JdbcLearningCandidateStateStore` adapter behind the port
LearningCandidateEvaluationStore  current `JdbcLearningCandidateEvaluationStore` adapter behind the port
```

**Acceptance criteria:**

- Each retained SQL operation is a named, parameterized, tenant-scoped `JdbcClient` adapter.
- Claims/leases/idempotency use one atomic SQL transition and never overwrite a successful result.
- Ordinary lookup/history is converted to JPA only where it is simpler and does not split one atomic invariant into multiple transactions.
- Tests cover concurrent workers, lease expiry, duplicate success replay, tenant isolation, and retryable failure.

- [x] **Step 1: Write failing concurrency tests**

Add or extend tests for two workers claiming the same row, expired versus active
lease, duplicate completed idempotency key, malformed JSON payload, and
cross-tenant identifier. PostgreSQL Testcontainers coverage remains present for
the actual unique/conditional update behavior, but requires Docker to execute.

- [x] **Step 2: Run focused tests**

```bash
./gradlew :modules:assistant:test :modules:ai-platform:test --tests '*AiJob*' --tests '*Idempotency*' --tests '*LearningCandidate*' --no-parallel --no-configuration-cache
```

- [x] **Step 3: Rename and simplify only the SQL adapter boundary**

Use `JdbcClient.sql(...).param(...).query(...)`/`update()` with explicit row
mapping. Keep SQL in the adapter, keep application ports technology-neutral,
and remove duplicate helper methods only after test coverage proves identical
claim and replay behavior.

- [x] **Step 4: Run unit tests, compile, Spotless, and commit**

```bash
./gradlew :modules:assistant:test :modules:ai-platform:test :modules:assistant:compileJava :modules:ai-platform:compileJava --no-parallel --no-configuration-cache
git add modules/assistant modules/ai-platform
git commit -m "refactor(ai): standardize atomic JdbcClient stores"
```

The remaining live PostgreSQL concurrency and AGE gates are environment-dependent
because Testcontainers cannot run without Docker in the current environment.

## 7. Phase E — Tenancy/bootstrap safety

### Task 12: Move tenant membership policy off bootstrap JDBC

**Files:**

- Delete: `modules/tenancy/src/main/java/com/emme/tenancy/application/service/EnsureTenantMembershipService.java`
- Create: `modules/identity/src/main/java/com/emme/identity/application/service/EnsureTenantMembershipService.java`
- Modify: `modules/identity/src/main/java/com/emme/identity/application/port/out/{RoleRepository,MembershipRepository}.java`
- Modify: `modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/{adapter,repository}/**`
- Create: `modules/subscriptions/src/main/java/com/emme/subscriptions/api/usecase/EnsureTenantSubscriptionUseCase.java`
- Create: `modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/EnsureTenantSubscriptionService.java`
- Modify: `modules/subscriptions/src/main/java/com/emme/subscriptions/domain/model/Subscription.java`
- Modify: `modules/subscriptions/src/main/java/com/emme/subscriptions/adapter/in/messaging/consumer/SubscriptionProvisioningListener.java`
- Test: identity membership, tenancy boundary, and subscription provisioning tests

**Acceptance criteria:**

- Application services express membership/provisioning policy and do not inject `JdbcTemplate` or bootstrap `JdbcClient`.
- Stable tenant registry/membership CRUD uses JPA where the entity-manager lifecycle is available; membership ownership stays in Identity because that module already owns the JPA model.
- Subscription provisioning calls a typed application use case under the trusted tenant context and does not interpolate schema identifiers.
- Duplicate provisioning is a no-op only when the database confirms the duplicate; operational failures remain visible and retryable.

- [x] **Step 1: Write failing service/listener tests**

Test membership creation, duplicate membership, tenant context mismatch,
subscription provisioning duplicate, invalid schema name, and migration
failure propagation. Assert no application service depends on bootstrap JDBC.

- [x] **Step 2: Run the focused tests**

```bash
./gradlew :modules:tenancy:test :modules:subscriptions:test --tests '*Membership*' --tests '*Provisioning*' --no-parallel --no-configuration-cache
```

- [x] **Step 3: Implement the smallest JPA/port move**

Reuse Identity's existing JPA role/membership repositories through explicit
application ports, and keep `BootstrapJdbcConfiguration` only for bootstrap
consumers. Keep raw connection/Liquibase classes unchanged. Route subscription
activation through the subscription repository under an explicit tenant context;
the repository lookup is the duplicate check, while persistence failures remain
visible for Modulith retry.

- [x] **Step 4: Run tests, compile, and commit**

```bash
./gradlew :modules:tenancy:test :modules:subscriptions:test :modules:tenancy:compileJava :modules:subscriptions:compileJava --no-parallel --no-configuration-cache
git add modules/tenancy modules/subscriptions
git commit -m "refactor(tenancy): isolate bootstrap persistence"
```

### Task 13: Verify the unavoidable tenant JDBC boundary

**Files:**

- Modify: `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/DatabaseRegistryAdapter.java`
- Modify: `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/LiquibaseTenantSchemaMigrationAdapter.java`
- Modify: `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantIdentifierResolver.java`
- Rename: `modules/shared/src/main/java/com/emme/shared/persistence/jdbc/JdbcConnectionExecutor.java` → `BootstrapConnectionExecutor.java`
- Modify: `modules/tenancy/src/main/java/com/emme/tenancy/configuration/BootstrapJdbcConfiguration.java`
- Test: `modules/tenancy/src/test/java/com/emme/tenancy/adapter/out/client/database/**`
- Rename: `modules/shared/src/test/java/com/emme/shared/persistence/jdbc/JdbcConnectionExecutorTest.java` → `BootstrapConnectionExecutorTest.java`

**Acceptance criteria:**

- The remaining lower-level JDBC code is limited to dynamic schema, Liquibase, resolver, registry-cycle, and session/RLS concerns.
- `BootstrapConnectionExecutor` is used only by bootstrap/lifecycle callers; the caller search proved the rename safe.
- Every dynamic schema identifier is validated against the trusted tenant registry and cannot be supplied as an untrusted SQL value.

- [x] **Step 1: Write failing boundary tests**

Cover dynamic schema validation, bootstrap connection closure, resolver before
entity-manager initialization, Liquibase failure, registry-cycle behavior,
tenant session setup, and cross-tenant access rejection.

- [x] **Step 2: Run focused tenancy/shared tests**

```bash
./gradlew :modules:tenancy:test :modules:shared:test --tests '*DatabaseRegistry*' --tests '*LiquibaseTenant*' --tests '*TenantIdentifier*' --tests '*BootstrapConnectionExecutor*' --no-parallel --no-configuration-cache
```

- [x] **Step 3: Rename only if the verified purpose is bootstrap-only**

Keep the lower-level connection callback because Spring documents that advanced
JDBC operations may need it. Do not replace it with JPA. Move any accidental
feature caller to a typed repository or `JdbcClient` adapter.

- [x] **Step 4: Run tests, compile, and commit**

```bash
./gradlew :modules:tenancy:test :modules:shared:test :modules:tenancy:compileJava :modules:shared:compileJava --no-parallel --no-configuration-cache
git add modules/tenancy modules/shared
git commit -m "refactor(tenancy): narrow bootstrap JDBC boundary"
```

#### Task 13A: Use `JdbcClient` for the scalar bootstrap lookup

The Hibernate tenant resolver now uses the named `bootstrapJdbcClient` bean for
its single schema-name query. This keeps the resolver independent from the
tenant-routed entity manager while using the current fluent Spring JDBC API.
The `bootstrapJdbcTemplate` bean remains intentionally limited to
`BootstrapConnectionExecutor`, whose callers require Spring's managed
`ConnectionCallback` boundary for session setup and lifecycle work. Both beans
are composed from the same dedicated bootstrap `DataSource`; this change does
not alter transaction propagation, database isolation, or tenant/RLS policy.

- [x] Add a success-path test that verifies the resolver obtains and uses
      `bootstrapJdbcClient`.
- [x] Migrate the scalar registry lookup from `JdbcTemplate` to `JdbcClient`.
- [x] Keep `JdbcTemplate` only for the raw managed connection callback.
- [x] Run the focused resolver test and tenancy formatting checks.

#### Current slice 13B — Inject the bootstrap client into tenant resolution

`TenantIdentifierResolver` now receives the named `bootstrapJdbcClient` through
its constructor. The resolver no longer reaches into
`ApplicationContextProvider` to locate infrastructure at runtime, so the
Hibernate boundary is explicit and directly testable. `TenantDataSourceConfiguration`
remains the composition root for this adapter and wires the same dedicated
bootstrap client into the resolver; the tenant-scoped data source and
connection-checkout schema selection are unchanged.

- [x] Write the red test using constructor-injected `JdbcClient` fakes.
- [x] Remove the resolver's static application-context lookup.
- [x] Wire the qualified bootstrap client in the tenant data-source factory.
- [x] Verify focused tests, compilation, Checkstyle, and Spotless.
- [x] Audit and replace `SchemaMultiTenantConnectionProvider`'s lifecycle
      fallback lookup through Spring-managed dependencies.

#### Current slice 13C — Register Hibernate tenancy through Spring beans

The Hibernate connection provider and tenant identifier resolver are now
Spring-managed components. Each implements `HibernatePropertiesCustomizer` and
registers its injected instance, so Hibernate receives the same dependency graph
that Spring tested and composed; the previous FQCN properties and reflective
construction path are removed from both application profiles. This follows
Spring Boot's documented customization hook and Hibernate's support for an
instance-valued multi-tenancy setting:
`[Spring Boot data access](https://docs.spring.io/spring-boot/how-to/data-access.html)`
and `[Hibernate multi-tenancy settings](https://docs.hibernate.org/orm/7.3/javadocs/org/hibernate/cfg/MultiTenancySettings.html)`.

- [x] Add red tests for provider routing and Hibernate instance registration.
- [x] Inject metadata `DataSource` and `TenantDatabasePoolProvider` into the
      connection provider.
- [x] Register the resolver and provider through `HibernatePropertiesCustomizer`.
- [x] Remove duplicate YAML class-name configuration and the orphaned
      `ApplicationContextProvider` service locator.
- [x] Reuse the managed resolver from `TenantDataSourceConfiguration` instead
      of constructing a second resolver/cache.
- [x] Reuse the primary core `DataSource` as the bootstrap boundary when the
      normal profile has no standalone `spring.datasource.url`; retain a
      dedicated bootstrap data source when an explicit URL is configured.
- [x] Verify tenancy tests, application compilation, Checkstyle, and Spotless.
- [ ] Validate non-H2 startup against a PostgreSQL/Testcontainers environment.

## 8. Phase F — External provider clients

### Task 14: Establish the typed HTTP client convention

**Files:**

- Modify: `modules/identity/src/main/java/com/emme/identity/configuration/IdentityClientConfiguration.java`
- Modify: `modules/calendar/src/main/java/com/emme/calendar/configuration/GoogleClientConfiguration.java`
- Modify: `modules/notification/src/main/java/com/emme/notification/configuration/NotificationClientConfiguration.java`
- Modify: `modules/payment/src/main/java/com/emme/payment/configuration/PaymentClientConfiguration.java`
- Create: one representative typed API interface under `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/http/ProviderHttpClient.java` only if the existing stack cannot share Spring's interface pattern directly
- Test: configuration tests under each listed module

**Acceptance criteria:**

- Timeout, base URL, authentication, observation, and error classification are configured through Spring-managed clients.
- Provider-specific request/response DTOs remain in provider packages.
- No new universal `HttpClient` abstraction hides provider semantics.

- [ ] **Step 1: Write failing configuration tests**

Test that each provider client has the configured base URL/timeout, does not
create a client per request, propagates correlation/tenant-safe headers, and
maps transport failures to the module's retryable error contract.

- [ ] **Step 2: Run the configuration tests**

```bash
./gradlew :modules:identity:test :modules:calendar:test :modules:notification:test :modules:payment:test --tests '*ClientConfiguration*' --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Implement one provider channel at a time using `RestClient` or `@HttpExchange`**

Use Spring's managed builder/configurer. Keep official SDK evaluation separate
for Keycloak and Google where signing/authentication may materially reduce risk.
Do not change payment state transitions in this task.

- [ ] **Step 4: Run tests and commit the convention slice**

```bash
./gradlew :modules:identity:test :modules:calendar:test :modules:notification:test :modules:payment:test --no-parallel --no-configuration-cache
git add modules/identity modules/calendar modules/notification modules/payment
git commit -m "refactor(integrations): standardize Spring HTTP clients"
```

#### Task 14A: Migrate the WhatsApp transport wrapper

The first provider slice is complete. WhatsApp reply delivery now injects a
qualified, singleton Spring `RestClient` built from `WhatsAppProperties`; the
one-method `AiHttpClient`/`OkHttpClient` wrapper and its generic AI transport
configuration were deleted. `WhatsAppReplyPort` remains unchanged, so the
application contract stays provider-neutral while the Graph API details remain
inside the outbound adapter.

- [x] Add provider contract coverage for request URL, bearer authentication,
      JSON body, incomplete credentials, and HTTP failure handling.
- [x] Configure one provider-scoped `whatsappRestClient` bean from the existing
      Spring Boot RestClient support.
- [x] Delete the zero-value `AiHttpClient` wrapper and generic configuration.
- [x] Verify the assistant test slice and dependency verification metadata.
- [ ] Repeat the same analysis for payment, notification, calendar, and
      Keycloak; preserve official SDKs where authentication/signing risk makes
      them safer than a hand-written Spring client.

### Task 15: Replace zero-value HTTP wrappers with named gateways

**Files:**

- Delete after caller migration: `modules/payment/src/main/java/com/emme/payment/configuration/PaymentHttpClient.java`
- Delete after caller migration: `modules/notification/src/main/java/com/emme/notification/configuration/NotificationHttpClient.java`
- Delete after caller migration: `modules/calendar/src/main/java/com/emme/calendar/configuration/GoogleHttpClient.java`
- ~~Delete after caller migration: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/AiHttpClient.java`~~ (done in Task 14A)
- Modify: provider classes under `modules/payment/src/main/java/com/emme/payment/adapter/out/provider/**`
- Modify: provider classes under `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/**`
- Modify: Google clients under `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/client/**`
- Modify: Keycloak client under `modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/KeycloakAdminClient.java`
- Test: existing provider contract tests plus MockWebServer/RestClient contract tests in each owning module

**Acceptance criteria:**

- Application ports use names such as `StripePaymentGateway`, `TwilioSmsSender`, `GoogleCalendarGateway`, and `KeycloakIdentityGateway`.
- Raw OkHttp request creation is removed from ordinary provider calls.
- Provider-specific idempotency, webhook signature, error, and retry semantics remain covered.
- Payment `authorize` and `capture` behavior is audited and tested before deletion of any wrapper.

- [ ] **Step 1: Write failing provider contract tests**

Cover successful request mapping, malformed provider response, 4xx validation,
401/403 configuration, 409 idempotent replay, 429 rate limit, timeout, 5xx,
and webhook signature failure. For Google/Keycloak, cover token expiry and
realm/calendar scope behavior.

- [ ] **Step 2: Run the provider tests**

```bash
./gradlew :modules:payment:test :modules:notification:test :modules:calendar:test :modules:identity:test --tests '*ProviderContractTest' --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Migrate callers and delete only zero-value wrappers**

Move request mapping to typed provider APIs, retain provider gateways as the
capability boundary, and delete each wrapper only when `rg` finds no callers,
configuration bean, test, or build dependency.

- [ ] **Step 4: Run tests, compile, and commit each provider group separately**

```bash
./gradlew :modules:payment:test :modules:notification:test :modules:calendar:test :modules:identity:test --no-parallel --no-configuration-cache
git add modules/payment modules/notification modules/calendar modules/identity
git commit -m "refactor(integrations): remove duplicate provider wrappers"
```

## 9. Phase G — Domain persistence waves

### Task 16: Standardize JPA aggregate persistence in foundational modules

**Files:**

- Modify: `modules/clients/src/main/java/com/emme/clients/adapter/out/persistence/**`
- Modify: `modules/services/src/main/java/com/emme/services/adapter/out/persistence/**`
- Modify: `modules/salon/src/main/java/com/emme/salon/adapter/out/persistence/**`
- Modify: `modules/clients/src/main/java/com/emme/clients/application/port/out/**`
- Modify: `modules/services/src/main/java/com/emme/services/application/port/out/**`
- Modify: `modules/salon/src/main/java/com/emme/salon/application/port/out/**`
- Test: corresponding repository, mapper, adapter, and application-service tests in each module

**Acceptance criteria:**

- Stable entity-backed CRUD uses Spring Data JPA repositories, projections, transaction boundaries, and optimistic locking where applicable.
- Mapping remains module-private and domain models remain framework-free.
- Pass-through helpers are removed only when the application port still communicates a meaningful module boundary or is intentionally replaced by a module service.
- Query count and N+1 behavior are covered for list/detail paths.

- [ ] **Step 1: Write failing repository and adapter tests**

For each aggregate, cover create, find, list, update, not-found, tenant filter,
version conflict, and read projection. Add a query-count assertion for list
operations that previously loaded entities one by one.

- [ ] **Step 2: Run the affected module tests**

```bash
./gradlew :modules:clients:test :modules:services:test :modules:salon:test --tests '*RepositoryTest' --tests '*PersistenceAdapterTest' --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Apply the standard entity/repository/mapper shape**

Use `SpringData<Aggregate>Repository`, JPA projections, `@Version`, and
`@Lock` where required. Keep application ports free of entity types and delete
only helpers whose behavior is fully represented by the repository/service.

- [ ] **Step 4: Run tests, architecture tests, compile, and commit each module group**

```bash
./gradlew :modules:clients:test :modules:services:test :modules:salon:test :modules:clients:compileJava :modules:services:compileJava :modules:salon:compileJava --no-parallel --no-configuration-cache
git add modules/clients modules/services modules/salon
git commit -m "refactor(domain): standardize foundational JPA persistence"
```

### Task 17: Make appointment collision handling concurrency-safe

**Files:**

- Modify: `modules/appointments/src/main/java/com/emme/appointments/adapter/out/persistence/adapter/AppointmentCollisionAdapter.java`
- Modify: `modules/appointments/src/main/java/com/emme/appointments/adapter/out/persistence/repository/SpringDataAppointmentRepository.java`
- Modify: `modules/appointments/src/main/java/com/emme/appointments/adapter/out/persistence/adapter/AppointmentPersistenceAdapter.java`
- Modify: appointment application mutation services under `modules/appointments/src/main/java/com/emme/appointments/application/service/**`
- Modify/create: appointment Liquibase migration under `database/src/main/resources/db/changelog/**`
- Test: `modules/appointments/src/test/java/com/emme/appointments/adapter/out/persistence/adapter/AppointmentCollisionAdapterTest.java`
- Test: `modules/appointments/src/test/java/com/emme/appointments/repository/AppointmentRepositoryTest.java`
- Test: `modules/appointments/src/integrationTest/**`

**Acceptance criteria:**

- Collision lookup uses a bounded indexed existence query or a database constraint appropriate to the scheduling invariant.
- Concurrent create/reschedule cannot both succeed when they overlap.
- JPA is attempted first for query/projection/locking; direct SQL or PostgreSQL exclusion/range constraints are retained only when the concurrency test proves they are required.
- User/tenant authorization remains in the application service.

**Current slice 17A — Tenant-scoped JPA pre-check and PostgreSQL invariant:**

- [x] Replace entity-list collision loading with a Spring Data `existsBy...` query filtered to `CONFIRMED` and `IN_PROGRESS`.
- [x] Keep collision ports tenant-scoped and remove unused non-tenant overloads/list-query methods.
- [x] Add a forward Liquibase migration with a PostgreSQL GiST exclusion constraint and active-appointment preflight.
- [x] Add migration contract coverage and a Testcontainers concurrency test asserting one commit and one `23P01` exclusion violation.
- [ ] Run the Testcontainers concurrency test with Docker available and verify it against the deployed migration path.

- [x] **Step 1: Write failing collision and concurrency tests**

Cover adjacent intervals, exact overlap, different staff/resource, different
tenant, cancellation freeing a slot, and two concurrent writes against the
same interval.

- [x] **Step 2: Run the focused unit/repository tests**

```bash
./gradlew :modules:appointments:test :modules:appointments:integrationTest --tests '*Collision*' --tests '*AppointmentRepository*' --no-parallel --no-configuration-cache
```

- [x] **Step 3: Implement the smallest query/constraint change**

Prefer a Spring Data existence/projection query and transaction lock. Add a
PostgreSQL migration only if a database exclusion/range invariant is necessary;
never rely on a Java pre-check alone for concurrent booking.

- [ ] **Step 4: Run unit/integration tests, compile, and commit**

The unit and H2 repository tests pass. The live PostgreSQL concurrency gate is
written but could not start because Docker is unavailable in the current
environment; Task 17 remains open until that gate runs successfully.

```bash
./gradlew :modules:appointments:test :modules:appointments:compileJava --no-parallel --no-configuration-cache
git add modules/appointments database/src/main/resources/db/changelog
git commit -m "fix(appointments): enforce collision invariant"
```

### Task 18: Standardize remaining entity modules

**Files:**

- Modify: `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/**`
- Modify: `modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/**`
- Modify: `modules/subscriptions/src/main/java/com/emme/subscriptions/adapter/out/persistence/**`
- Modify: `modules/documents/src/main/java/com/emme/documents/adapter/out/persistence/**`
- Modify: `modules/catalog/src/main/java/com/emme/catalog/adapter/out/persistence/**`
- Modify: `modules/calendar/src/main/java/com/emme/calendar/adapter/out/persistence/**`
- Modify: `modules/notification/src/main/java/com/emme/notification/adapter/out/persistence/**`
- Modify: `modules/payment/src/main/java/com/emme/payment/adapter/out/persistence/**`
- Test: corresponding `SpringData*RepositoryTest`, `*PersistenceAdapterTest`, and application-service tests

**Acceptance criteria:**

- Each stable aggregate has one module-private JPA repository and one clear mapping adapter where the port boundary is needed.
- Webhook, event, and payment idempotency state preserves unique constraints and atomic transitions.
- Document/catalog vector indexing remains a projection boundary; metadata CRUD is JPA-first.
- No module imports another module's entity or repository.

#### Current slice 18A — Remove zero-value document persistence mapper

The document JPA entities already own the complete persistence conversion
(`DocumentEntity.from/toDomain` and `DocumentChunkEntity.from/toDomain`).
`DocumentPersistenceMapper` only forwarded to those methods and required a
dedicated configuration bean, so it added indirection without preserving a
provider-neutral boundary. The application `DocumentRepository` port remains
unchanged and the adapter still owns all JPA repository access.

- [x] Add adapter contract coverage for document and chunk persistence.
- [x] Remove the redundant mapper and its zero-value configuration/package files.
- [x] Use ID-only aggregate CRUD inside the tenant-scoped connection and keep
      the explicit chunk replacement delete/flush/write sequence unchanged.
- [ ] Extend the same evidence-based review to the remaining entity modules.

#### Current slice 18B — Remove zero-value subscription persistence mapper

`SubscriptionEntity` already provides the complete aggregate conversion and
the subscription application port is provider-neutral. The mapper component
only delegated to those entity methods, so the adapter now depends directly on
the Spring Data repository and the entity mapping while preserving tenant-aware
lookup and update semantics.

- [x] Add adapter contract coverage for subscription rehydration.
- [x] Remove the redundant mapper component and package metadata.
- [x] Preserve the `SubscriptionRepository` port and tenant-keyed singleton lookup.
- [ ] Extend the same evidence-based review to the remaining entity modules.

#### Current slice 18C — Use ID-only customer lookups inside the tenant connection

The tenant boundary is established before persistence access: the tenant-scoped
DataSource resolves the authenticated schema and tenant session setting whenever
a connection is acquired. Because the Hikari pools are shared at the database
level (especially in `SHARED` mode), a pool initialization SQL statement cannot
bind one tenant schema safely. The customer adapter therefore keeps the simple
Spring Data `findById` contract; the connection/schema boundary and PostgreSQL
RLS provide isolation without duplicating tenant predicates in every CRUD lookup.

- [x] Add adapter coverage for an existing-customer update by ID.
- [x] Remove the redundant tenant-qualified Spring Data query.
- [x] Keep customer application reads and appointment callers ID-based.
- [x] Run the focused clients persistence test.
- [x] Record the connection-checkout isolation decision.

#### Current slice 18D — Use ID-only salon aggregate updates

Booking-policy, business-profile, and operating-hours are ordinary tenant-schema
CRUD. Their adapters use the tenant-scoped connection and Spring Data JPA, so
existing records are loaded with `findById`; tenant IDs remain domain/entity
data and are still protected by the database RLS contract. This avoids adding a
second tenant argument to provider-neutral repository implementations.

- [x] Add adapter coverage for ID-based booking-policy, business-profile, and
      operating-hours updates.
- [x] Remove the redundant `findByTenantIdAndId` methods.
- [x] Update all three adapter update branches to use `findById`.
- [x] Verify the focused salon adapter tests.

#### Current slice 18K — Remove tenant predicates from Salon schema-local reads

The tenant connection is selected before Salon persistence executes. Profile and
booking policy are tenant-local singletons, while operating hours are keyed by
day within that schema. Their JPA reads therefore use singleton/day queries
without repeating `tenantId`; tenant IDs remain in domain state and application
commands for authorization, creation, and response mapping.

- [x] Add adapter contract coverage for schema-local profile, policy, and day reads.
- [x] Replace tenant-qualified Spring Data methods with singleton/day methods.
- [x] Update Salon application services and appointment availability lookup.
- [x] Run Salon and appointment tests plus Spotless.
- [ ] Apply the same operation-by-operation review to remaining tenant-schema lists.

#### Current slice 18L — Remove tenant predicates from Clients schema-local lists

Customer list and name-search operations execute through the tenant-selected
JPA connection. The provider-neutral port now exposes `findAll()` and
`searchByName(String)`; application use cases retain the tenant argument at the
web/application boundary for authorization and request context, while the
adapter does not repeat the schema boundary in every query.

- [x] Add adapter coverage for schema-local list and name-search operations.
- [x] Replace tenant-qualified Spring Data methods with standard list/name methods.
- [x] Update customer application services and the repository integration test.
- [x] Run Clients and appointment tests plus Spotless.
- [ ] Continue the same operation-by-operation review for remaining tenant-schema lists.

#### Current slice 18M — Remove tenant predicates from Services schema-local lists

Artist listing and active service-catalog listing are tenant-schema reads. The
ports now use standard `findAll()` and status filtering, while appointment
authorization still receives the tenant ID at the application boundary and
uses it for actor/reference policy rather than repeating it in JPA predicates.

- [x] Add adapter contract coverage for artist and service schema-local lists.
- [x] Replace tenant-qualified Spring Data list/status methods.
- [x] Update service consumers and appointment reference validation.
- [x] Run Services and appointment tests plus Spotless.
- [ ] Continue the same operation-by-operation review for remaining tenant-schema lists.

#### Current slice 18N — Remove tenant predicates from Notification schema-local lists

Notification listing runs through the tenant-selected JPA connection. The
application query still carries the tenant ID as request and authorization
context, but the persistence port uses the standard `findAll()` operation so
the selected schema is the isolation boundary rather than a duplicated query
predicate. Provider-reference and callback operations remain explicit because
they may resolve a tenant before a tenant-scoped connection exists.

- [x] Add adapter contract coverage for schema-local notification listing.
- [x] Replace tenant-qualified Spring Data list methods with inherited `findAll()`.
- [x] Update the notification application list service and test fakes.
- [x] Run Notification tests, compilation, and Spotless.
- [ ] Continue the same operation-by-operation review for remaining tenant-schema lists.

#### Current slice 18O — Remove tenant predicates from Catalog schema-local reads

Catalog item listing runs through the tenant-selected JPA connection, so the
provider-neutral port now uses standard `findAll()`. Hybrid matching does not
load the entire catalog: after vector/text ranking, it uses Spring Data's
inherited `findAllById(...)` to fetch only matched items. The explicit tenant
ID remains at the search projection boundary because the shared hybrid-search
adapter still queries tenant-keyed vector/full-text data.

- [x] Add adapter contract coverage for schema-local list and matched-ID reads.
- [x] Replace the tenant-qualified Spring Data list method with inherited JPA methods.
- [x] Update catalog list and hybrid-match consumers.
- [x] Run Catalog tests, compilation, and Spotless.
- [ ] Continue the same operation-by-operation review for remaining tenant-schema lists.

#### Current slice 18P — Remove tenant predicates from Appointment schema-local reads

Appointment list and date-window queries execute after tenant schema selection,
so their JPA predicates no longer need to repeat `tenantId`. The port keeps
the operation's business filters and ordering, while collision/exclusion
queries retain explicit tenant keys because they enforce a shared database
invariant and are backed by PostgreSQL concurrency protections.

- [x] Add adapter contract coverage for ordered and date-window reads.
- [x] Replace tenant-qualified list/date Spring Data methods with schema-local methods.
- [x] Update appointment application consumers and repository integration coverage.
- [x] Run Appointments tests, compilation, Checkstyle, and Spotless.
- [ ] Continue the same operation-by-operation review for remaining tenant-schema lists.

#### Current slice 18Q — Remove tenant predicates from Document and chunk CRUD

Document metadata and chunks are owned by the tenant schema and accessed after
tenant connection selection. The JPA port now uses inherited `findAll()` and
ID/document-key methods without repeating `tenantId`; the search port retains
tenant scope for the shared vector/full-text projection, and the domain chunk
still carries its tenant identity for persistence and mapping.

- [x] Add failing adapter/service contract changes for schema-local document/chunk operations.
- [x] Replace tenant-qualified Spring Data document/chunk methods with schema-local methods.
- [x] Update document listing, chunking, retrieval, and search hydration callers.
- [x] Run Documents tests, compilation, Checkstyle, and Spotless.
- [ ] Continue the same operation-by-operation review for remaining tenant-schema lists.

#### Current slice 18R — Remove tenant predicates from Payment history listing

Payment history is read from the tenant-selected schema and now uses inherited
JPA `findAll()`. Provider-reference resolution remains tenant-qualified because
callback processing can identify the tenant before the tenant-scoped connection
is established; payment authority, webhook idempotency, and state transitions
are unchanged.

- [x] Add adapter contract coverage for schema-local payment listing.
- [x] Replace the tenant-qualified history query with inherited `findAll()`.
- [x] Update list service and in-memory test repositories.
- [x] Run Payment tests, compilation, Checkstyle, and Spotless.
- [ ] Continue the same operation-by-operation review for remaining tenant-schema lists.

#### Current slice 18S — Remove tenant predicates from Assistant conversation listing

Conversation aggregate listing is a tenant-schema-local JPA operation. The
provider-neutral repository uses inherited `findAll()`, while conversation
events, pending actions, channel-participant provider references, and other
history/workflow queries retain explicit keys because they are not generic
aggregate listing operations.

- [x] Add adapter contract coverage for schema-local conversation listing.
- [x] Replace tenant-qualified conversation JPA list methods with `findAll()`.
- [x] Remove the unused duplicate conversation status list declaration.
- [x] Run Assistant tests, compilation, Checkstyle, and Spotless.
- [ ] Continue the same operation-by-operation review for remaining tenant-schema lists.

#### Current slice 18T — Remove tenant predicates from Google Sheets listing

The Sheets controller already requires an authenticated tenant context, and
the list operation executes through the tenant-selected connection. The query
port and JPA adapter now use `findAll()`; tenant-qualified spreadsheet lookup
for re-export remains explicit because it resolves a provider/business key.

- [x] Add adapter contract coverage for schema-local spreadsheet listing.
- [x] Replace the tenant-qualified list method with inherited JPA `findAll()`.
- [x] Keep the controller tenant-context requirement and provider-key lookup.
- [x] Run Calendar tests, compilation, Checkstyle, and Spotless.
- [ ] Continue the same operation-by-operation review for remaining tenant-schema lists.

#### Push-gate correction — stabilize source contract traversal

The application push gate exposed a race in `KafkaEventContractTest`: walking
the entire `applications` tree allowed concurrent Spring Modulith documentation
generation to remove `build/spring-modulith-docs` during traversal. The test now
enumerates stable `src/main` and build-script roots, preserving dependency
contract coverage without traversing mutable build output.

- [x] Reproduce the missing-build-directory failure from the push gate.
- [x] Restrict the source scan to stable source and build-script roots.
- [x] Add the failure mode and prevention rule to engineering lessons.
- [x] Run focused Kafka/Modulith tests and Spotless.

#### Current slice 18E — Use ID-only service-catalog updates

Artist and service CRUD is already expressed cleanly with Spring Data JPA. The
tenant-scoped connection selects the tenant schema before the repository call,
so existing-record updates use the standard `findById` operation. Specialized
tenant-filtered list/status queries remain separate concerns and will be
reviewed by operation type rather than mechanically changing every method.

- [x] Add adapter coverage for ID-based artist and service updates.
- [x] Remove the redundant tenant-qualified derived queries.
- [x] Update both adapter update branches to use `findById`.
- [x] Run focused services tests, compilation, Checkstyle, and Spotless.

#### Current slice 18F — Use ID-only appointment updates

Appointments are ordinary tenant-schema CRUD for update and rehydration. The
adapter now uses standard JPA `findById` for an existing appointment, while
collision, list, and time-window operations retain their explicit operation
filters until their schema-routing and database-invariant contracts are audited
individually.

- [x] Add adapter coverage for an existing appointment update by ID.
- [x] Remove the redundant tenant-qualified appointment query.
- [x] Update the adapter to use `findById`.
- [x] Run the appointments module tests, compilation, Checkstyle, and Spotless.

#### Current slice 18G — Use ID-only document aggregate lookups

Document lifecycle operations and ordinary aggregate saves now use the standard
`findById` JPA operation inside the tenant-scoped connection. The document
application port no longer exposes a redundant tenant-plus-ID lookup. Chunk
replacement, bulk chunk reads, and hybrid search remain separate operations
because their tenant filters and replacement boundaries need an explicit audit.

- [x] Add a document service/adapter contract test for ID-based loading.
- [x] Remove the redundant tenant-qualified document aggregate lookup.
- [x] Update document lifecycle callers to use `findById`.
- [x] Keep chunk bulk/replace operations unchanged for their dedicated audit.
- [x] Run document tests, compilation, Checkstyle, and Spotless.

#### Current slice 18H — Use ID-only notification and payment CRUD lookups

Notification delivery/cancellation and payment state transitions operate inside
the tenant-scoped connection. Their application ports and Spring Data adapters
now use standard `findById` for aggregate identity, while provider-reference,
list, webhook-claim, and other operation-specific lookups retain explicit
tenant/business-key semantics.

- [x] Add notification and payment tests for ID-based aggregate loading.
- [x] Remove redundant tenant-qualified ID methods from both JPA repositories.
- [x] Simplify notification/payment mutation helpers and callers.
- [x] Preserve tenant-qualified provider-reference and list operations.
- [x] Run notification/payment tests, Checkstyle, and Spotless.

#### Current slice 18I — Use ID-only Assistant aggregate lookups

Conversation and pending-action are tenant-owned aggregates persisted through
the tenant-scoped JPA connection. Their provider-neutral ports and adapters now
use the inherited Spring Data `findById` contract; the tenant ID remains part
of commands, domain state, authorization/context checks, and child/event
queries. This keeps aggregate CRUD simple without weakening the explicit scope
of conversation history, participant, expiration, or list operations.

- [x] Add contract coverage for connection-scoped conversation and action reads.
- [x] Remove redundant tenant-qualified aggregate methods from Assistant ports
      and Spring Data repositories.
- [x] Simplify Assistant persistence adapters and service helper callers.
- [x] Preserve explicit tenant filters for child, list, expiration, and
      operation-specific queries.
- [x] Run Assistant tests, compilation, Checkstyle, and Spotless.

#### Current slice 18J — Use ID-only subscription aggregate saves

Subscription is a tenant-owned JPA aggregate. Provisioning runs after
`TenantActivated` and explicitly installs tenant context; public operations
also require the current tenant before entering the application service. The
aggregate is unique per tenant schema, so its lookup now follows the same
schema-local singleton pattern as Salon profiles and policies.

- [x] Add contract coverage for existing subscription saves.
- [x] Replace the redundant tenant-plus-ID save lookup with `findById`.
- [x] Replace the tenant-keyed singleton lookup with provider-neutral `find()`
      backed by JPA `findFirstByOrderByCreatedAtAsc()`.
- [x] Update the H2 full-context fixture to avoid cross-tenant test leakage
      without adding a production tenant predicate.
- [x] Run subscription tests, compilation, Checkstyle, and Spotless.

#### Current slice 18U — Use schema-local Subscription singleton reads

The Subscription lookup is now schema-local for all production callers. The
database schema guarantees one row per tenant (`UNIQUE (tenant_id)`), while
connection checkout selects the tenant schema and session. The application
port therefore exposes `find()` rather than repeating a tenant ID that the
connection has already applied. API query/command tenant IDs remain at the
authorization and domain boundaries.

- [x] Add adapter coverage for schema-local singleton rehydration.
- [x] Update all Subscription services and the provisioning listener path to
      use the context-selected `find()` operation.
- [x] Keep `findById` for existing aggregate saves.
- [x] Isolate the H2 full-context module test because it cannot emulate one
      physical schema per tenant in a shared in-memory database.
- [x] Run the Subscription module test, compilation, Checkstyle, and Spotless.

#### Current slice 18V — Remove dead Calendar list queries

Calendar sync-state and event-link repositories contained tenant-qualified
`findByTenantId` methods without any production caller. These methods added
surface area without providing a usable application capability, so they were
removed. The active sync-state lookup remains tenant/provider-qualified because
it is a business-key lookup and the schema currently does not guarantee one
state per provider. The event-link lookup remains deferred until its
provider-aware cardinality and idempotency contract is redesigned.

- [x] Verify production caller usage before removal.
- [x] Remove unused sync-state and event-link list methods.
- [x] Keep active provider/business-key operations unchanged.
- [x] Update the repository test and run Calendar quality gates.

#### Current slice 18W — Remove dead tenant-list declarations

Four tenant-qualified Spring Data list methods had no production or test
caller: artist capabilities, notification preferences, channel participants,
and identity memberships. They were removed from the concrete repositories
only; the active application ports and provider/business-key operations remain
unchanged. This reduces generated query surface and avoids implying a list
capability that the application does not expose.

- [x] Verify no callers before removal.
- [x] Remove unused derived methods and imports.
- [x] Run Services, Salon, Assistant, and Identity quality gates.

#### Current slice 18X — Use schema-local Assistant conversation queries

Conversation history and active pending-action reads execute through the
tenant-selected schema connection. Their existing ordering and status
predicates remain, but the redundant tenant predicate is removed from the
provider-neutral ports, adapters, and Spring Data repositories. Tenant IDs
remain in commands/domain records for RLS, event payloads, authorization, and
cross-tenant expiration/claim workflows; those specialized operations are not
collapsed into ordinary schema-local reads.

- [x] Add adapter tests for latest-event, ordered-history, and active-action
      reads.
- [x] Replace tenant-qualified conversation event methods with
      `findLatestByConversationId` and `findByConversationId`.
- [x] Replace the tenant-qualified active-action method with
      `findByConversationIdAndStatus`.
- [x] Run Assistant tests, compilation, Checkstyle, and Spotless.

#### Tenant isolation boundary correction

`TenantDatabasePoolProvider` caches pools by `databaseId`, not by tenant schema.
`TenantScopedDataSource#getConnection()` therefore applies `setSchema`, the
validated `search_path`, and `app.current_tenant_id` when a connection is taken
from the pool. `connectionInitSql` remains appropriate for the fixed core pool,
but not for dynamic tenant schema selection in a shared database pool.

The next repository audit will classify operations as: ID-only tenant-schema
CRUD; explicit tenant filters for shared/control-plane or cross-tenant jobs; and
named `JdbcClient` operations for atomic transitions, vectors/full-text, or
dynamic provisioning. The application ports remain provider-neutral throughout.

### Remaining tenant-qualified lookup audit

| Boundary | Decision | Reason / next action |
|---|---|---|
| Identity membership `findByIdAndTenantId` | Keep | `membership` is stored in the shared `emme_core` schema and supports authorization across tenant contexts; the explicit tenant predicate is part of the security contract. |
| Calendar event link `findByTenantIdAndAppointmentId` | Defer and redesign | `appointment_id` is not the event-link aggregate ID, the table permits multiple providers/links, and the current `Optional` contract has a cardinality risk. Add a provider-aware or list-based contract and a uniqueness/idempotency decision before changing it. |
| Calendar OAuth token `findByTenantIdAndUserIdAndPersonaType` | Keep | This is a tenant/user/persona business-key lookup, not aggregate identity; tenant and persona are required to select credentials safely. |
| Subscription singleton lookup | Converted | `TenantActivated` and web boundaries establish tenant context before access; schema-local JPA `findFirstByOrderByCreatedAtAsc()` replaces the redundant tenant predicate. |
| Calendar unused tenant list queries | Removed | No application caller used the methods; deleting them reduces Spring Data surface area without changing active provider/state contracts. |
| Unused tenant-list declarations in Services/Salon/Assistant/Identity | Removed | The methods had no callers or application-port capability; active relationship, preference, participant, and authorization lookups remain intact. |
| Assistant conversation aggregate and conversation-scoped reads | Converted | Conversation listing, ordered event history, and active-action reads now use schema-local JPA methods; expiration scans, claims/idempotency, and provider-channel lookups remain explicit for their cross-request or business-key semantics. |
| Documents metadata and chunks | Converted | Document listing, chunk-by-document, chunk-by-ID, and replacement delete now use schema-local JPA methods; the document search projection still keeps explicit tenant scope. |
| Catalog item metadata and ranked hydration | Converted | Catalog listing uses `findAll()` and ranked hydration uses `findAllById(...)`; shared vector/full-text search remains explicitly tenant-scoped. |
| Notification and payment history listing | Converted | Ordinary tenant-schema lists use JPA `findAll()`; notification/payment provider-reference and callback operations retain explicit tenant/business-key scope. |
| Shared vector/full-text projections and AI atomic stores | Keep | `JdbcClient` remains justified for pgvector/FTS/RRF, dynamic projection tables, atomic claims/idempotency, JSONB, and cross-tenant jobs; each adapter remains behind a provider-neutral port. |

- [x] **Step 1: Write failing per-module persistence tests for completed slices**

Completed slices 18K–18S cover schema-local reads in Salon, Clients, Services,
Notification, Catalog, Appointments, Documents, Payment, and Assistant. Each
slice was test-first and preserves the explicit tenant boundary for specialized
operations. Remaining module matrices still need to cover Calendar,
Subscriptions provisioning, Identity/control-plane access, and AI specialized
stores.

Use the same create/find/list/update/not-found/tenant/version/idempotency matrix
for each aggregate, adding webhook signature and duplicate-delivery cases to
notification/payment and projection rebuild cases to documents/catalog.

- [ ] **Step 2: Run each module group separately**

```bash
./gradlew :modules:tenancy:test :modules:identity:test :modules:subscriptions:test :modules:documents:test :modules:catalog:test :modules:calendar:test :modules:notification:test :modules:payment:test --tests '*RepositoryTest' --tests '*PersistenceAdapterTest' --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Convert only the proven JPA candidates**

Use projections and locking before custom SQL. Keep specialized vector,
generated-FTS, webhook, and atomic state-transition SQL in named adapters when
the ledger says JPA is less clear or unsafe.

- [ ] **Step 4: Run tests, architecture checks, compile, and commit in two groups**

```bash
./gradlew :modules:tenancy:test :modules:identity:test :modules:subscriptions:test :modules:documents:test :modules:catalog:test :modules:calendar:test :modules:notification:test :modules:payment:test --no-parallel --no-configuration-cache
git add modules/tenancy modules/identity modules/subscriptions modules/documents modules/catalog modules/calendar modules/notification modules/payment
git commit -m "refactor(domain): standardize remaining JPA persistence"
```

## 10. Phase H — Events, Redis, libraries, and build foundations

### Task 19: Standardize Modulith events and Kafka boundaries

#### Current slice 19A — Keep tenant event publication behind a port

`CreateTenantService` no longer imports Spring event infrastructure. The
application service publishes `TenantCreated` through the provider-neutral
`TenantEventPublisher` port, while `SpringTenantEventPublisher` remains the
single Modulith adapter. This preserves provider substitution and the
transactional event flow without introducing a Kafka dependency into the
application layer.

- [x] Add a service test proving tenant creation publishes through the port.
- [x] Add the Spring Modulith publisher adapter.
- [x] Remove `ApplicationEventPublisher` from the application service.
- [ ] Complete the listener idempotency, context reconstruction, and Kafka
      externalization gates.

#### Current slice 19B — Keep WhatsApp event publication behind a port

`ProcessWhatsAppMessageService` now depends on the provider-neutral
`WhatsAppMessageEventPublisher` port. `SpringWhatsAppMessageEventPublisher`
contains the Spring Modulith publication mechanic, so the application service
no longer imports Spring event infrastructure. The inbound
`WhatsAppMessageReceivedListener` is also an adapter: it restores the event's
tenant/database context and delegates to `ProcessWhatsAppMessageUseCase`.
The existing durable webhook claim and tenant validation remain unchanged.

- [x] Update the enqueue contract test to use the provider-neutral publisher.
- [x] Add the WhatsApp event publisher port and Spring adapter.
- [x] Remove Spring event infrastructure and the redundant constructor overload
      from the application service.
- [x] Move the Modulith listener and event-context reconstruction into the
      inbound messaging adapter.
- [x] Run focused assistant tests and architecture checks.
- [ ] Complete listener duplicate-delivery and external Kafka boundary tests.

#### Current slice 19C — Make externalized appointment replay independent of web security

`AppointmentCreated` is an externalized public fact and may be delivered by
Kafka after the originating request has completed. The Identity consumer now
uses the event's `customerId` and `tenantId` directly, so replay does not depend
on a request-local Spring Security context. The existing membership use case is
idempotent, preserving safe duplicate delivery behavior. The listener also has
an explicit stable Modulith identifier.

- [x] Add a failing listener test with no `SecurityContext`.
- [x] Remove request-local authentication from the durable consumer.
- [x] Preserve the provider-neutral membership use-case boundary.
- [ ] Add live duplicate-delivery/replay coverage to the Kafka integration gate.

**Files:**

- Modify: event publishers/listeners under `modules/assistant/src/main/java/com/emme/assistant/**`
- Modify: `modules/appointments/src/main/java/com/emme/appointments/adapter/out/messaging/publisher/SpringAppointmentEventPublisher.java`
- Modify: `modules/identity/src/main/java/com/emme/identity/adapter/in/messaging/consumer/**`
- Modify: `modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/messaging/consumer/**`
- Modify: `modules/subscriptions/src/main/java/com/emme/subscriptions/adapter/in/messaging/consumer/SubscriptionProvisioningListener.java`
- Modify: `applications/emme-platform/src/test/java/com/emme/KafkaEventStreamingIntegrationTest.java`
- Test: module event publication/listener tests and integration tests under `applications/emme-platform/src/integrationTest/**`

**Acceptance criteria:**

- In-process module interactions use typed Spring Modulith events and the publication registry.
- Kafka is used only for events that need external consumers, independent replay, partitioning, or delivery boundaries.
- Listeners are idempotent, classify duplicate/no-op versus operational failure, and preserve tenant/correlation context.
- No custom publication/retry table duplicates the Modulith registry.

- [ ] **Step 1: Write failing publication/listener tests**

Test publication inside the business transaction, listener retry, duplicate
delivery, tenant context reconstruction, Kafka partition key, and failed
publication recovery.

- [ ] **Step 2: Run focused event tests**

```bash
./gradlew :modules:assistant:test :modules:appointments:test :modules:identity:test :modules:tenancy:test :modules:subscriptions:test --tests '*Event*' --tests '*Listener*' --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Remove duplicated mechanics**

Replace custom in-process queues/retry bookkeeping with Modulith publication
configuration. Keep typed event contracts and business listener policy. Add
Kafka externalization only for existing selected external event boundaries.

- [ ] **Step 4: Run tests, integration checks, and commit**

```bash
./gradlew :modules:assistant:test :applications:emme-platform:test --tests '*KafkaEventStreamingIntegrationTest' --no-parallel --no-configuration-cache
git add modules applications/emme-platform/src/test/java/com/emme/KafkaEventStreamingIntegrationTest.java
git commit -m "refactor(events): standardize Modulith event boundaries"
```

### Task 20: Simplify Redis and semantic hot-state wiring

**Files:**

- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiRedisConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiRedisSemanticConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/redis/RedisAiOperationalStateAdapter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/RedisSemanticCacheHotStore.java`
- Modify: `modules/identity/src/main/java/com/emme/identity/adapter/out/ratelimit/RedisLoginAttemptRateLimiter.java`
- Test: Redis unit/integration tests under assistant and identity

**Acceptance criteria:**

- Boot/Spring Data Redis manages the connection and serialization path where possible.
- Redis keys are tenant/principal scoped where data is personalized and include version/model identity where semantic data requires it.
- Cache/vector eviction and Redis outages produce safe misses or documented rate-limit behavior; durable state is never lost.
- Native Redis/Jedis use remains only for a tested atomic primitive unavailable or materially less clear through Spring abstractions.

- [ ] **Step 1: Write failing Redis tests**

Cover TTL, eviction, tenant-key isolation, Redis unavailable, atomic compare and
delete, rate limit boundary, vector metadata filter, embedding contract, and
durable fallback.

- [ ] **Step 2: Run focused Redis tests**

```bash
./gradlew :modules:assistant:test :modules:identity:test --tests '*Redis*' --tests '*Semantic*' --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Implement managed wiring and narrow native access**

Replace direct client construction with Spring-managed connection factories
where behavior is equivalent. Keep custom cache policy/admission and direct
atomic operations only in named adapters.

- [ ] **Step 4: Run tests and commit**

```bash
./gradlew :modules:assistant:test :modules:identity:test :modules:assistant:compileJava --no-parallel --no-configuration-cache
git add modules/assistant modules/identity
git commit -m "refactor(redis): simplify hot-state infrastructure"
```

#### Current slice 20A — Standardize the tenancy Redis template boundary

The tenancy HTTP rate-limit interceptor now requires Spring Data Redis's
`StringRedisTemplate`, matching the operational-state adapter, live-event
publisher, login-attempt limiter, calendar OAuth flow, and shared test fixture.
The broader `RedisTemplate<String, String>` type was technically compatible
but allowed an object/binary serializer bean to activate a string-keyed rate
limiter, which could produce unreadable keys or runtime serialization errors.

Jedis remains limited to the Spring AI Redis vector-store and native set/index
operations in the semantic hot projection. The semantic `RedisClient` is
already Spring-managed with `destroyMethod = "close"`; it is not replaced by a
second generic template because the official Spring AI Redis vector store
requires the Jedis client and the hot projection uses Redis-native index
operations that are not smaller through `StringRedisTemplate`.

- [x] Add a failing contract test for the required Redis template type.
- [x] Change the interceptor constructor and conditional bean boundary to
      `StringRedisTemplate`.
- [x] Run the tenancy check, including Spotless, Checkstyle, compilation, and
      tests.
- [ ] Continue Redis outage/eviction and semantic metadata integration gates
      with Docker-enabled Redis.

### Task 21: Split generic and feature-specific test fixtures

**Files:**

- Modify: `libraries/testing/build.gradle.kts`
- Modify: `modules/tenancy/src/testFixtures/java/com/emme/tenancy/testing/BaseTenantModuleTest.java`
- Modify: `libraries/testing/src/testFixtures/java/com/emme/testing/BaseWebTest.java`
- Modify: `modules/identity/src/testFixtures/java/com/emme/identity/testing/MockIdentityProviderAdministrationConfig.java`
- Modify: `modules/tenancy/src/testFixtures/java/com/emme/testing/TestBootstrapJdbcConfig.java`
- Modify: `libraries/testing/src/testFixtures/java/com/emme/testing/TestSecurityConfig.java`
- Create/modify: feature fixtures under `modules/identity/src/testFixtures/**`, `modules/tenancy/src/testFixtures/**`, `modules/salon/src/testFixtures/**`, and `modules/subscriptions/src/testFixtures/**`
- Test: fixture-consuming module tests across `modules/*/src/test/**`

**Acceptance criteria:**

- Generic testing library no longer depends on concrete identity, salon, subscription, or tenancy production types.
- Concrete Keycloak subclassing and broad infrastructure mocks are replaced by protocol fakes or module-owned test configuration.
- Bootstrap JDBC test configuration is consumed only by bootstrap tests.
- Modules compile with only the fixtures they use.

- [ ] **Step 1: Write failing fixture dependency/architecture test**

Add `libraries/testing/src/testFixtures/java/com/emme/testing/TestingFixtureDependencyTest.java` to assert generic fixtures do not reference feature package names, concrete provider clients, or feature-specific repositories.

- [ ] **Step 2: Run the test and compile affected fixtures**

```bash
./gradlew :libraries:testing:test :libraries:testing:compileTestFixturesJava --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Move only feature fixtures to owning modules**

Keep generic JWT, web, Spring context, and container helpers in
`libraries/testing`. Create module test fixtures with protocol fakes and move
feature imports/tests one module at a time.

- [ ] **Step 4: Run representative module tests and commit**

```bash
./gradlew :libraries:testing:test :modules:identity:test :modules:tenancy:test :modules:assistant:test --no-parallel --no-configuration-cache
git add libraries/testing modules/identity modules/tenancy modules/salon modules/subscriptions modules/assistant
git commit -m "refactor(testing): isolate feature fixtures"
```

#### Current slice 21A — Move tenant provisioning out of the generic web fixture

`BaseWebTest` now contains only generic Spring Security/MockMvc helpers. The
tenant-creation use case and tenant result mapping live in the tenancy-owned
`TenantWebTest` test fixture, and only web-test consumers that provision a
tenant depend on that fixture. This keeps provider-neutral test infrastructure
separate from tenancy policy without changing production code.

- [x] Add a failing architecture test for tenancy-free `BaseWebTest`.
- [x] Add `modules/tenancy`'s tenant-aware web fixture.
- [x] Migrate tenant-provisioning web tests to `TenantWebTest`.
- [x] Compile all affected fixtures/tests and run representative web tests.
- [ ] Continue separating `BaseSpringModuleTest` feature setup and provider fakes.

#### Current slice 21C — Use the identity-provider port in test fixtures

`MockKeycloakAdminClientConfig` was a concrete adapter subclass that required
Keycloak client configuration and HTTP dependencies in every full-context test.
It is replaced by `MockIdentityProviderAdministrationConfig`, which supplies a
no-op `IdentityProviderAdministrationPort` implementation. Production
composition remains unchanged; the test seam now follows the same port-first
boundary as the application listener.

- [x] Add a failing test requiring the fixture bean to return the provider port.
- [x] Replace concrete Keycloak subclassing with a provider-port fake.
- [x] Rename the fixture to communicate its capability rather than its vendor.
- [x] Run focused identity tests and affected fixture compilation.
- [ ] Remove remaining feature-specific setup from `BaseSpringModuleTest` in a
      dependency-safe follow-up slice.

#### Current slice 21D — Move tenant-aware full-context setup to tenancy fixtures

The full-context base class and H2 bootstrap configuration are now owned by
the tenancy test-fixture artifact. Generic `libraries/testing` retains only
shared web, repository, security, and container helpers. All module consumers
that need tenant provisioning use `BaseTenantModuleTest`; tenant web tests
receive the bootstrap override through the tenancy-owned `TenantWebTest`.

- [x] Add a failing fixture-boundary architecture test.
- [x] Move `BaseSpringModuleTest` to
      `modules/tenancy/.../BaseTenantModuleTest.java`.
- [x] Move `TestBootstrapJdbcConfig` to tenancy test fixtures.
- [x] Migrate full-context module tests and fixture dependencies.
- [x] Remove feature-module dependencies from `libraries/testing`.
- [x] Make the direct tenancy web test import its tenancy bootstrap fixture explicitly.
- [x] Run all affected test compilation and style checks.
- [ ] Split the remaining identity, salon, and subscription setup from
      `BaseTenantModuleTest` only after a dependency-safe fixture design is
      established.

### Task 22: Remove duplicate Gradle capabilities and dependencies

**Files:**

- Modify: `modules/booking/build.gradle.kts`
- Modify: `modules/catalog/build.gradle.kts`
- Modify: `modules/assistant/build.gradle.kts`
- Modify: `applications/emme-platform/build.gradle.kts`
- Modify: all modules that explicitly reapply `emme.testing`
- Modify: `build-logic/src/main/kotlin/emme.persistence.gradle.kts`
- Modify: `build-logic/src/main/kotlin/emme.testing.gradle.kts`
- Modify: `build-logic/src/main/kotlin/emme.messaging.gradle.kts`
- Modify: `platform/build.gradle.kts`
- Test: build-logic tests under `build-logic/src/test/**`; compile and dependency-analysis tasks

**Acceptance criteria:**

- `modules/booking` contains one `kernel` dependency and no unused persistence/web/integration plugin for its placeholder boundary.
- `modules/catalog` contains one `kernel` dependency and only source-used capabilities.
- `modules/assistant` contains one security-test dependency.
- Conventions are applied once through the plugin chain; application-level Modulith is not duplicated.
- Platform constraints use the narrowest safe scope after dependency analysis.

- [ ] **Step 1: Write failing convention/dependency tests**

Add build-logic tests that apply representative convention plugins to a
temporary project and assert plugin application counts and expected
configuration names. Add a repository check for duplicate dependency notation
in the known module build files.

- [x] **Step 2: Run build-logic tests and dependency analysis**

```bash
./gradlew :build-logic:test --no-parallel --no-configuration-cache
./gradlew :modules:assistant:computeActualUsageMain :modules:assistant:computeAdvice \
  :modules:booking:computeActualUsageMain :modules:booking:computeAdvice \
  :modules:catalog:computeActualUsageMain :modules:catalog:computeAdvice \
  --no-daemon --no-parallel --no-configuration-cache
```

The repository has no root `dependencyAnalysis` task. The dependency-analysis
plugin exposes per-project `computeActualUsage*` and `computeAdvice` tasks, but
the first representative run is currently blocked by the plugin's ASM parser:
the Gradle daemon runs on Java 25 and the plugin fails on class-file major
version 69 before producing advice. Keep the source-level duplicate-declaration
test as the deterministic guard until the plugin/toolchain compatibility is
resolved; do not mark dependency advice as complete from a failed run.

- [ ] **Step 3: Remove duplicate declarations and split conventions only where measured**

Remove repeated lines and redundant plugins first. Split persistence/testing
conventions only when the source/dependency report shows a real capability
overprovisioning benefit that outweighs new convention names.

- [ ] **Step 4: Run compile and build checks, then commit**

```bash
./gradlew compileJava dependencyAnalysis --no-parallel --no-configuration-cache
git add build-logic platform modules applications
git commit -m "build: remove duplicate framework capabilities"
```

#### Current slice 22A — Remove duplicate module dependency declarations

The known duplicate declarations were limited to redundant Gradle notation:
`modules/booking` declared `libraries:kernel` six times, `modules/catalog`
declared it three times, and `modules/assistant` declared the Spring Security
test dependency twice. A repository inventory test now guards one declaration
per dependency, and only the redundant lines were removed. Plugin/convention
changes remain separate until dependency analysis demonstrates a real benefit.

- [x] Add a failing repository dependency-duplication test.
- [x] Remove redundant `libraries:kernel` declarations from booking and catalog.
- [x] Remove the duplicate Spring Security test dependency from assistant.
- [x] Run the repository inventory test, affected compilation, and Spotless.
- [x] Record the actual per-project dependency-analysis task names and Java 25
      class-file compatibility blocker.
- [ ] Complete convention-plugin and dependency-analysis follow-up slices once
      the analysis plugin supports the configured Java toolchain.

## 11. Phase I — Database, deployment, and final cleanup

### Task 23: Add database ownership and migration contract checks

**Files:**

- Modify: `database/src/main/resources/db/changelog/db.changelog-master.yaml`
- Modify: `database/src/main/resources/db/changelog/**` only with new forward migrations
- Create/modify: `database/src/test/java/com/emme/database/*MigrationContractTest.java`
- Modify: `database/docker/run-migrations.sh`
- Test: existing database tests and PostgreSQL Testcontainers migration tests

**Acceptance criteria:**

- Every module-owned durable table has documented owner and migration location.
- RLS policies enforce tenant isolation for tenant-scoped durable tables.
- Required pgvector, generated FTS, HNSW, AGE, unique, and version constraints are present.
- Migration scripts validate slugs/schema identifiers and return non-zero on partial failure.
- Deployed migrations are never edited in place.

- [ ] **Step 1: Write failing migration contract tests**

Test table ownership metadata, RLS enabled/policies, unique idempotency keys,
version columns, vector dimensions/indexes, generated `tsvector`, and tenant
schema provisioning state. Test invalid tenant slug/schema input in the shell
script with a disposable PostgreSQL environment.

- [ ] **Step 2: Run the database contract suite**

```bash
./gradlew :database:test --tests '*MigrationContractTest' --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Add only required forward migrations and script fixes**

Use Liquibase for schema changes and keep SQL-specific behavior in the
database. Align runtime and script validation through tests rather than copying
unsafe SQL interpolation into application code.

- [ ] **Step 4: Run migration tests and commit**

```bash
./gradlew :database:test :database:compileJava --no-parallel --no-configuration-cache
git add database
git commit -m "test(database): enforce framework-first persistence contracts"
```

### Task 24: Standardize deployment, health, and CI gates

**Files:**

- Modify: `.github/workflows/ci-backend.yml`
- Modify: `.github/workflows/**` affected by duplicate build/security checks
- Modify: `deployment/compose/**`
- Modify: `infra/kubernetes/**`
- Delete after validation: `deployment/compose/compose.environment-e2e.yaml.bak`
- Modify: `gradle/environments/**`
- Test: compose/Kubernetes smoke scripts and CI configuration validation

**Acceptance criteria:**

- Fast affected-module checks run before expensive integration gates.
- Full integration, security, dependency, startup, and E2E gates remain available at phase/release boundaries.
- Migration jobs, application health probes, Redis/Kafka/PostgreSQL dependencies, and secrets have one documented source per environment.
- Stale `.bak` configuration is deleted only after the active E2E overlay is proven equivalent.

- [ ] **Step 1: Write failing configuration checks**

Validate every referenced environment variable, health URL, compose profile,
Kubernetes probe, migration job dependency, and CI Gradle task. Assert the
final quality command includes compile, tests, Spotless, Checkstyle, coverage,
security/dependency analysis, and architecture tests.

- [ ] **Step 2: Run configuration validation**

```bash
./gradlew tasks appConfig --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Simplify duplicate operational wiring**

Keep environment-specific overrides explicit. Remove only duplicate profiles,
stale files, and repeated CI task definitions proven by the configuration tests.

- [ ] **Step 4: Run smoke checks and commit**

```bash
./gradlew check --no-parallel --no-configuration-cache
git add .github deployment infra gradle
git commit -m "chore(ops): standardize repository verification gates"
```

### Task 25: Remove verified compatibility classes and dependencies

**Files:**

- Delete: duplicate AI contracts and provider wrappers listed in the migration ledger
- Delete: `modules/payment/.../PaymentHttpClient.java`, `modules/notification/.../NotificationHttpClient.java`, `modules/calendar/.../GoogleHttpClient.java`, and `modules/assistant/.../AiHttpClient.java` only after Task 15
- Delete: `modules/ai-platform/.../SpringAiModelProvider.java` only after Task 4
- Modify: `gradle/libs.versions.toml`, `platform/build.gradle.kts`, and owning build files
- Modify: package-info and architecture tests with stale module names
- Test: all affected module tests and architecture/dependency checks

**Acceptance criteria:**

- `rg` finds no caller, bean, import, test, or dependency for each deleted class.
- Replacement integration tests pass for every deleted capability.
- No serialized event/workflow contract changes occur without compatibility coverage.
- Unused provider/JDBC/HTTP dependencies are removed from the narrowest owning scope.

- [ ] **Step 1: Write failing deletion-candidate tests**

Add a repository inventory test that fails when a ledger item marked ready for
deletion still has a source caller, bean declaration, import, or build
dependency.

- [ ] **Step 2: Run caller and dependency searches**

```bash
rg -n 'SpringAiModelProvider|PaymentHttpClient|NotificationHttpClient|GoogleHttpClient|AiHttpClient|JdbcTemplate|NamedParameterJdbcTemplate' modules libraries applications tools
./gradlew dependencyAnalysis --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Delete one compatibility family at a time**

Delete only after its ledger condition, focused tests, architecture tests, and
compilation pass. Update names and documentation in the same logical family;
do not mix unrelated formatting changes.

- [ ] **Step 4: Run affected tests and commit each family**

```bash
./gradlew :modules:assistant:test :modules:ai-platform:test :modules:payment:test :modules:notification:test :modules:calendar:test --no-parallel --no-configuration-cache
git add modules libraries platform gradle
git commit -m "refactor: remove verified compatibility layers"
```

#### Current slice 13D — Validate Hibernate schema identifiers at the provider boundary

Completed in this slice:

- The Hibernate multi-tenant connection provider validates every non-core
  schema identifier immediately before acquiring a tenant connection.
- The existing `TenantSchemaName` validator remains the single validation
  policy; no SQL quoting or dynamic identifier concatenation was introduced.
- Invalid identifiers are rejected before `TenantDatabasePoolProvider` is
  touched, protecting direct Hibernate/provider calls as well as the normal
  tenant checkout path.

- [x] Add a failing provider test for an invalid schema identifier.
- [x] Validate the identifier before tenant pool acquisition and schema
      selection.
- [x] Run focused tenancy tests, Spotless, and Checkstyle.
- [ ] Run the PostgreSQL/Testcontainers startup and schema-routing gate when
      Docker is available.

#### Current slice 8C — Centralize AI staff-role policy

Completed in this slice:

- The repeated staff-role representation set is now owned by
  `AiStaffRolePolicy` in the provider-neutral application security package.
- Quote review, conversation resume, workflow persistence, and LangGraph
  checkpoint authorization all use that one policy.
- Controller-level Spring Security annotations remain unchanged; this utility
  is the defense-in-depth policy used after an authenticated AI context exists.

- [x] Add policy characterization tests for supported and non-staff roles.
- [x] Replace duplicated role sets in services and adapters.
- [x] Add package metadata and run assistant tests, compilation, Spotless, and
      Checkstyle.
- [ ] Reconcile the role vocabulary with the identity module's eventual
      centralized authorization contract before changing serialized/auth tokens.

#### Current slice 21B — Keep generic H2 fixtures free of PostgreSQL provisioning

Completed in this slice:

- The tenancy-owned `TestBootstrapJdbcConfig` now supplies a primary no-op
  `TenantSchemaMigrationPort` for H2 module tests.
- Generic test fixtures no longer pull the database module just to expose
  PostgreSQL Liquibase resources.
- Generic web fixtures no longer import tenant bootstrap configuration.
- PostgreSQL tenant schema migration remains covered by the real adapter and
  is reserved for the Testcontainers/integration source set.

- [x] Add a fixture contract test for the no-op migration boundary.
- [x] Remove the unnecessary database runtime dependency from generic testing
      fixtures.
- [x] Apply the H2 provisioning override to tenancy-owned web fixtures as well
      as the shared full-context module fixture.
- [x] Verify tenancy tests complete without asynchronous PostgreSQL migration
      errors in H2 contexts.
- [ ] Run the real Liquibase migration against PostgreSQL/Testcontainers when
      Docker is available.

#### Current slice 18Y — Restore tenant context for Calendar event processing

Completed in this slice:

- `StaffCalendarSyncAdapter` now restores the tenant from each durable
  `CalendarSyncRequested` event before invoking tenant-scoped JPA use cases.
- Staff-token enumeration uses schema-local JPA `findAll()` instead of a
  redundant `findByTenantId` predicate; the connection-selected tenant schema
  is now the isolation boundary.
- Interactive OAuth credential selection remains explicitly keyed by tenant,
  user, and persona because those fields define the provider/business lookup,
  not an ordinary schema-local list.
- Failure handling remains inside the restored context so status updates also
  use the correct tenant connection.

- [x] Add a failing listener regression test for tenant-context restoration.
- [x] Convert the staff-token list to schema-local `findAll()`.
- [x] Run focused Calendar listener and OAuth repository tests.
- [ ] Continue the Calendar event-link provider-cardinality audit before
      changing appointment/provider lookups.

## 12. Subagent-driven execution protocol

Subagents are the default for independent work, as requested. The coordinator
must keep shared contracts, database migrations, and composition-root changes
sequential.

### 12.1 Safe parallel assignments

After Tasks 1–4 establish the ledger and canonical contracts, dispatch fresh
contexts for:

| Subagent | Isolated assignment | Required output |
|---|---|---|
| A | Spring AI tools/structured extraction (Task 5) | Tests, implementation, commit SHA, behavior notes |
| B | RAG/vector/cache review (Task 6) | Tests, implementation, SQL retention rationale, commit SHA |
| C | LangGraph boundary (Tasks 7–8) | Topology/security tests, workflow changes, commit SHA |
| D | AI persistence classification (Task 9) | Completed ledger and classification tests, no unapproved broad rewrite |
| E | Provider HTTP inventory and one provider migration (Tasks 14–15) | Provider contract tests, typed client change, commit SHA |
| F | Test fixture split (Task 21) | Fixture dependency test, moved fixtures, commit SHA |

Do not parallelize tasks that edit the same contract files, version catalog,
Liquibase master/changelog, shared build convention, or application composition
root. Rebase/merge one reviewed commit at a time and run the checkpoint gate
before dispatching the next dependent group.

### 12.2 Required subagent handoff

Each subagent reports:

1. files changed and files deliberately not changed;
2. failing test observed before implementation;
3. focused tests and compilation results;
4. remaining risk or environment limitation;
5. commit SHA and clean scoped diff.

The coordinator reviews the diff against the design and runs the task's
checkpoint tests before accepting the commit. A subagent must not delete a
compatibility class or alter a database migration without the ledger condition
and replacement evidence.

## 13. Checkpoints and verification commands

### Checkpoint A — after Tasks 1–6

- [ ] AI contracts have one canonical capability per operation.
- [ ] `ai-contracts` has no framework/provider imports.
- [ ] Spring AI chat/tools/RAG construction has one production path.
- [ ] Focused assistant, ai-platform, shared, and database tests pass.
- [ ] Affected compilation and architecture tests pass.

```bash
./gradlew :libraries:ai-contracts:test :modules:ai-platform:test :modules:assistant:test :modules:shared:test :database:test --no-parallel --no-configuration-cache
```

### Checkpoint B — after Tasks 7–13

- [ ] LangGraph is limited to proven resumable complexity.
- [ ] AI JDBC stores are classified and stable CRUD has a tested JPA path.
- [ ] Remaining `JdbcClient` code is named and justified.
- [ ] Tenant bootstrap JDBC is isolated from application policy.
- [ ] Tenant provisioning duplicate/failure behavior is integration-tested.

```bash
./gradlew :modules:assistant:test :modules:ai-platform:test :modules:tenancy:test :modules:subscriptions:test :modules:shared:test --no-parallel --no-configuration-cache
```

### Checkpoint C — after Tasks 14–22

- [ ] Provider gateways use typed Spring HTTP clients or justified official SDKs.
- [ ] Redis and Modulith behavior is safe under outage/retry/duplicate delivery.
- [ ] Generic test fixtures no longer depend on feature modules.
- [ ] Duplicate Gradle declarations and unused placeholder capabilities are removed.

```bash
./gradlew test compileJava dependencyAnalysis --no-parallel --no-configuration-cache
```

### Final repository gate — after Tasks 23–25

- [ ] Full Java compilation and all unit/integration suites pass with zero failures/skips.
- [ ] PostgreSQL/RLS/Liquibase/pgvector/AGE migration contracts pass.
- [ ] Redis/Kafka/Testcontainers integration passes in an environment with Docker.
- [ ] Application startup, migration jobs, health probes, authenticated API, webhook, and E2E flows pass.
- [ ] Spotless, Checkstyle, JaCoCo threshold, Sonar/dependency/security checks pass.
- [ ] Load-test baseline shows no regression in JPA queries, JDBC claims, vector search, Redis hit/miss, provider calls, or workflow latency.
- [ ] `git diff --check` passes and all generated artifacts are committed/pushed.

```bash
./gradlew check integrationTest e2eTest --no-parallel --no-configuration-cache
./gradlew spotlessCheck checkstyleMain jacocoTestCoverageVerification dependencyAnalysis --no-parallel --no-configuration-cache
```

## 14. Definition of done

- [ ] Every task has a failing test or explicit inventory/architecture test before implementation.
- [ ] Every task leaves the repository compilable and its focused tests green.
- [ ] Every remaining custom adapter has a recorded reason, owner, test category, and rollback.
- [ ] Every deleted class has no remaining caller/configuration/dependency and replacement evidence.
- [ ] Spring AI, JPA, Redis, Modulith, Kafka, and LangGraph4j are used only for responsibilities they simplify or uniquely provide.
- [ ] Tenant isolation, authorization, idempotency, audit, observability, and payment authority are preserved.
- [ ] Names communicate capability and ownership without unnecessary implementation leakage.
- [ ] The final quality gates and operational checks pass, or exact environment blockers are recorded in the migration ledger.
- [ ] Changes are committed atomically, pushed to `feat/ai-platform-foundation`, and the remote tip is verified.

## 15. Plan review gaps to resolve before execution

- The exact canonical embedding name (`EmbeddingPort` versus `EmbeddingService`) must be selected by caller search in Task 3; the plan intentionally prevents two identical contracts but does not invent a public API name without usage evidence.
- Keycloak and Google provider SDK adoption requires a protocol/auth/error comparison in Task 14; Spring HTTP interfaces remain the default.
- Appointment exclusion/range constraints require PostgreSQL concurrency evidence in Task 17; a JPA existence query is the default first attempt.
- The current platform patch upgrade to the latest compatible stable version is a separate maintenance change and is not part of the first refactoring commits.
