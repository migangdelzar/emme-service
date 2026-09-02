# AI Platform Task 3 Report

## Status

Task 3 is implemented on `feat/ai-platform-foundation`. Assistant provider
selection now has explicit `ChatModelSelector` and `EmbeddingModelSelector`
policies backed by the canonical Spring AI adapters already introduced in Task
2.

## Changes

- Renamed `ChatProviderChain` to `ChatModelSelector` and
  `EmbeddingProviderChain` to `EmbeddingModelSelector`.
- Migrated Spring AI chat and embedding configuration, provider registries, RAG
  composition, and Redis semantic test wiring to the selector names.
- Preserved configured order and fallback only for
  `ChatProviderUnavailableException` / `EmbeddingProviderUnavailableException`.
- Preserved provider/model identity on successful chat completion results.
- Preserved `GENERATION` and `EMBEDDING` model-admission scheduling for every
  attempted candidate, including fallback candidates.
- Preserved immediate propagation of non-fallback failures such as invalid
  requests and invalid embedding dimensions.
- Renamed and expanded selector tests with fallback identity, ordered fallback,
  admission-attempt, and non-fallback propagation coverage.
- Did not delete raw provider implementations or modify later simplification
  tasks.

## TDD Evidence

1. Red: focused tests were changed to the planned selector names before
   production classes existed. Gradle failed during `compileTestJava` with
   unresolved `ChatModelSelector` and `EmbeddingModelSelector` symbols.
2. Green: the selector implementations and all callers were migrated. The
   focused suite initially exposed a test-double mismatch: its scheduler
   wrapped unchecked provider failures, unlike the production scheduler.
3. Refactor/green: the recording scheduler test doubles now preserve unchecked
   exceptions, matching `BoundedModelExecutionScheduler`; the selector suite
   then passed.

## Verification

- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests ...`
  covering both selectors, chat fallback service, and Spring AI
  chat/embedding/RAG/Redis configuration: **50 tests passed**.
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test`: **420 tests,
  18 failures** in existing unrelated package metadata, tenant datasource
  naming, and JPA/application-context tests. No Task 3 test failed.
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:spotlessCheck`:
  blocked by pre-existing formatting violations in unrelated assistant files;
  the changed selector/configuration files were not listed among the reported
  violations.
- `git diff --check`: passed.

## Concerns and scope limits

- The repository's default Java runtime is Java 17; this Java 25+ Gradle build
  was verified with `mise exec java@25.0.2`.
- The full assistant-suite failures and Spotless violations predate this task
  and remain untouched.
- Existing unrelated dirty-worktree files were not staged or modified.
