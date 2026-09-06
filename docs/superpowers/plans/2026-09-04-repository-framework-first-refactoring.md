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
- Use enums or value objects for finite domain states at application, domain, and persistence boundaries. Do not encode lifecycle states as raw Java strings; keep database/API serialization compatible at the adapter boundary.
- Every implementation task writes the failing test first, runs the focused test, implements the minimum, refactors only after green, and commits one logical slice.
- Do not edit deployed Liquibase migrations in place; add forward migrations and migration-contract coverage.
- Do not combine dependency upgrades with behavioral refactors. Upgrade to the latest compatible stable patch in a separate platform task.
- Preserve unrelated worktree changes and stage only files belonging to the current task.
- Final repository-wide Spotless, Checkstyle, compilation, coverage, integration, startup, E2E, security, and performance gates run after the gradual waves; affected-module gates run before each slice commit.

### Current slice — Identity membership enum boundary

Identity membership status now uses the existing `MembershipStatus` enum across
the application result, current-user result, HTTP response, and mapping
boundaries. Jackson continues to serialize the same stable enum names, so this
removes Java-side raw state strings without changing the external status values.

- [x] Add a failing convention test for enum-typed membership status boundaries.
- [x] Migrate Identity mappers and affected Assistant/Identity fixtures.
- [x] Run focused Identity tests, compilation, and Spotless checks.
- [ ] Continue finite-state migration in separate owning-module slices.

### Current slice — Subscription status enum boundary

Subscription status now uses the existing `SubscriptionStatus` enum across the
application result and HTTP response boundaries. Stable enum-name serialization
preserves the existing external values while keeping subscription lifecycle
state typed inside Java.

- [x] Add a failing convention test for enum-typed subscription status boundaries.
- [x] Migrate the subscription application mapper and public result/response.
- [x] Run focused Subscription tests and the affected fast quality gate.
- [ ] Continue finite-state migration in separate owning-module slices.

### Current slice — Clients customer status enum boundary

Customer status now uses the existing `CustomerStatus` enum across the
application result, HTTP response, and application mapping boundaries. Stable
enum-name serialization preserves the existing `ACTIVE`/`RETIRED` values while
keeping customer lifecycle state typed inside Java.

- [x] Add a failing convention test for enum-typed customer status boundaries.
- [x] Migrate the customer application mapper and public result/response.
- [x] Run focused Clients tests and the affected fast quality gate.
- [ ] Continue finite-state migration in separate owning-module slices.

### Current slice — Calendar status enum boundary

Calendar event-link and synchronization state now retain their existing domain
enums through application results and the HTTP synchronization response. Their
stable enum names remain unchanged at serialization boundaries.

- [x] Add a failing convention test for enum-typed Calendar status boundaries.
- [x] Migrate Calendar result/response models, mappers, and affected fixtures.
- [x] Run focused Calendar tests and the affected fast quality gate.
- [ ] Run live Calendar provider/replay checks when PostgreSQL and provider
      infrastructure are available.

### Current slice — Catalog status enum boundary

Catalog item status now uses the existing `CatalogItemStatus` enum through the
application result and HTTP response mappings. Stable enum-name serialization
preserves the existing catalog status values.

- [x] Add a failing convention test for enum-typed Catalog status boundaries.
- [x] Migrate Catalog result/response models and the application mapper.
- [x] Run focused Catalog tests and the affected fast quality gate.
- [ ] Continue finite-state migration in separate owning-module slices.

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

#### Current slice 6G — Remove the deprecated raw-string proactive route

The proactive semantic tool boundary now accepts only a prepared `SemanticQuery`.
ChatService prepares the query once when semantic shortcuts are configured and
passes that same value to the tool route. The deprecated raw-string route and
its embedding-owning constructor were removed after focused API, routing, and
ChatService tests were migrated. The semantic-cache raw-string overloads remain
as a separate compatibility family until their callers are migrated.

- [x] Add a failing API-boundary test for the prepared-only proactive route.
- [x] Remove the deprecated raw-string route and legacy constructor.
- [x] Migrate ChatService and proactive-route tests to the shared query.
- [x] Remove the remaining semantic-cache raw-string compatibility family.

#### Current slice 6H — Delete the deprecated embedding compatibility port

All Assistant production and test consumers now depend on the provider-neutral
`EmbeddingService`. The deprecated `EmbeddingModelPort` subtype and its source
file were removed; Spring composition roots and optional dependencies now use
the canonical service type directly.

- [x] Add a failing source-inventory test for the deprecated port deletion.
- [x] Migrate embedding selectors, adapters, routers, and configuration to
      `EmbeddingService`.
- [x] Migrate affected tests and delete `EmbeddingModelPort.java`.
- [x] Run the Assistant unit, integration-source, and Spotless gates.

#### Current slice 6I — Delete the duplicate library chat alias

The framework-neutral `com.emme.ai.contracts.model.ChatModel` was only an
inheritance alias for the deprecated composite provider and Assistant-local
chat port. It was removed after source and test caller searches confirmed that
Spring AI's `org.springframework.ai.chat.model.ChatModel` is a separate,
provider-internal framework type and that no application caller depended on
the library alias directly.

- [x] Add a failing source-inventory assertion for the library alias deletion.
- [x] Remove the alias inheritance from `AiModelProvider` and
      `ChatCompletionPort`.
- [x] Keep Spring AI's provider-internal `ChatModel` imports unchanged.
- [x] Run AI-contracts, AI-platform, and Assistant focused tests and compile.

#### Current slice 6J — Delete the duplicate library embedding alias

The framework-neutral `com.emme.ai.contracts.model.EmbeddingModel` was only
inherited by the deprecated composite `AiModelProvider`. Canonical embedding
callers already use `EmbeddingService`, and Spring AI's
`org.springframework.ai.embedding.EmbeddingModel` remains a separate
provider-internal transport type, so the unused library alias can be deleted
without changing embedding metadata or selection behavior.

- [x] Add a failing source-inventory assertion for the library alias deletion.
- [x] Remove the alias inheritance from `AiModelProvider`.
- [x] Keep Spring AI's provider-internal `EmbeddingModel` imports unchanged.
- [x] Run AI-contracts, AI-platform, and Assistant compilation and focused tests.

#### Current slice 6K — Route basic chat composition through the canonical capability

The basic Assistant chat composition no longer consumes the deprecated
composite `AiModelProvider`. Spring AI's default Ollama and Groq composition
roots now expose provider-identified `AiChatCompletion` adapters, while the
mock provider implements the same capability for tests and retains the
composite contract only for the still-pending embedding, image, and retrieval
compatibility callers. The canonical request preserves first-turn empty
conversation context and the existing mock empty-message HTTP behavior.

- [x] Add an architecture test proving basic chat composition does not import
      `AiModelProvider`.
- [x] Add the provider-identified Spring AI `AiChatCompletion` adapter and
      configuration beans.
- [x] Migrate `DefaultChatCompletionConfiguration` and its tests to the
      canonical chat request/response contract.
- [x] Preserve mock provider compatibility and provider admission/context
      checks.
- [x] Run AI-contracts, AI-platform, Assistant, integration-source, and
      Spotless verification gates.
- [ ] Migrate remaining composite embedding, image, and retrieval callers
      before deleting `SpringAiModelProvider`.

#### Current slice 6R — Add the canonical chat request to provider selection

`ChatModelSelector` implements the canonical `AiChatCompletion` contract.
Canonical requests validate the bound execution context, honor the ordered
admitted-provider policy, preserve the explicit fallback flag, and return
provider/model metadata.

- [x] Add failing selector tests for canonical provider admission and fallback.
- [x] Implement canonical request selection and provider/model response metadata.
- [x] Preserve the existing no-context behavior of the temporary string API.
- [x] Migrate all Assistant consumers and delete the temporary chat ports.

#### Current slice 6S — Route RAG answer policy through canonical chat

`RagAnswerPolicy` now depends directly on `AiChatCompletion` and sends an
explicit execution context plus provider admission/fallback policy with every
request. Spring RAG composition exposes the concrete selector as the
canonical capability and derives the admission policy from configured provider
keys; no default or hidden provider identity is introduced.

