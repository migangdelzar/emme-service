# Canonical Chat Path Implementation Plan

> **For agentic workers:** Execute this plan task-by-task with strict Red → Green → Refactor TDD.

**Goal:** Make assistant chat execute through one required application chat port so legacy model compatibility cannot bypass selector fallback, admission, policy, or durable tracing.

**Architecture:** `ChatService` will own chat orchestration while receiving one required `ChatCompletionPort`. Spring AI remains the enabled runtime composition root through `SpringAiChatProviderRegistry` and `ChatModelSelector`; when Spring chat is disabled, an assistant composition adapter will expose the existing `AiModelProvider` through the same selector, scheduler, and tracing boundary. `RagQueryService` and non-chat `AiModelProvider` capabilities remain outside this slice.

**Tech Stack:** Java 25, Spring Boot, Spring AI, Gradle, JUnit 5, AssertJ, Mockito.

## Global Constraints

- Preserve tenant/authentication context requirements enforced by `AiExecutionContextScope`.
- Preserve configured selector fallback order and fallback only on `ChatProviderUnavailableException`.
- Preserve model admission through the existing `ModelExecutionScheduler`.
- Preserve durable Emme chat tracing through `TracingChatCompletionPort`.
- Do not introduce a new universal AI abstraction.
- Do not stage or modify unrelated existing dirty-worktree changes.

## Task 1: Lock canonical service behavior

**Files:**
- Modify: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ChatServiceTest.java`
- Modify: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ChatServiceProviderFallbackTest.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ChatService.java`

- [ ] Write failing tests showing `ChatService` accepts the required chat port, returns its result, preserves context/cache/tool behavior, and never invokes `AiModelProvider.chat` after a chat-port provider outage.
- [ ] Run the focused ChatService tests and observe compilation/test failure caused by the old constructors and fallback.
- [ ] Remove the service’s `AiModelProvider`, `ModelCapability`, scheduler, admission, and legacy fallback fields/constructors; require `ChatCompletionPort` while retaining cache, proactive tools, metrics, and context checks.
- [ ] Run the focused service tests and refactor only for clarity while keeping all tests green.

## Task 2: Provide one compatibility composition path

**Files:**
- Create: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/LegacyChatCompletionConfiguration.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiChatConfiguration.java`
- Create: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/LegacyChatCompletionConfigurationTest.java`
- Modify: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/SpringAiChatConfigurationTest.java`

- [ ] Write failing configuration tests proving the compatibility provider is exposed as an identified chat port with scheduler admission and tracing, and is absent when the Spring chat composition root is selected.
- [ ] Run those tests and observe the missing configuration/conditional behavior.
- [ ] Implement the compatibility bean around the existing `AiModelProvider` using `ChatModelSelector` and `TracingChatCompletionPort`; make the Spring bean return `IdentifiedChatCompletionPort` so both roots are mutually exclusive by type.
- [ ] Run focused configuration tests and refactor only without changing behavior.

## Task 3: Add architecture regression and report

**Files:**
- Create: `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/ChatCompositionArchitectureTest.java`
- Create: `.superpowers/sdd/reports/2026-09-03-canonical-chat-path.md`

- [ ] Write failing source-architecture tests rejecting `AiModelProvider`/legacy fallback references in `ChatService` and requiring mutually exclusive chat composition roots.
- [ ] Run the architecture test and observe its failure against the current source.
- [ ] Implement the minimum source/configuration changes needed for the test to pass.
- [ ] Run focused Java 25 assistant and ai-platform tests, then record exact commands/results and remaining compatibility uses in the report.
- [ ] Commit and push only the plan, chat production/tests, architecture test, and report; verify the remote SHA.
