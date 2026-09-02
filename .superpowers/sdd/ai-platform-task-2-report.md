# AI Platform Task 2 Report

## Status

Task 2 is implemented on `feat/ai-platform-foundation`. Spring AI chat and
embedding adapters now live in `modules:ai-platform`; assistant provider
registries use those adapters through local application-port bridges while
retaining the existing provider-chain fallback and tracing boundaries.

## Changes

- Added `SpringAiChatModel`, a provider/model-identified adapter over Spring
  AI `ChatClient`. It preserves the existing system prompt, advisor, tool
  callback, and tool-search session scoping behavior without parsing HTTP
  payloads.
- Added `SpringAiEmbeddingModel`, a provider/model-identified adapter over
  Spring AI `EmbeddingModel`. It validates configured dimensions and converts
  the returned vector to the existing application representation.
- Added direct Spring AI model/client dependencies to `modules:ai-platform`.
- Added conditional adapter factory beans to `AiProviderConfiguration` for
  the existing named Ollama Spring AI beans.
- Updated assistant chat and embedding provider registries to consume the
  platform adapters, translating only the local provider-unavailable boundary
  required by the existing fallback chains.
- Added focused delegation, provider identity, error propagation, and
  configuration factory tests.

## TDD Evidence

1. Red: the focused adapter/configuration test command failed during test
   compilation because the new adapter classes and direct Spring AI
   dependencies were absent.
2. Green: the implementation was added and the focused platform tests passed.
3. Post-format green: the platform and assistant focused suites passed after
   formatting.

## Verification

- `mise exec java@25.0.2 -- ./gradlew :modules:ai-platform:test` — passed.
- `mise exec java@25.0.2 -- ./gradlew :modules:ai-platform:spotlessCheck` —
  passed.
- Focused assistant chat/embedding/configuration tests — passed:
  `SpringAiChatClientAdapterTest`, `SpringAiEmbeddingAdapterTest`,
  `SpringAiChatConfigurationTest`, `SpringAiEmbeddingConfigurationTest`, and
  `SpringAiRedisSemanticConfigurationTest`.
- `git diff --check` — passed before commit.

## Concerns and scope limits

- The default shell Java 17 runtime cannot configure this Java 25+ Gradle
  build. All successful verification used `mise exec java@25.0.2`; no
  `--no-verify` bypass was needed for verification.
- Module-wide `:modules:assistant:spotlessCheck` still reports pre-existing
  formatting violations in unrelated assistant files. Those files were not
  modified.
- Raw Ollama/Groq providers, the old assistant adapter source files, and
  provider selection/fallback policy remain in place for later plan tasks.
- Unrelated dirty-worktree files were not staged or changed.