- [x] Add failing policy tests for canonical request and provider metadata.
- [x] Migrate grounded and advisor-backed RAG answer composition.
- [x] Expose canonical selector beans from both chat composition profiles.
- [x] Migrate `RagQueryService` to the canonical request boundary.
- [x] Migrate `ChatService` and tracing/provider adapters.

#### Current slice 6T — Migrate the retrieval-service completion fallback

`RagQueryService` now invokes `AiChatCompletion` with the bound execution
context and an explicit provider policy. When Spring chat properties are
available, the policy preserves the configured provider order and fallback;
otherwise it uses the configured platform provider. The service no longer
depends on the deprecated Assistant chat port.

- [x] Add the architecture assertion for the canonical service dependency.
- [x] Migrate the raw retrieval fallback to `AiChatCompletion.Request`.
- [x] Preserve configured multi-provider fallback when Spring chat is enabled.
- [x] Mark selector composition beans primary so raw provider capabilities do
      not create ambiguous canonical injections.
- [x] Run focused RAG service, composition, and web-context tests.
- [x] Migrate `ChatService` to canonical requests and explicit cache identity.
- [x] Migrate tracing/provider registry adapters and delete the remaining
      temporary chat adapters.

#### Current slice 6U — Migrate the chat service to canonical requests

`ChatService` now depends directly on `AiChatCompletion`. Each model execution
receives the bound `AiExecutionContext` and the configured provider policy, and
each semantic-cache write carries the canonical response provider/model plus
the existing knowledge, policy, and source versions. The service no longer
branches on a deprecated chat port or synthesizes `legacy-provider` /
`legacy-model` metadata. Tracing and provider-registry adapters were migrated
in the follow-up deletion slice below so the service boundary remained
independently reviewable.

- [x] Add failing source-architecture coverage for the deprecated chat service
      dependency and synthetic legacy response identity.
- [x] Migrate `ChatService` constructors and execution to `AiChatCompletion`.
- [x] Preserve semantic-cache writes with explicit response and policy
      metadata.
- [x] Run the full Assistant unit suite and focused canonical-chat tests.
- [x] Migrate tracing/provider-registry adapters and delete the temporary chat
      ports after all callers and tests are migrated.

#### Current slice 6V — Delete the temporary chat completion port family

All Assistant production adapters and tests now use `AiChatCompletion.Request`
and `ChatResponse`. The temporary Assistant `ChatCompletionPort`,
`IdentifiedChatCompletionPort`, and tracing adapter were deleted after a source
inventory proved that no caller, bean, or test still depended on them.

- [x] Add a failing source-inventory test for the temporary chat port files.
- [x] Migrate ChatService and RagQueryService test doubles to canonical requests.
- [x] Migrate selector, tracing, and provider-registry composition adapters.
- [x] Delete the temporary chat port family and legacy tracing test name.
- [x] Run focused tests, Assistant compilation, and source architecture checks.
- [ ] Run live provider/trace behavior gates when Docker-backed dependencies are
      available.

#### Current slice 6W — Name the basic chat composition by behavior

The non-enhanced Spring AI profile is the default basic provider composition
for the mock and single-provider platform paths. It is not a historical
compatibility port, so its configuration and bean names no longer use
`Legacy` terminology; the Spring AI-enabled profile remains mutually exclusive.

- [x] Add a failing architecture test for the behavior-based configuration name.
- [x] Rename the configuration and test to `DefaultChatCompletionConfiguration`.
- [x] Rename misleading chat completion composition methods containing `Port`.
- [x] Preserve default mock-provider, scheduler, tracing, and Spring AI profile
      selection behavior.
- [x] Run focused configuration and architecture tests.
- [ ] Run live application startup checks for both profiles when Docker-backed
      dependencies are available.

#### Current slice 6L — Keep document retrieval on the canonical embedding port

The Assistant document retrieval adapter now requires the provider-neutral
`EmbeddingService` directly. Its compatibility fallback through
`AiModelProvider` was removed after the canonical embedding composition was
already available; vector dimension validation, tenant context binding, and
ranked score propagation remain unchanged. The platform composite provider is
still retained for the remaining image and embedding adapters.

- [x] Add a failing architecture test for the retrieval adapter's composite
      provider dependency.
- [x] Inject `EmbeddingService` directly and remove the legacy vector fallback.
- [x] Update retrieval tests to use versioned canonical embedding vectors.
- [x] Run the full Assistant unit suite and integration-source compilation.
- [ ] Migrate the remaining platform embedding and image callers before
      deleting `SpringAiModelProvider`.

#### Current slice 6M — Remove the composite image capability adapter

The provider-neutral `CaptionImageUseCase` is now the direct image boundary.
Spring AI vision, deterministic mock captioning, and explicit unsupported Groq
behavior are composed as that capability, so the redundant
`AiCaptionImageAdapter` no longer injects `AiModelProvider`. Catalog callers
continue to depend only on `CaptionImageUseCase`.

- [x] Add a failing canonical capability test for Spring AI vision.
- [x] Expose mock and Spring AI vision implementations through
      `CaptionImageUseCase`.
- [x] Add explicit unsupported Groq composition and preserve provider behavior.
- [x] Delete the composite image adapter and update capability inventory tests.
- [x] Run AI Platform, Assistant, integration-source, and Spotless gates.
- [ ] Migrate the remaining platform embedding adapter before deleting
      `SpringAiModelProvider`.

#### Current slice 6N — Remove the composite embedding capability adapter

The provider-neutral `EmbeddingService` is now composed directly for the
default mock, Ollama, and unsupported Groq provider paths. The redundant
`AiEmbeddingAdapter` no longer wraps `AiModelProvider`; canonical versioned
vectors remain the input to Assistant retrieval and semantic consumers, and
the deprecated composite provider is retained only as an explicitly tracked
compatibility family pending final deletion.

- [x] Add a failing source-inventory test for the composite embedding adapter.
- [x] Add deterministic mock `EmbeddingService` coverage with model identity.
- [x] Compose Ollama and unsupported Groq embedding capabilities at the
      provider boundary.
- [x] Delete `AiEmbeddingAdapter` and update capability inventories.
- [x] Run AI Platform, Assistant, integration-source, and Spotless gates.
- [ ] Delete `SpringAiModelProvider` and `AiModelProvider` after the remaining
      compatibility bean/test inventory is removed.

#### Current slice 6O — Delete the verified composite provider family

All application capability callers now depend on canonical provider-neutral
ports. The deprecated `AiModelProvider`, `SpringAiModelProvider`, and mock
composite inheritance were removed; provider composition and transport tests
now exercise `AiChatCompletion`, `EmbeddingService`, and
`CaptionImageUseCase` independently.

- [x] Add a failing repository source-inventory test for the composite family.
- [x] Remove the deprecated contract, Spring adapter, composite configuration
      beans, and obsolete provider tests.
- [x] Remove composite methods from the mock provider while preserving
      canonical chat and caption behavior.
- [x] Migrate configuration and Groq transport tests to canonical capabilities.
- [x] Run contract, AI Platform, Assistant, integration-source, and Spotless
      verification gates.

#### Current slice 6P — Remove semantic-cache identity fallbacks

The semantic-cache port no longer provides constructors that silently assign a
legacy identity. All cache lookups and writes now carry explicit response,
knowledge, policy, and source identity metadata; unit and pgvector integration
fixtures were migrated to state that contract directly. Redis/vector and live
PostgreSQL behavior remain Docker-gated.

- [x] Add a failing source-inventory test for the legacy identity factory and
      overloaded cache constructors.
- [x] Migrate Assistant unit and pgvector integration fixtures to explicit
      `SemanticCacheIdentity` values.
- [x] Remove `SemanticCacheIdentity.legacy()` and the legacy `Lookup`/`Put`
      constructors.
- [x] Run the full Assistant unit suite, integration-test source compilation,
      and Spotless.
- [ ] Run live semantic-cache/vector behavior and PostgreSQL gates with Docker.

#### Current slice 6Q — Use structured semantic-cache invalidation

