# AI Platform Task 1 Implementation Report

## Status

Task 1 is implemented within the approved scope, including the assistant integration compatibility fix. Provider transport contracts and implementations no longer own intent routing; chat and embedding capabilities remain on the provider boundary, while intent detection now uses the assistant semantic routing boundary.

## Changes

- Removed `routeIntent` and the nested `IntentResult` type from `AiModelProvider`.
- Removed intent-classification logic and routing-only imports from `OllamaModelProvider`, `GroqModelProvider`, and `MockModelProvider`.
- Kept provider identity, chat, embedding, caption, and mock-capability behavior unchanged.
- Replaced the legacy contract-result test with a source boundary test that rejects routing vocabulary from the contract and all three provider transports.
- Migrated `DetectIntentService` and its focused tests off the removed `AiModelProvider.routeIntent` API.
- Preserved semantic results, backend AI execution-context enforcement, and non-transient failure propagation.
- Added explicit zero-confidence `GENERAL` results for semantic abstention, disabled routing, and transient embedding/vector failures instead of guessing or routing through a provider.

## TDD Evidence

1. Red: `./gradlew :libraries:ai-contracts:test --tests com.emme.ai.contracts.ContractValidationTest` failed because `AiModelProvider.java` still contained `routeIntent`.
2. Green: after the scoped implementation, the same focused contract test passed.
3. Green verification: `./gradlew :libraries:ai-contracts:test :modules:ai-platform:test` passed under Java 26.

### Integration compatibility fix

1. Red: `./gradlew :modules:assistant:test --tests com.emme.assistant.ai.application.service.DetectIntentServiceTest --no-daemon` failed during assistant compilation because `DetectIntentService` and its tests still referenced the removed `AiModelProvider.IntentResult` and `routeIntent` symbols.
2. Green: after migrating the service to `Optional<SemanticIntentRouter>` and replacing the deleted provider fallback with explicit safe results, the focused test passed with 7 tests completed and 0 failures.
3. Focused semantic verification passed for `DetectIntentServiceTest`, `SemanticIntentRouterTest`, and `SpringAiSemanticConfigurationTest` under the installed Java 26 runtime.

## Verification

- `./gradlew :libraries:ai-contracts:test :modules:ai-platform:test` — `BUILD SUCCESSFUL`.
- `./gradlew :modules:ai-platform:spotlessCheck` — passed.
- `git diff --check` — passed.
- `./gradlew :modules:assistant:test --tests com.emme.assistant.ai.application.service.DetectIntentServiceTest --tests com.emme.assistant.ai.application.service.SemanticIntentRouterTest --tests com.emme.assistant.ai.configuration.SpringAiSemanticConfigurationTest` — passed.
- No production Java source references `routeIntent` or `AiModelProvider.IntentResult`; the contract boundary test intentionally retains the `routeIntent` string assertion, and historical design/plan text retains the term.
- The aggregate `:libraries:ai-contracts:spotlessCheck` remains blocked by a pre-existing formatting violation in `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/rag/package-info.java`; that unrelated file was not changed.

## Concern / Follow-up

Resolved. `DetectIntentService` no longer depends on `AiModelProvider` or provider-owned intent routing. It delegates to the existing assistant `SemanticIntentRouter` when configured and returns an explicit zero-confidence safe result when routing abstains, is disabled, or encounters an allowed transient vector/provider failure. Non-transient and security failures still propagate.

## Scope Guard

The compatibility fix additionally changes only `DetectIntentService` and `DetectIntentServiceTest`; the provider implementations and provider contract remain untouched by this follow-up. Existing unrelated dirty-worktree files remain unstaged and outside the commit.
