# AI Platform Task 1 Implementation Report

## Status

Task 1 is implemented within the approved scope. Provider transport contracts and implementations no longer own intent routing; chat and embedding capabilities remain on the provider boundary.

## Changes

- Removed `routeIntent` and the nested `IntentResult` type from `AiModelProvider`.
- Removed intent-classification logic and routing-only imports from `OllamaModelProvider`, `GroqModelProvider`, and `MockModelProvider`.
- Kept provider identity, chat, embedding, caption, and mock-capability behavior unchanged.
- Replaced the legacy contract-result test with a source boundary test that rejects routing vocabulary from the contract and all three provider transports.

## TDD Evidence

1. Red: `./gradlew :libraries:ai-contracts:test --tests com.emme.ai.contracts.ContractValidationTest` failed because `AiModelProvider.java` still contained `routeIntent`.
2. Green: after the scoped implementation, the same focused contract test passed.
3. Green verification: `./gradlew :libraries:ai-contracts:test :modules:ai-platform:test` passed under Java 26.

## Verification

- `./gradlew :libraries:ai-contracts:test :modules:ai-platform:test` — `BUILD SUCCESSFUL`.
- `./gradlew :modules:ai-platform:spotlessCheck` — passed.
- `git diff --check` — passed.
- The aggregate `:libraries:ai-contracts:spotlessCheck` remains blocked by a pre-existing formatting violation in `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/rag/package-info.java`; that unrelated file was not changed.

## Concern / Follow-up

`modules/assistant/src/main/java/com/emme/assistant/ai/application/service/DetectIntentService.java` and related assistant tests still reference the removed legacy provider fallback API. A read-only `:modules:assistant:compileJava` check reports the expected missing `AiModelProvider.IntentResult` and `routeIntent` symbols. This is intentionally not changed in Task 1 because assistant routing migration is assigned to a later task.

## Scope Guard

Only the three provider implementations, the provider contract, the existing contract test, and this report are part of this task’s changes. Existing unrelated dirty-worktree files remain unstaged and untouched.