Semantic-cache invalidation now accepts only the existing structured
`SemanticCacheInvalidation` target. `SemanticChatCache` supplies the current
tenant and principal plus the manual dependency marker, so the durable adapter
does not reconstruct policy from a raw cache-kind string. Live Redis and
PostgreSQL invalidation behavior remain Docker-gated.

- [x] Add a failing source-contract test for the raw string invalidation
      overload.
- [x] Migrate `SemanticChatCache` and JDBC/test callers to structured targets.
- [x] Remove `SemanticCachePort.invalidate(String)` and the stale recording
      test-double override.
- [x] Run the full Assistant unit suite, integration-test source compilation,
      and Spotless.
- [ ] Run live semantic-cache invalidation and PostgreSQL gates with Docker.

#### Current slice 6B — Redis hot-projection hardening and construction simplification

Completed in this slice:

- `RedisSemanticCacheHotStore` now has one canonical production constructor accepting the provider-neutral `EmbeddingModelConfiguration`, clock, and optional Redis projection dependencies.
- Test-only construction variants were migrated to a local fixture helper, removing six ambiguous public entry points without changing the application port.
- Redis vector documents without a similarity score are ignored instead of being converted into a synthetic `0.0` candidate.
- Regression coverage confirms a Redis outage safely falls back to the durable PostgreSQL cache and that malformed hot documents fail closed.

The remaining Task 6 work is intentionally not collapsed into this slice: advisor-order assertions, complete metadata/model contract coverage, and measured `HybridSearch` alternatives still require separate evidence.

#### Current slice 6E — Centralize Redis semantic-cache metadata

The Redis semantic-cache vector index and hot-store adapter now share one
metadata contract. Field names and Redis index types are defined by
`RedisSemanticCacheMetadata`; the configuration consumes its vector-store
fields and the hot-store filters/writes consume the same constants. This
removes a drift risk without moving Spring AI types into the provider-neutral
semantic-cache port.

- [x] Add a failing metadata-contract test covering all cache projection keys.
- [x] Centralize metadata names and Redis field types in the Spring AI adapter boundary.
- [x] Use the shared contract for vector-store configuration and hot-store reads/writes.
- [x] Run focused Redis semantic-cache and configuration tests.
- [x] Measure the remaining `HybridSearch` alternative before replacing direct SQL;
      retain the PostgreSQL adapter because the Spring AI vector-store contract
      does not cover the required combined FTS/pgvector/RRF projection.

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
- [x] Measure a future Spring AI/vector-store alternative before considering
      replacement; retain direct SQL for the combined ranking contract.

#### Current slice 6F — Evaluate Spring AI PgVectorStore for hybrid search

The Spring AI PgVectorStore alternative was evaluated against the existing
`HybridSearch` contract. Spring AI provides portable vector similarity search,
top-k/threshold controls, and metadata filtering through `VectorStore`; it is
appropriate for a pure vector retrieval capability. The current repository
capability additionally requires one PostgreSQL statement that combines:

- weighted PostgreSQL full-text ranking over generated `tsvector` columns;
- pgvector cosine ranking;
- reciprocal-rank fusion with stable result IDs and scores;
- tenant-scoped projection and bounded hydration of domain records.

Replacing this with PgVectorStore would either drop lexical ranking or require
two provider calls plus a new application-side fusion implementation. That
would add latency, duplicate ranking mechanics, and make tenant/security
filtering less explicit. The decision is therefore to keep `HybridSearch` as
the provider-neutral port and `PostgresHybridSearch` as its specialized
adapter. Spring AI remains the preferred framework path for pure vector
retrieval and Redis hot projections. A runtime benchmark remains a future
optimization gate when Docker-backed PostgreSQL data is available; it is not a
precondition for this functional capability decision.

