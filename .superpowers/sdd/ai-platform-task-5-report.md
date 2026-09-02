# Task 5 Report: Simplify Admission and Observations

## Status

Implemented and verified. Scoped changes are ready to commit and push on
`feat/ai-platform-foundation`.

## Scope

- Clarified capability admission through `ModelCapacityProfile.limitFor(...)`.
- Simplified semaphore acquisition and rollback while preserving the existing
  global, capability, tenant, user, and queue limits.
- Preserved the shared generation/vision capacity and existing timeout,
  interruption, and permit-release behavior.
- Delegated chat and embedding observations to Spring AI using the active
  `ObservationRegistry`.
- Verified provider and model observation fields for Ollama chat and embedding
  calls. Spring AI's Ollama embedding shorthand did not populate the request
  model, so a narrow convention supplies the configured model name while
  retaining Spring AI's standard convention fields.
- Retained assistant tracing wrappers. They persist durable, redacted
  `AiModelExecutionTrace` records and business fields that Spring observations
  do not replace; removing them would change behavior.

## TDD Evidence

- Red: observation tests initially failed to compile because registry-aware
  provider factory boundaries were absent.
- Green: registry-aware chat and embedding factories were added; provider and
  model observation assertions passed.
- Red: `ModelCapacityProfileTest` initially failed to compile because
  `limitFor(ModelCapability)` was absent.
- Green: `limitFor(...)` was added and the scheduler now uses that boundary.
- Refactor: admission acquisition now records acquired semaphores and rolls
  them back uniformly, with no business behavior change.
- Characterization coverage verifies capability isolation, queue timeout,
  interruption removal, permit release, and existing fairness behavior.

## Verification

Passed:

```text
mise exec java@25.0.2 -- ./gradlew :modules:ai-platform:test
BUILD SUCCESSFUL

mise exec java@25.0.2 -- ./gradlew :modules:ai-platform:test \
  --tests com.emme.ai.platform.model.BoundedModelExecutionSchedulerTest \
  --tests com.emme.ai.platform.model.ModelCapacityProfileTest \
  --tests com.emme.ai.platform.configuration.AiProviderConfigurationIntegrationTest
BUILD SUCCESSFUL

mise exec java@25.0.2 -- ./gradlew :modules:assistant:test \
  --tests com.emme.assistant.ai.configuration.SpringAiChatConfigurationTest \
  --tests com.emme.assistant.ai.configuration.SpringAiEmbeddingConfigurationTest \
  --tests com.emme.assistant.ai.application.provider.TracingEmbeddingModelPortTest
BUILD SUCCESSFUL

mise exec java@25.0.2 -- ./gradlew :modules:ai-platform:spotlessJavaCheck
BUILD SUCCESSFUL
```

`git diff --check` is clean for all scoped ai-platform and assistant
configuration paths.

The aggregate `:modules:assistant:spotlessJavaCheck` was not green because it
scans unrelated pre-existing dirty files (including persistence, semantic
cache, and web-security files); none of the scoped Task 5 configuration files
were listed as violations.

Spring AI observation behavior was checked against the official observability
and ChatClient documentation:

- [Spring AI observability](https://docs.spring.io/spring-ai/reference/observability/index.html)
- [Spring AI ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)

## Concerns

- The assistant module's full formatting task remains coupled to unrelated
  worktree changes and should be rerun after those changes are isolated or
  formatted by their owners.
- No tracing wrapper was removed because Spring observations do not provide
  the durable persistence, redaction, payload, and business outcome contract
  currently supplied by those wrappers.
