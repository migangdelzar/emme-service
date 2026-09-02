# AI Platform Simplification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Simplify `ai-platform` and assistant provider integration by delegating model, embedding, transport, tool, observation, event, and persistence mechanics to existing Spring and infrastructure capabilities.

**Architecture:** `ai-platform` remains one module containing provider, admission, and learning infrastructure. Assistant use cases depend on framework-neutral contracts; Spring AI adapters provide chat, embedding, structured output, and provider integration. Custom code remains only for Emme policy, tenant safety, deterministic routing, admission, fallback policy, and learning governance.

**Tech Stack:** Java 25+, Gradle, Spring Boot, Spring AI, Spring Modulith, Spring Data Redis, PostgreSQL, Micrometer/OpenTelemetry, JUnit 5, Testcontainers.

## Global Constraints

- Do not duplicate capabilities already provided by Spring Boot, Spring AI, Spring Data/JDBC, Redis, Kafka, Spring Modulith, or LangGraph4j.
- Preserve tenant isolation, authorization, idempotency, audit, model admission, and fallback behavior.
- Do not route intent from provider implementations; intent belongs to assistant routing.
- Do not delete raw provider adapters until replacement tests and callers are verified.
- Do not touch unrelated dirty-worktree changes.
- Use strict TDD and commit each task independently.

## Task 1: Separate provider capabilities

**Files:**

- Modify: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/model/AiModelProvider.java`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider/ollama/OllamaModelProvider.java`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider/groq/GroqModelProvider.java`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider/mock/MockModelProvider.java`
- Test: existing provider and contract tests

- [ ] Write failing tests proving provider implementations no longer own intent routing.
- [ ] Run focused provider tests and confirm failure.
- [ ] Remove `routeIntent` and unrelated routing vocabulary from provider transport contracts/implementations while preserving chat and embedding behavior.
- [ ] Run provider and contract tests and confirm pass.
- [ ] Commit `refactor(ai): separate provider transport from intent routing`.

## Task 2: Introduce Spring AI model adapters

**Files:**

- Create or modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiChatModel.java`
- Create or modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiEmbeddingModel.java`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/configuration/AiProviderConfiguration.java`
- Modify: assistant provider configuration and focused tests

- [ ] Write failing adapter tests for Spring AI chat and embedding delegation, provider identity, and error propagation.
- [ ] Run focused tests and confirm failure.
- [ ] Implement thin adapters over existing Spring AI `ChatClient`/model and embedding interfaces; do not reproduce HTTP payload parsing.
- [ ] Run focused tests and confirm pass.
- [ ] Commit `feat(ai-platform): add spring ai model adapters`.

## Task 3: Migrate provider selection and fallback

**Files:**

- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/provider/ChatProviderChain.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/provider/EmbeddingProviderChain.java`
- Rename only after migration: provider selector classes and related tests
- Modify: Spring AI configuration tests

- [ ] Write failing tests for ordered fallback, provider/model identity, admission scheduling, and non-fallback error propagation using the canonical adapters.
- [ ] Run focused tests and confirm failure.
- [ ] Replace generic provider-chain naming with a small `ChatModelSelector`/embedding selection policy while retaining existing safe fallback semantics.
- [ ] Run assistant provider, embedding, and configuration tests.
- [ ] Commit `refactor(assistant): simplify model selection policy`.

## Task 4: Remove duplicate raw transport only after verification

**Files:**

- Delete only when repository-wide caller search is clean: raw OkHttp provider implementations and unused HTTP helpers
- Modify: Gradle dependencies only if no remaining consumer requires them
- Test: provider integration/contract tests

- [ ] Add a failing architecture assertion that active provider wiring uses Spring AI adapters.
- [ ] Run the assertion and confirm current raw provider wiring fails it.
- [ ] Switch configuration to Spring AI provider beans and preserve deterministic mock configuration.
- [ ] Run provider contract tests, assistant focused tests, and integration tests with test doubles.
- [ ] Search callers with `rg` and delete only unused raw transport classes/dependencies.
- [ ] Run `./gradlew :modules:ai-platform:test :modules:assistant:test` with Java 25+.
- [ ] Commit `refactor(ai): remove duplicated model transport`.

## Task 5: Simplify admission and observations

**Files:**

- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/model/BoundedModelExecutionScheduler.java`
- Modify: `modules/ai-platform/src/main/java/com/emme/ai/platform/model/ModelCapacityProfile.java`
- Modify: assistant tracing wrappers only if Spring observations cover required fields
- Test: scheduler and observation tests

- [ ] Write failing tests for capacity limits, timeout, interruption, and provider/model observation fields.
- [ ] Run focused tests and confirm failure where the simplified boundary is absent.
- [ ] Rename scheduler only if the clearer name does not create compatibility churn; retain the existing contract if consumers depend on it.
- [ ] Remove tracing wrappers only after equivalent Micrometer/OpenTelemetry coverage is verified.
- [ ] Run focused tests and commit `refactor(ai-platform): delegate observations and clarify admission`.

## Task 6: Verify learning remains asynchronous infrastructure

**Files:**

- Review/modify only if required: `modules/ai-platform/src/main/java/com/emme/ai/platform/learning/*`
- Test: learning lifecycle and worker tests

- [ ] Add or update tests proving learning evaluation is asynchronous and cannot mutate production routing from one interaction.
- [ ] Confirm persistence uses PostgreSQL and scheduled coordination uses existing Modulith/ShedLock mechanisms.
- [ ] Remove no learning behavior unless it is demonstrably duplicated by an existing framework.
- [ ] Commit `test(ai-platform): verify asynchronous learning boundaries`.

## Final verification

- [ ] Run focused `ai-platform` and assistant tests.
- [ ] Run contract and architecture tests.
- [ ] Run relevant integration tests for Spring AI provider wiring.
- [ ] Run formatting and `git diff --check`.
- [ ] Confirm unrelated dirty files remain unstaged.
- [ ] Update the blueprint and progress ledger with actual removals and remaining intentional abstractions.
- [ ] Push all commits to `feat/ai-platform-foundation`.

## Definition of Done

- [ ] Spring AI owns supported model and embedding transport.
- [ ] Provider implementations no longer perform intent routing.
- [ ] Assistant uses one clear selection/fallback policy.
- [ ] Duplicate raw transport is removed only after verified migration.
- [ ] Admission, tenant safety, fallback, audit, and observability behavior remain intact.
- [ ] Learning remains durable, asynchronous, and governed.
- [ ] Tests pass for all changed paths and limitations are documented.