Reference: [Spring AI PGvector reference](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html)
and [Spring AI vector-store API](https://docs.spring.io/spring-ai/reference/api/vectordbs.html).

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

- [x] **Step 1: Write failing tests**

Cover advisor order, retrieved-document tenant filter, empty retrieval,
provider fallback, unsafe answer abstention, top-1 threshold and margin,
embedding model/version/dimension mismatch, Redis safe miss, durable cache
hit confirmation, and hybrid search ranking. These tests are covered by the
completed advisor, RAG, semantic policy, Redis projection, metadata, and
hybrid-search slices.

- [x] **Step 2: Run focused tests**

```bash
./gradlew :modules:assistant:test :modules:shared:test :database:test --tests '*Rag*' --tests '*Semantic*' --tests '*HybridSearch*' --no-parallel --no-configuration-cache
```

The focused matrix passed with the current provider-neutral ports and Spring AI
adapters.

- [x] **Step 3: Implement the minimum consolidation**

Build one advisor list per use case, inject the canonical router, simplify the
cache constructor to one production path, and keep policy-specific code only
where the framework cannot express tenant/security/abstention rules. The
advisor composition now delegates ordering to Spring's comparator and keeps
the security and prompt advisors ahead of retrieval. The remaining direct SQL
hybrid adapter is intentional and documented in slice 6F.

- [x] **Step 4: Run focused tests, integration contracts, and commit**

```bash
./gradlew :modules:assistant:test :modules:shared:test :database:test :modules:assistant:compileJava --no-parallel --no-configuration-cache
git add modules/assistant modules/shared
git commit -m "refactor(ai): consolidate Spring AI RAG and semantic paths"
```

Focused assistant/shared/database tests, integration-test source compilation,
and repository quality checks pass. Runtime pgvector/Redis integration remains
Docker-gated.

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

#### Current slice 8C — Bind checkpoints to the tenant JDBC client

The opt-in LangGraph composition root now qualifies its checkpoint persistence
dependency as `tenantJdbcClient`. The previous unqualified `JdbcClient` was
ambiguous once the AI job composition root exposed `coreJdbcClient`, and could
have selected the control-plane connection instead of the tenant-schema
connection. The LangGraph library types remain confined to the adapter and
composition-root edge; application workflow ports are unchanged.

- [x] Add a failing configuration test for the tenant JDBC qualifier.
- [x] Bind `jdbcCheckpointSaver` to `tenantJdbcClient`.
- [x] Run focused LangGraph tests and the application DDD/hexagonal architecture test.
- [ ] Complete live PostgreSQL checkpoint security/resume coverage with Docker.

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

#### Current slice 8D — Cross-tenant checkpoint rejection

The checkpoint saver now distinguishes a genuinely new workflow from an
existing workflow ID that is not accessible to the authenticated tenant,
conversation, or principal. The latter is rejected instead of being returned
as an empty checkpoint history. The live PostgreSQL proof remains Docker-gated.

- [x] Add focused regression coverage for an inaccessible existing workflow ID.
- [x] Reject cross-tenant workflow IDs before returning checkpoint history.
- [x] Run focused LangGraph/checkpoint tests, compilation, and Spotless.
- [ ] Run the live PostgreSQL checkpoint security/resume coverage when Docker is available.

#### Current slice 8E — Quote graph capability gating

The quote graph definition and compiled graph are now created only when the
quote capability is enabled. This keeps the opt-in LangGraph composition root
from creating an orphaned quote graph when `app.ai.quote.enabled` is false;
the existing quote resume adapter condition remains aligned with the same
property.

- [x] Add focused configuration coverage for quote capability gating.
- [x] Gate the quote graph and compiled graph beans with the quote property.
- [x] Run focused LangGraph configuration tests.
- [ ] Run live LangGraph startup and checkpoint coverage when Docker is available.

#### Current slice 8F — Quote resume checkpoint guard

Quote workflow resume now verifies that a persisted checkpoint exists before
calling `updateState` or resuming the graph. Missing checkpoints retain the
adapter's existing workflow-context error wrapper while exposing the explicit
missing-checkpoint cause; no state mutation is attempted.

- [x] Add focused missing-checkpoint resume coverage.
- [x] Reject quote resume without a checkpoint before state mutation.
- [x] Run the focused quote resume test suite.
- [ ] Run live LangGraph checkpoint/resume coverage when Docker is available.

#### Current slice 8G — LangGraph context composition coverage

Spring context coverage now proves that disabled LangGraph startup creates no
checkpoint or compiled-graph beans, while enabling conversation and quote
capabilities creates exactly one named compiled graph for each capability.
This locks the opt-in composition boundary without adding provider types to
application ports.

- [x] Verify disabled LangGraph startup.
- [x] Verify one named compiled graph per enabled capability.
- [x] Run the focused LangGraph configuration test suite.
- [ ] Run live startup and checkpoint coverage when Docker is available.

- [x] **Step 1: Write failing configuration/security tests**

Test disabled graph startup, one graph bean per capability, unauthorized
checkpoint read, cross-tenant workflow ID, duplicate checkpoint update,
malformed thread ID, resume without checkpoint, and authorized staff resume.

The non-Docker boundary is covered by focused configuration, checkpoint,
namespace, cross-tenant, duplicate-update, missing-checkpoint, and staff-resume
tests.

- [x] **Step 2: Run tests to verify the current behavior**

```bash
./gradlew :modules:assistant:test --tests '*LangGraph*' --tests '*Checkpoint*' --no-parallel --no-configuration-cache
```

- [x] **Step 3: Implement the minimum composition/name cleanup**

Keep `JdbcClient` and the tenant-aware decorator. Move generic library types
behind the adapter, remove duplicate resume adapter logic only when the tests
prove no separate policy is lost, and use capability-qualified bean names.

- [x] **Step 4: Run tests, compile, and commit**

```bash
./gradlew :modules:assistant:test :modules:assistant:compileJava --no-parallel --no-configuration-cache
git add modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfiguration.java modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfigurationTest.java modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/workflow
git commit -m "refactor(ai): simplify LangGraph workflow composition"
```

The non-Docker Task 8 security/configuration gate passed on 2026-09-06. Live
PostgreSQL checkpoint security/resume and startup evidence remain queued until
Docker is available.

#### Current slice 8H — Gate payment workflow persistence with LangGraph

Payment workflow checkpoint, execution-context, and tenant-routed payment-source
adapters are now created only when `app.ai.langgraph.enabled=true`. This keeps
ordinary Assistant/web contexts free of graph-only tenant JDBC dependencies while
preserving the enabled composition root. Focused opt-in tests and the full
Assistant unit suite pass; live PostgreSQL startup, ownership, and resume checks
remain Docker-gated.

- [x] Add focused opt-in configuration coverage for each graph-only adapter.
- [x] Apply the LangGraph property condition to all three adapters.
- [x] Run the full Assistant unit suite.
- [ ] Run live PostgreSQL checkpoint security/resume coverage with Docker.

#### Current slice 8I — Keep Assistant workflow composition on public APIs

The Assistant appointment/payment workflow composition now consumes public
appointments use cases rather than appointment repositories or domain objects.
Appointment-hold creation and retrieval are composed by the appointments module,
which preserves tenant-schema routing while keeping the Assistant boundary
provider- and persistence-neutral. Payment API leaf packages used by Assistant
are explicitly exposed through the existing `payment-api` named interface.

- [x] Add a failing appointments use-case and configuration composition test.
- [x] Expose `GetAppointmentHoldUseCase` and move hold configuration into the
      appointments module.
- [x] Replace Assistant appointment repository/domain imports with public use
      cases and API result records.
- [x] Mark all consumed Payment API leaf packages with `payment-api`.
- [x] Run the affected tests, integration-test compilation, architecture tests,
      Spotless, and the full repository `check`.
- [ ] Run the live PostgreSQL workflow startup/security/resume checks when
      Docker is available.

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

#### Current slice 13E — Preserve tenant provisioning idempotency

Tenant provisioning requests now return the existing control-plane tenant ID
when the requested slug is already registered. The adapter still avoids a
second registry insert, so repeated provisioning requests cannot create a new
logical tenant while the existing schema-per-tenant routing record remains the
authoritative identity.

- [x] Add a failing adapter test for an existing slug with a different request
      tenant ID.
- [x] Return the existing registry tenant ID for duplicate slugs.
- [x] Verify focused tenancy provisioning tests and compilation.
- [ ] Run duplicate/failure behavior against PostgreSQL/Testcontainers when
      Docker is available.

## 8. Phase F — External provider clients

The executable provider-HTTP plan is now maintained separately at
[`2026-09-05-external-provider-http-clients.md`](2026-09-05-external-provider-http-clients.md).
It supersedes the broad Task 14/15 outline below with HTTP-01 through HTTP-13:
provider-by-provider TDD slices, exact file ownership, MockRestServiceServer
contract tests, a small MockWebServer transport matrix, retained OkHttp E2E
boundaries, and wrapper/dependency deletion gates. Execute that focused plan
before treating Phase F as complete; the historical outline below is retained
only for continuity with earlier checkpoints.

### Historical Task 14: Establish the typed HTTP client convention (superseded)

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

### Historical Task 15: Replace zero-value HTTP wrappers with named gateways (superseded)

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

#### Current slice 16A — Prove foundational JPA list query counts

The Clients, Services, and Salon aggregates already use module-private Spring
Data repositories, provider-neutral application ports, and framework-free
domain mappings. This slice adds repository-level H2 evidence that list and
singleton reads execute one SQL statement after pending writes are flushed,
guarding against accidental N+1 behavior without changing schema-local tenant
routing.

- [x] Enable Hibernate statistics in the repository-test profile only.
- [x] Add customer create/find and one-query list coverage.
- [x] Add service create/find and one-query active-list coverage.
- [x] Add Salon singleton configuration and one-query operating-hours coverage.
- [x] Run the full Clients, Services, and Salon module tests, compilation, and
      Spotless checks.
- [x] Commit and push `4c02eb92`, `1ad62cb6`, and `783c6d73`.
- [ ] Add/execute live PostgreSQL tenant-routing and optimistic-lock conflict
      evidence when Docker is available.

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

The focused collision/repository tests and `:modules:appointments:compileJava`
passed on 2026-09-06. The remaining Task 17 evidence is the live PostgreSQL
exclusion-constraint race against the deployed migration path.

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
- [x] Replace the tenant-qualified active-action method with a schema-local
      status query ordered by creation time and ID.
- [x] Run Assistant tests, compilation, Checkstyle, and Spotless.

#### Current slice 18AC — Order Assistant pending actions deterministically

Active pending actions are already schema-local, but the previous Spring Data
method did not specify an order. Database row order is not a contract, so a
conversation with multiple pending actions could produce an unstable API order
across plans, indexes, or PostgreSQL versions. The adapter now delegates to a
derived JPA query ordered by `created_at` and the inherited UUID `id`, preserving
the provider-neutral application port while making the read deterministic.

- [x] Add a failing adapter test for the ordered repository method.
- [x] Add the Spring Data `createdAt`/`id` ordering contract.
- [x] Preserve the existing application port and status filter.
- [x] Run the focused Assistant test.
- [ ] Add a measured composite index if production query plans show the sort is
      material; do not add speculative indexing in this slice.

#### Current slice 18AD — Persist Assistant JPA updates through managed entities

Assistant `Conversation` and `PendingAction` updates previously reconstructed
entities and passed them to `save`. Because their domain models did not carry
the inherited JPA version, this could classify an existing row as new and
undermine optimistic locking. New aggregates now use null IDs so JPA assigns
identity on persist; existing updates load by ID, mutate the managed entity,
and save it. Domain and application ports remain framework-free.

- [x] Add failing adapter tests for existing conversation and pending-action
      updates.
- [x] Use managed-entity update paths with inherited `@Version` support.
- [x] Preserve immutable aggregate fields during updates.
- [x] Run focused Assistant tests and the full Assistant check.
- [ ] Run the live PostgreSQL update/conflict test when Docker is available.

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

- [x] **Step 2: Run each module group separately**

```bash
./gradlew :modules:tenancy:test :modules:identity:test :modules:subscriptions:test :modules:documents:test :modules:catalog:test :modules:calendar:test :modules:notification:test :modules:payment:test --tests '*RepositoryTest' --tests '*PersistenceAdapterTest' --no-parallel --no-configuration-cache
```

The deterministic repository/adapter matrix passed for tenancy, identity,
subscriptions, documents, catalog, calendar, notification, and payment. The
remaining live tenant-routing and PostgreSQL concurrency evidence still
requires Docker.

- [x] **Step 3: Convert only the proven JPA candidates**

Use projections and locking before custom SQL. Keep specialized vector,
generated-FTS, webhook, and atomic state-transition SQL in named adapters when
the ledger says JPA is less clear or unsafe.

- [x] **Step 4: Run tests, architecture checks, compile, and commit in two groups**

```bash
./gradlew :modules:tenancy:test :modules:identity:test :modules:subscriptions:test :modules:documents:test :modules:catalog:test :modules:calendar:test :modules:notification:test :modules:payment:test --no-parallel --no-configuration-cache
git add modules/tenancy modules/identity modules/subscriptions modules/documents modules/catalog modules/calendar modules/notification modules/payment
git commit -m "refactor(domain): standardize remaining JPA persistence"
```

#### Current slice 18AG — Close the tenant-qualified persistence audit

The remaining tenant-qualified persistence methods were reviewed operation by
operation after the schema-local JPA conversions. No additional removal is
safe: the remaining methods are either control-plane or authorization state,
provider/business-key lookups, callbacks and idempotency claims, cross-tenant
jobs, or specialized vector/full-text/JSONB/atomic stores. These boundaries
must keep explicit tenant identity even when ordinary tenant-schema CRUD uses
the selected schema as its isolation boundary.

- [x] Inventory all remaining tenant-qualified repository and persistence
      adapter methods.
- [x] Confirm schema-local CRUD/list candidates are already converted to
      ID-only or schema-local JPA operations.
- [x] Confirm explicit tenant predicates remain for membership, provider keys,
      callbacks, claims, cross-tenant jobs, projections, and specialized SQL.
- [x] Run the deterministic repository/adapter matrix for the remaining entity
      modules.
- [x] Record that no further mechanical tenant-ID removal is justified.
- [ ] Run live tenant-routing, PostgreSQL RLS, and optimistic-lock gates when
      Docker is available.

#### Current slice 25D — Rename the embedding tracing decorator

`TracingEmbeddingModelPort` was a concrete decorator around the canonical
`EmbeddingService`; it was not an application port and retained a misleading
compatibility-era name. The implementation is now `TracingEmbeddingService`.
The provider registry, focused tracing tests, and supporting specifications use
the behavior-based name; tracing and failover behavior are unchanged.

- [x] Add a failing source-contract test for the old decorator name.
- [x] Rename the production decorator and its focused test.
- [x] Migrate registry, specification, and plan references.
- [x] Run focused contract/tracing tests and Assistant compilation.
- [ ] Run live provider tracing gates when Docker/provider infrastructure is
      available.

## 10. Phase H — Events, Redis, libraries, and build foundations

### Task 19: Standardize Modulith events and Kafka boundaries

#### Current slice 19F — Make tenant activation duplicate-safe

`TenantActivationListener` now treats an activation event for an already
`ACTIVE` tenant as a no-op. It does not rewrite the registry or publish a
second `TenantActivated` event, while `PROVISIONING` and `FAILED` tenants
continue through the normal activation path. This is in-process
duplicate-delivery policy; durable Modulith/Kafka replay evidence remains
runtime-gated.

- [x] Add a failing listener test for an already active tenant.
- [x] Skip duplicate activation before registry mutation and downstream event
      publication.
- [x] Run focused Tenancy listener tests, Spotless, Checkstyle, and Modulith
      architecture validation.
- [ ] Run live duplicate-delivery/publication-retry checks when PostgreSQL and
      Kafka are available.

#### Current slice 19D — Give durable Modulith listeners stable identities

Provisioning and calendar listeners now declare explicit Modulith listener IDs,
matching the existing Assistant payment, WhatsApp, and semantic-cache listener
contracts. Stable IDs make event-publication records and retry diagnostics
independent of generated method identity; this slice does not add a second
delivery mechanism or alter tenant/business behavior.

- [x] Add a failing application source-contract test for the durable listener
      identity set.
- [x] Add explicit IDs to calendar, tenancy, identity, and subscription
      listeners.
- [x] Run the application contract test and affected module tests.
- [x] Run affected-module Spotless checks.
- [ ] Run live duplicate-delivery and event-publication retry gates when Kafka,
      PostgreSQL, and the deployment runtime are available.

#### Current slice 19E — Restore database routing for Calendar replay

`CalendarSyncRequested` now carries the tenant's database routing identity.
The Calendar inbound listener resolves that identity through the public Tenancy
database-routing use case before publishing the internal sync request, and the
Google adapter restores both tenant and database context before tenant-schema
access. The Calendar module does not import the Tenancy module's internal
repository port.
This prevents an externalized appointment replay from silently selecting the
default database when the tenant is routed to a dedicated database.

- [x] Add a failing Calendar listener test for database routing propagation.
- [x] Add the database ID to the internal Calendar sync event.
- [x] Resolve the database ID through the public Tenancy API boundary.
- [x] Restore tenant and database context in the Calendar provider adapter.
- [x] Run focused Calendar listener/provider tests and module compilation.
- [ ] Run live database-per-tenant replay and provider synchronization checks
      when PostgreSQL and external provider infrastructure are available.

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

- [x] **Step 2: Run focused event tests**

```bash
./gradlew :modules:assistant:test :modules:appointments:test :modules:identity:test :modules:tenancy:test :modules:subscriptions:test --tests '*Event*' --tests '*Listener*' --no-parallel --no-configuration-cache
```

The focused event/listener suites passed and the corresponding integration-test
source sets compile. Live Kafka replay/duplicate-delivery evidence remains
environment-gated by Docker.

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

- [x] **Step 2: Run focused Redis tests**

```bash
./gradlew :modules:assistant:test :modules:identity:test --tests '*Redis*' --tests '*Semantic*' --no-parallel --no-configuration-cache
```

The focused Redis/semantic suites pass. The identity Redis limiter now has a
fail-closed contract for `RedisConnectionFailureException`; live outage,
eviction, and vector metadata behavior remain Docker-gated.

- [x] **Step 3: Implement managed wiring and narrow native access**

Replace direct client construction with Spring-managed connection factories
where behavior is equivalent. Keep custom cache policy/admission and direct
atomic operations only in named adapters.

- [x] **Step 4: Run tests and commit**

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

#### Current slice 20B — Fail closed when the distributed login limiter is unavailable

The Redis-backed login-attempt limiter now returns `false` when Spring Data
Redis reports a connection failure. This keeps the security boundary fail
closed during an outage while preserving the existing process-local fallback
for deployments that do not configure Redis. The provider-neutral
`LoginAttemptRateLimiter` port and atomic Lua counter remain unchanged.

- [x] Add a failing Redis outage test.
- [x] Return a rejected decision for `RedisConnectionFailureException`.
- [x] Run the identity Redis test and the assistant Redis/semantic matrix.
- [ ] Run live Redis outage and recovery behavior with Docker.

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

#### Current slice 21F — Guard generic fixture dependencies

The generic testing library now has an executable architecture test that scans
all test-fixture Java sources and its build file for identity, salon,
subscription, tenancy, Keycloak, Google client, and feature-module references.
The existing tenancy-owned fixtures remain the owners of tenant provisioning
and entitlement setup.

- [x] Add `TestingFixtureDependencyTest` before changing fixture ownership.
- [x] Verify generic fixture sources contain no feature package or provider
      client references.
- [x] Verify the generic testing build has no feature-module dependencies.
- [x] Run the testing-library tests, fixture compilation, and Spotless.
- [x] Commit and push `16cf9a48`.
- [ ] Run all fixture-consuming module tests after any further fixture moves.

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
- [x] Split the remaining identity, salon, and subscription setup from
      `BaseTenantModuleTest` through a tenancy-owned entitlement fixture.

#### Current slice 21E — Split tenant entitlement setup from the generic fixture

`BaseTenantModuleTest` no longer autowires the unused
`SpringDataBusinessProfileRepository`, `SpringDataMembershipRepository`, or
`SpringDataRoleRepository`. Subscription and feature-flag setup now lives in
the tenancy-owned `EntitledTenantModuleTest`, and Identity tests own their role
and membership repositories. The generic base therefore contains only tenant
creation, request, and JWT mechanics.

- [x] Add a failing fixture-boundary test for the unused salon repository.
- [x] Remove the unused salon and membership repository fields from `BaseTenantModuleTest`.
- [x] Move the Identity-only role repository injection to `IdentityModuleTest`.
- [x] Remove the now-unneeded direct Salon dependency from the tenancy fixture artifact.
- [x] Add `EntitledTenantModuleTest` for subscription and feature-flag setup.
- [x] Migrate all `fullSetup()` consumers to the entitled fixture.
- [x] Run the tenancy boundary, representative full-context, and affected fixture compilation checks.

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

- [x] **Step 1: Write failing convention/dependency tests**

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
plugin exposes per-project `computeActualUsage*` and `computeAdvice` tasks. The
plugin was upgraded from `2.14.0` to `3.18.0`, and dependency verification
metadata was regenerated for the new artifact graph. The representative
assistant, booking, and catalog analysis tasks now complete on the Java 25
toolchain, including bytecode analysis and advice generation.

The generated advice contains configuration recommendations and intentional
framework-owned/transitive entries. It is evidence for follow-up slices, not a
license for bulk removal. Existing source inventory tests remain the guard for
duplicate declarations, and each future dependency change must be validated
against source usage and the affected test classpath.

- [x] **Step 3: Remove duplicate declarations and split conventions only where measured**

Remove repeated lines and redundant plugins first. Split persistence/testing
conventions only when the source/dependency report shows a real capability
overprovisioning benefit that outweighs new convention names. The measured
cleanup removed duplicate dependency notation, redundant convention
applications, and eight remaining unit-test fixture redeclarations; integration
test fixture declarations remain explicit because they belong to a separate
source set.

- [x] **Step 4: Run compile and build checks, then commit**

```bash
./gradlew check --no-daemon --no-parallel --no-configuration-cache
./gradlew :modules:assistant:computeAdvice :modules:booking:computeAdvice :modules:catalog:computeAdvice \
  --no-daemon --no-parallel --no-configuration-cache
```

The repository check and representative dependency-analysis tasks pass. The
analysis output remains a review artifact: framework-owned and transitive
recommendations are not bulk-applied. The assistant duplicate-class warning was
resolved by excluding the legacy non-Jakarta Swagger annotation artifact from
the Spring AI OpenAI dependency.

#### Current slice 22A — Remove duplicate module dependency declarations

The known duplicate declarations were limited to redundant Gradle notation:
`modules/booking` declared `libraries:kernel` six times, `modules/catalog`
declared it three times, and `modules/assistant` declared the Spring Security
test dependency twice. A repository inventory test now guards one declaration
per dependency, and only the redundant lines were removed. The convention
ownership cleanup is covered by the follow-up 22B slice; deeper convention
splits remain gated on dependency-analysis evidence.

- [x] Add a failing repository dependency-duplication test.
- [x] Remove redundant `libraries:kernel` declarations from booking and catalog.
- [x] Remove the duplicate Spring Security test dependency from assistant.
- [x] Run the repository inventory test, affected compilation, and Spotless.
- [x] Record the actual per-project dependency-analysis task names and Java 25
      class-file compatibility blocker.
- [x] Remove redundant shared test-fixture declarations from modules already
      applying the `emme.testing` convention.
- [x] Extend the repository inventory guard to prevent that convention
      duplication from returning.
- [x] Upgrade dependency analysis to `3.18.0`, regenerate verification metadata,
      and run representative advice tasks on the configured Java toolchain.
- [x] Review the assistant duplicate-class advice and remove the proven
      conflicting legacy Swagger annotation artifact.
- [x] Review remaining generated advice module by module before changing any
      additional dependency or convention declaration. The module-only pass
      completed for all database, library, business-module, and tooling
      projects; recommendations are predominantly convention-owned framework
      transitives or test/integration scope suggestions. No additional safe
      removal was justified. The application integration source set remains a
      tooling exception because its disabled application JAR is requested by
      the analysis plugin.

#### Current slice 22B — Let the testing convention own shared fixtures

The `emme.testing` convention already contributes the generic testing fixture
to a module's test suite. Nine modules repeated the same dependency in their
build scripts, adding maintenance noise without adding a different capability.
Those declarations were removed, while integration-test fixture declarations
remain explicit because they belong to a separate source set. The repository
inventory test now enforces this ownership rule.

The same rule was then applied to the remaining Spring/Java-library modules:
assistant, calendar, catalog, identity, notification, payment, shared, and
tenancy. Their unit-test source sets now receive the generic fixture solely
through `emme.testing`; integration-test fixture dependencies remain explicit.

- [x] Add a failing repository test for convention-owned shared fixtures.
- [x] Remove the nine redundant test-suite fixture declarations.
- [x] Compile every affected module's test sources and run the inventory test.
- [x] Remove explicit `emme.testing` applications from seven modules already
      covered by `emme.spring-module` → `emme.java-library`.
- [x] Extend the repository inventory guard to prevent duplicate convention
      application from returning.
- [x] Remove the duplicate `emme.modulith` plugin and shared test fixture from
      the application already covered by `emme.spring-application`.
- [x] Remove the empty `emme.test-fixtures` convention from subscriptions;
      Identity's fixture remains because tenancy consumes it.
- [x] Remove redundant `spring-boot-starter-test` declarations from
      `ai-platform` and `shared`, which inherit it from `emme.testing`.
- [x] Remove the unused `modules:shared` dependency from the generic testing
      library's main and fixture configurations.
- [x] Revisit convention-plugin scope after Java 25-compatible dependency
      analysis is available. Representative advice was reviewed module by
      module; no additional safe scope reduction was justified.

#### Current slice 22D — Complete Java 25 dependency-analysis review

The build-logic suite and representative Assistant, Booking, and Catalog
dependency-analysis tasks now pass on the configured Java 25 toolchain. Advice
was reviewed as evidence; framework-owned transitives and source-set-specific
test dependencies were retained, and no speculative convention split or bulk
dependency removal was applied.

- [x] Run `:build-logic:test`.
- [x] Run representative `computeActualUsageMain` and `computeAdvice` tasks.
- [x] Review remaining advice against source usage and test classpaths.
- [x] Commit/push the prior measured dependency cleanup; no new build change
      was justified by this review.

#### Current slice 22C — Remove the proven Swagger duplicate

Dependency insight showed that Spring AI's OpenAI client brought
`io.swagger.core.v3:swagger-annotations:2.2.31`, while Springdoc brought the
Jakarta artifact `swagger-annotations-jakarta:2.2.38`. Both publish overlapping
annotation classes, so the runtime classpath emitted duplicate-class advice.
The scoped exclusion is attached only to `spring-ai-openai`; Springdoc's
Jakarta artifact remains the single runtime provider. AI Platform and Assistant
tests, compilation, dependency insight, and dependency advice pass after the
change.

- [x] Add a failing repository guard for the scoped exclusion.
- [x] Add the exclusion to the Spring AI OpenAI dependency.
- [x] Verify only `swagger-annotations-jakarta` remains on Assistant runtime.
- [x] Run AI Platform/Assistant tests and dependency-analysis tasks.

The same analysis also identified the aggregate `spring-boot-starter` as
unused in `ai-platform`; that library consumes the specific Spring Boot APIs
provided by its existing framework dependencies and the deployable application
owns the aggregate starter. The direct aggregate dependency was removed and
AI Platform/Assistant compilation and tests remained green.

- [x] Add a failing repository guard for the unused aggregate starter.
- [x] Remove the aggregate starter from `modules/ai-platform`.
- [x] Re-run AI Platform/Assistant compilation, tests, and dependency advice.

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

- [x] **Step 1: Write failing migration contract tests**

Test table ownership metadata, RLS enabled/policies, unique idempotency keys,
version columns, vector dimensions/indexes, generated `tsvector`, and tenant
schema provisioning state. Test invalid tenant slug/schema input in the shell
script with a disposable PostgreSQL environment.

The existing per-capability migration contracts and the new
`MigrationCatalogContractTest` cover the checked-in Liquibase catalog and
required AI/PostgreSQL invariants. Invalid slug/schema execution and live
catalog/RLS behavior remain Docker-gated.

- [x] **Step 2: Run the database contract suite**

```bash
./gradlew :database:test --tests '*MigrationContractTest' --no-parallel --no-configuration-cache
```

The existing migration-contract suite passes. It covers the AI job, semantic,
quote, learning, AGE, design-image, appointment-collision, and event
idempotency migrations. Live Liquibase application and PostgreSQL catalog/RLS
verification remain Docker-gated; no deployed migration was edited in this
slice.

- [x] **Step 3: Add only required forward migrations and script fixes**

Use Liquibase for schema changes and keep SQL-specific behavior in the
database. Align runtime and script validation through tests rather than copying
unsafe SQL interpolation into application code.

The 2026-09-06 script slice tightened seed-slug validation to the PostgreSQL
identifier-safe boundary (`^[A-Za-z][A-Za-z0-9-]{0,62}$`) before registry writes.
No deployed migration was edited; the remaining live Liquibase/catalog/RLS gate
is still Docker-dependent.

- [x] **Step 4: Run migration tests and commit**

```bash
./gradlew :database:test :database:compileJava --no-parallel --no-configuration-cache
git add database
git commit -m "test(database): enforce framework-first persistence contracts"
```

#### Current slice 23A — Verify tenant migration script contracts

The forward migration/script work is complete for the available non-Docker
verification lane. Seed slugs are validated before registry writes, checked-in
migration contracts pass, and shell syntax is valid. No deployed migration was
edited; PostgreSQL execution remains a runtime gate.

- [x] Run `bash -n database/docker/run-migrations.sh`.
- [x] Run the full `:database:test` contract suite and `:database:compileJava`.
- [x] Keep live Liquibase, catalog, and RLS verification Docker-gated.

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

- [x] **Step 1: Write failing configuration checks**

Validate every referenced environment variable, health URL, compose profile,
Kubernetes probe, migration job dependency, and CI Gradle task. Assert the
final quality command includes compile, tests, Spotless, Checkstyle, coverage,
security/dependency analysis, and architecture tests.

- [x] **Step 2: Run configuration validation**

```bash
./gradlew tasks --no-daemon --no-parallel --no-configuration-cache
node scripts/validate-backend-workflow.mjs
node scripts/validate-container-workflow.mjs
node deployment/compose/compose.age.contract.test.mjs
node deployment/compose/compose.kafka.contract.test.mjs
node deployment/compose/compose.e2e.contract.test.mjs
```

The repository does not expose an `appConfig` Gradle task. The available
workflow and Compose contract checks pass; Kubernetes smoke execution remains
environment-gated.

- [x] **Step 3: Simplify duplicate operational wiring**

Keep environment-specific overrides explicit. Remove only duplicate profiles,
stale files, and repeated CI task definitions proven by the configuration tests.

The stale E2E Compose backup was removed after the active overlay passed its
contract test and was verified to contain the current migration, Keycloak,
Redis, and health-check wiring.

#### Current slice 24A — Add executable deployment contract validation

The backend workflow now invokes a deployment contract validator during the
quality job. The validator protects Kubernetes liveness/readiness probes,
non-root execution, migration-job secret wiring, and the workflow invocation
itself. Existing backend and container workflow validators remain separate.

- [x] Add the failing deployment-contract test before wiring the CI step.
- [x] Add `scripts/validate-deployment-contracts.mjs`.
- [x] Invoke it from `.github/workflows/ci-backend.yml`.
- [x] Run backend workflow, container workflow, and deployment-contract checks.
- [x] Commit and push `9f33de38`.
- [x] Render all Kubernetes overlays and validate JVM/native Compose configs.
- [ ] Run live Kubernetes and Compose smoke checks when their runtime
      environment is available.
- [x] Run the full repository `./gradlew check` phase gate; 258 actionable tasks
      passed after the compatibility inventory formatting correction.

- [ ] **Step 4: Run smoke checks and commit**

```bash
./gradlew check --no-parallel --no-configuration-cache
git add .github deployment infra gradle
git commit -m "chore(ops): standardize repository verification gates"
```

### Task 25: Remove verified compatibility classes and dependencies

**Files:**

- Delete: duplicate AI contracts and provider wrappers listed in the migration ledger
- Delete: `modules/payment/.../PaymentHttpClient.java`, `modules/notification/.../NotificationHttpClient.java`, and `modules/calendar/.../GoogleHttpClient.java` only after focused-plan task `HTTP-13`; the Assistant `AiHttpClient` deletion is already complete
- Delete: `modules/ai-platform/.../SpringAiModelProvider.java` only after Task 4
- Modify: `gradle/libs.versions.toml`, `platform/build.gradle.kts`, and owning build files
- Modify: package-info and architecture tests with stale module names
- Test: all affected module tests and architecture/dependency checks

**Acceptance criteria:**

- `rg` finds no caller, bean, import, test, or dependency for each deleted class.
- Replacement integration tests pass for every deleted capability.
- No serialized event/workflow contract changes occur without compatibility coverage.
- Unused provider/JDBC/HTTP dependencies are removed from the narrowest owning scope.

- [x] **Step 1: Write failing deletion-candidate tests**

Add a repository inventory test that fails when a ledger item marked ready for
deletion still has a source caller, bean declaration, import, or build
dependency. `CompatibilityDeletionInventoryTest` now reads explicit
`Pending`/`Ready`/`Deleted` ledger rows, verifies path/status consistency, and
checks qualified references before a ready or deleted item can pass.

- [x] **Step 2: Run caller and dependency searches**

```bash
rg -n 'SpringAiModelProvider|PaymentHttpClient|NotificationHttpClient|GoogleHttpClient|AiHttpClient|JdbcTemplate|NamedParameterJdbcTemplate' modules libraries applications tools
./gradlew :modules:assistant:computeActualUsageMain :modules:assistant:computeAdvice \
  :modules:ai-platform:computeActualUsageMain :modules:ai-platform:computeAdvice \
  :modules:booking:computeActualUsageMain :modules:booking:computeAdvice \
  :modules:catalog:computeActualUsageMain :modules:catalog:computeAdvice \
  --no-daemon --no-parallel --no-configuration-cache
```

The repository-wide caller search is complete. The module-only advice pass
completed for all non-application projects; the application-specific analysis
remains blocked by its `integrationTest` task requesting the disabled
`emme-platform-0.1.0.jar`.

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

#### Current slice 25A — Make compatibility deletion status executable

The deletion checklist is now backed by `CompatibilityDeletionInventoryTest`.
The migration ledger records the remaining provider wrappers and the composite
Spring AI model provider as `Pending`; the test will reject a future `Ready`
or `Deleted` status while its qualified implementation reference remains.
The test also distinguishes a deleted tenancy implementation from an Identity
replacement that happens to share the same simple class name.

- [x] Add the failing inventory test before adding ledger status rows.
- [x] Add explicit compatibility candidate status rows to the migration ledger.
- [x] Verify pending paths and deleted paths against the repository.
- [x] Verify ready/deleted candidates have no qualified source, test, bean, or
      build references.
- [ ] Delete a compatibility family; this remains gated by the pending caller
      migrations and the explicitly deferred provider HTTP session.

#### Current slice 25B — Delete unused routing compatibility contracts

The deprecated `ai-contracts` intent-routing records, routing port, and
`ToolRisk` enum had no production, bean, test, or build callers outside their
own contract test. Assistant-owned semantic routing and `AiToolRisk` remain the
active boundaries. A source-inventory assertion now protects the deletion.

- [x] Add a failing source-inventory test for the routing and tool compatibility
  family.
- [x] Verify no production, test, bean, reflection, or build caller remains.
- [x] Delete the routing records, routing port, package metadata, and
  `ToolRisk` compatibility enum.
- [x] Run all `ai-contracts` tests, Assistant compilation, and Spotless.
- [x] Commit and push `8b603cbf`.
- [ ] Run any live provider behavior gates when Docker is available.

#### Current slice 25C — Remove unused controller construction shortcut

The Assistant web controller’s four-argument constructor existed only for a
unit test that exercised the non-durable chat endpoint. Spring uses the
durable-conversation composition constructor, so the shortcut was removed
without changing `/api/ai/chat` behavior.

- [x] Add a failing reflection test proving only the durable composition
  constructors remain.
- [x] Migrate the isolated controller test to the durable constructor.
- [x] Remove the unused four-argument compatibility constructor.
- [x] Run the full Assistant unit suite, integration-test source compilation,
  and Spotless.
- [x] Commit and push `3a67ee68`.
- [ ] Run live web/application startup gates when Docker is available.

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

#### Current slice 18Z — Make Calendar event-link lookup schema-local and provider-aware

The single-link Calendar application port no longer accepts a redundant tenant
identifier. Tenant schema routing is the isolation boundary, while provider is
retained in the query because an appointment may have more than one external
calendar link as providers are added. `findByAppointmentIdAndProvider` avoids
the previous `Optional` ambiguity without changing the database schema or
removing the existing list query used for multi-provider operations.

- [x] Add a failing adapter test for appointment/provider lookup.
- [x] Replace the tenant-qualified Spring Data query with the provider-aware,
      schema-local query.
- [x] Update the Calendar application port, use cases, and Google adapters.
- [x] Run focused Calendar tests and `:modules:calendar:check`.
- [x] Add a PostgreSQL uniqueness/cardinality migration for the singular
      appointment/provider lookup; the constraint still permits one link for
      each future provider.

#### Current slice 18AA — Make Calendar sync-state lookup schema-local

Calendar synchronization state is stored in the tenant schema and the tenant
connection is selected before the JPA repository is invoked. Its lookup now
uses the provider-only Spring Data method; tenant identity remains in the
domain/entity for context, creation, response mapping, and RLS, while the
application use case still receives the current tenant ID at the boundary.

- [x] Add failing repository and adapter coverage for provider-only lookup.
- [x] Replace `findByTenantIdAndProvider` with `findByProvider` in the JPA
      repository and application port.
- [x] Update the Calendar adapter and sync service without changing the
      provider-neutral API response or tenant context boundary.
- [x] Run focused Calendar persistence tests and the full Calendar check.
- [ ] Continue the operation-by-operation review for remaining tenant-schema
      methods whose tenant ID is a business key or shared/control-plane key.

#### Current slice 18AB — Enforce Calendar event-link cardinality

The application port intentionally returns one link for an appointment and
provider. The original schema only constrained external event IDs, so duplicate
rows could make the JPA `Optional` query fail at runtime with a non-unique
result. A forward migration now preflights existing duplicates and adds a
tenant/provider/appointment uniqueness constraint. The tenant column remains in
the database key for compatibility with the existing RLS and shared-database
deployment mode; tenant schema routing remains the application isolation
boundary.

- [x] Add a failing migration contract for the forward cardinality change.
- [x] Add duplicate-data preflight and the unique constraint migration.
- [x] Include the migration in the studio Liquibase changelog.
- [x] Run migration catalog and focused database contract tests.
- [ ] Run the migration against PostgreSQL/Testcontainers and verify existing
      deployment data has no duplicate appointment/provider links.

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
| E | Provider HTTP focused plan (`HTTP-01`–`HTTP-13`) | Provider contract tests, transport matrix, dependency cleanup, commit SHAs |
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

- [x] AI contracts have one canonical capability per operation.
- [x] `ai-contracts` has no framework/provider imports.
- [x] Spring AI chat/tools/RAG construction has one production path.
- [x] Focused assistant, ai-platform, shared, and database tests pass.
- [x] Affected compilation and architecture tests pass.

```bash
./gradlew :libraries:ai-contracts:test :modules:ai-platform:test :modules:assistant:test :modules:shared:test :database:test --no-parallel --no-configuration-cache
```

### Checkpoint B — after Tasks 7–13

- [x] LangGraph is limited to proven resumable complexity.
- [x] AI JDBC stores are classified and stable CRUD has a tested JPA path.
- [x] Remaining `JdbcClient` code is named and justified.
- [x] Tenant bootstrap JDBC is isolated from application policy.
- [ ] Tenant provisioning duplicate/failure behavior is integration-tested.

```bash
./gradlew :modules:assistant:test :modules:ai-platform:test :modules:tenancy:test :modules:subscriptions:test :modules:shared:test --no-parallel --no-configuration-cache
```

### Checkpoint C — after Phase F and Tasks 20–22

- [ ] Provider gateways use typed Spring HTTP clients or justified official SDKs.
- [ ] Redis and Modulith behavior is safe under outage/retry/duplicate delivery.
- [x] Generic test fixtures no longer depend on feature modules.
- [x] Duplicate Gradle declarations and unused placeholder capabilities are removed.

```bash
./gradlew test compileJava \
  :modules:assistant:computeAdvice :modules:booking:computeAdvice \
  :modules:catalog:computeAdvice :modules:ai-platform:computeAdvice \
  --no-daemon --no-parallel --no-configuration-cache
```

There is no root `dependencyAnalysis` task in this repository. Dependency
analysis is exposed per project through `computeActualUsage*` and
`computeAdvice`; representative Java 25-compatible advice tasks pass. The
provider HTTP and Redis/Modulith outage gates remain open pending their
dedicated migrations and Docker-backed tests.

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
- Keycloak and Google provider SDK adoption is explicitly deferred to the focused provider HTTP plan; Spring `RestClient` remains the default unless a separate protocol/auth/error comparison approves an SDK.
- Appointment exclusion/range constraints require PostgreSQL concurrency evidence in Task 17; a JPA existence query is the default first attempt.
- The current platform patch upgrade to the latest compatible stable version is a separate maintenance change and is not part of the first refactoring commits.

## Current slice 18AE — Preserve versioned Notification and Payment updates

Notification and Payment are tenant-owned JPA aggregates with inherited
optimistic versioning. Their previous adapters rebuilt an entity from the domain
object for every save, which discarded the persistence version and could make a
Spring Data update look like a new insert. New aggregates now leave identity
generation to JPA; existing aggregates are loaded by ID and updated on the
managed entity. The application ports remain unchanged and provider-neutral.

- [x] Add failing adapter regressions for existing Notification and Payment
      updates.
- [x] Make new domain aggregate IDs null until JPA persists them.
- [x] Add mapper `updateEntity` methods and conditionally restore identity only
      for existing mapped aggregates.
- [x] Load existing entities by ID before saving and preserve their managed
      JPA/version state.
- [x] Run focused tests and `:modules:notification:check` plus
      `:modules:payment:check`.
- [x] Re-run the repository-wide `check` after the slice.
- [ ] Run PostgreSQL optimistic-lock conflict coverage when Docker is available.

This follows the established Conversation and PendingAction persistence pattern:
JPA remains the default for ordinary tenant-schema aggregate CRUD, while the
application/domain layers do not depend on JPA types.

## Current slice 18AF — Preserve versioned CatalogItem updates

CatalogItem is a tenant-owned JPA aggregate with shared optimistic versioning
and a domain status transition. Its adapter previously rebuilt an entity with a
domain-assigned ID for every save, which bypassed the managed update pattern and
could lose persistence version state. New items now defer identity generation to
JPA; existing items are loaded by ID and only their mutable status is applied.

- [x] Add a failing adapter regression for an existing CatalogItem update.
- [x] Make new CatalogItem identity JPA-generated and nullable before persist.
- [x] Conditionally restore entity identity and update status on the managed
      entity.
- [x] Run the focused Catalog persistence test and full Catalog check.
- [x] Re-run the repository-wide `check` after the slice.
- [ ] Run PostgreSQL optimistic-lock conflict coverage when Docker is available.

CatalogItemImage remains unchanged because its current contract is create and
delete-only with no domain mutation path. Customer identity and membership also
remain unchanged because they are shared control-plane persistence without the
tenant-owned versioned aggregate model.
