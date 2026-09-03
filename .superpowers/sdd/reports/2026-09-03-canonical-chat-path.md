# Canonical Chat Path Checkpoint

**Date:** 2026-09-03
**Branch:** `feat/ai-platform-foundation`
**Status:** Implemented scoped slice; broader module baseline remains failing

## Scope

The approved slice is to make assistant chat depend on one required
`ChatCompletionPort`, remove `ChatService`'s direct `AiModelProvider` fallback,
and provide a mutually exclusive compatibility composition path that preserves
the existing selector, admission, and durable tracing behavior.

## Implementation

- `ChatService` now requires one `ChatCompletionPort` and no longer imports or
  invokes `AiModelProvider`, `ChatProviderFailurePolicy`, or a legacy fallback.
- `LegacyChatCompletionConfiguration` adapts the existing provider only when
  `app.ai.spring-chat.enabled` is false or absent. It applies the existing
  selector, generation admission scheduler, failure policy, and durable tracing
  wrapper.
- `SpringAiChatConfiguration` exposes its selected provider as an
  `IdentifiedChatCompletionPort`; the enabled Spring chat root and compatibility
  root are mutually exclusive by property.
- Regression coverage now includes canonical service behavior, compatibility
  tracing/admission, Spring-enabled application-context selection, and source
  architecture guards.

## Exact verification result

Command run with the repository's Java 25 toolchain:

```text
mise exec -- ./gradlew :modules:assistant:test --tests 'com.emme.assistant.ai.application.service.ChatServiceTest' --tests 'com.emme.assistant.ai.application.service.ChatServiceProviderFallbackTest' --no-daemon
```

Result: **passed** for the focused canonical-path classes. The result files
report 42 assistant tests and 9 ai-platform tests, with zero skips, failures, or
errors:

```text
ChatServiceTest                         10 passed
ChatServiceProviderFallbackTest          2 passed
ChatModelSelectorTest                    7 passed
TracingChatCompletionPortTest            3 passed
SpringAiChatConfigurationTest             8 passed
LegacyChatCompletionConfigurationTest     4 passed
ChatCompositionArchitectureTest           2 passed
SpringAiChatClientAdapterTest              6 passed
SpringAiModelProviderTest                  2 passed
SpringAiChatModelTest                      2 passed
AiProviderConfigurationTest                 4 passed
AiProviderWiringArchitectureTest            1 passed
```

The initial red run failed at `:modules:assistant:compileTestJava` with twelve
constructor errors after the regression tests were updated first. A later
configuration red run exposed two wiring defects (unconditional legacy bean
creation and unstubbed test-provider identity); both were fixed and the focused
tests subsequently passed.

## Broader verification limitation

The combined module invocation also executed the existing broader assistant
suite and ended with **456 tests completed, 18 failed**. Those failures are
outside this slice and include pre-existing package metadata and application
context failures, notably missing `DataSource`/`coreDataSource` setup in
conversation and AI module context tests. The output also shows the existing
`RagQueryService` legacy compatibility path, which is intentionally the next
separate RAG/embedding slice from the final-review document.

Spotless was not clean at repository baseline: the unrelated
`libraries/ai-contracts/src/main/java/com/emme/ai/contracts/rag/package-info.java`
file violates the configured formatter. The scoped assistant source was
formatted by the module task during verification; no unrelated file was
changed.

## Checkpoint files

- `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ChatService.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ChatServiceTest.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ChatServiceProviderFallbackTest.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/LegacyChatCompletionConfiguration.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiChatConfiguration.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/LegacyChatCompletionConfigurationTest.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/configuration/ChatCompositionArchitectureTest.java`
- `docs/superpowers/plans/2026-09-03-canonical-chat-path.md`

The unrelated pre-existing dirty-worktree changes were not staged.
