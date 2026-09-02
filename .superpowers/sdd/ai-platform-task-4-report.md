# Task 4 Report: Remove Duplicate Raw Transport

## Outcome

Task 4 is complete. Active Ollama and Groq provider wiring now uses Spring AI-backed adapters, the deterministic mock provider remains active for `app.ai.provider=mock`, and the unused raw OkHttp provider implementations and helper were removed after caller verification.

Admission and observability code was not changed.

## Task 4 Review Remediation — 2026-09-02

Both Task 4 review findings are addressed:

1. Provider contract/integration coverage now exercises the concrete Spring AI factories without paid APIs. `AiProviderConfigurationIntegrationTest` uses a local `MockWebServer` to verify Groq's OpenAI-compatible `/chat/completions` request, bearer credential, model, response mapping, and provider-neutral `AiModelProvider` bridge. It also verifies the concrete Ollama chat and embedding factories and request payloads. `AiProviderConfigurationTest` verifies active Groq context wiring.
2. Spring AI provider failures preserve fallback semantics. `ChatProviderFailurePolicy` maps runtime outages and missing-credential failures to `ChatProviderUnavailableException`, including failures from the legacy `AiModelProvider` path. Invalid input and schema failures remain unchanged and propagate without triggering fallback. The active registry and the compatibility Spring AI adapter both use this policy.

No live Ollama, Groq, or paid OpenAI API was contacted.

## Implementation

- Added `SpringAiModelProvider`, the provider-neutral bridge over the existing `SpringAiChatModel` and optional `SpringAiEmbeddingModel` adapters.
- Added Spring AI Ollama and OpenAI-compatible provider dependencies and configuration beans.
- Preserved Groq's unsupported embedding behavior as an empty embedding result.
- Preserved `MockModelProvider` and its deterministic chat/embedding behavior.
- Removed the unused raw `OllamaModelProvider`, `GroqModelProvider`, their package metadata/tests, and `AiProviderHttpClient`.
- Updated architecture and contract tests to assert the Spring AI wiring and absence of duplicate raw transport.

## TDD Evidence

1. Red: architecture/configuration tests were added before the bridge/configuration existed; the focused task run failed for missing `SpringAiModelProvider` and missing active provider wiring.
2. Green: the minimal bridge, Spring AI configuration, dependencies, and raw transport removal were implemented; focused tests passed.
3. Refactor: canonical Spring AI adapters were reused, provider selection stayed conditional, and caller/contract assertions were updated; focused tests remained green.

## Verification

Passed with Java 25 (`mise exec java@25.0.2`):

- `:modules:ai-platform:test`
- `:libraries:ai-contracts:test --tests '*ContractValidationTest'`
- Focused provider configuration, architecture, bridge, Spring AI adapter, and assistant registry/service tests.
- `git diff --check`

Review-remediation verification under Java 25 (`mise exec java@25.0.2`):

- Focused assistant provider/fallback/adapter tests and ai-platform provider/factory tests: passed.
- `:modules:ai-platform:test`: passed, 46 tests.
- The added assistant-focused tests: passed, including the three fallback/error regressions and the Spring AI adapter schema regression.
- `:modules:assistant:test`: 441 tests completed, 18 failures in the same unrelated package-contract, datasource, and broad Spring context coverage already documented above; no remediation test failed.
- `:modules:ai-platform:spotlessCheck`: passed.
- `:modules:assistant:spotlessCheck`: remains blocked by pre-existing formatting violations in unrelated dirty files; the scoped Java files were formatted and no unrelated files were changed.

The required aggregate command was run:

```text
./gradlew :modules:ai-platform:test :modules:assistant:test
```

The ai-platform suite passed. The assistant suite completed 435 tests with 18 failures in existing unrelated coverage: package-contract drift and broad Spring Boot contexts without `entityManagerFactory`. The focused assistant tests relevant to this task passed, including Spring AI configuration, embedding contract, provider fallback, and RAG query coverage.

## Caller and Dependency Audit

Repository-wide search found no remaining callers of the deleted raw provider classes or `AiProviderHttpClient`. The ai-platform direct OkHttp dependency was removed; shared platform and assistant OkHttp dependencies remain because they have other consumers. Jackson remains in ai-platform because learning-store implementations consume `ObjectMapper`.

## Concerns

- The aggregate assistant test task remains red for the pre-existing 18 failures described above; these were not modified because they are outside Task 4's scope.
- Real-provider runtime validation was configuration-level and test-double based; no external Ollama or Groq service was contacted.
- Spring AI configuration follows the official Ollama and OpenAI-compatible Chat Model APIs: [Ollama Chat Model](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html) and [OpenAI Chat Model](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/chat/openai-chat.html).
