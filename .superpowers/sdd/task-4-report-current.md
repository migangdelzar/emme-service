# Task 4 — Semantic routing, cache, and RAG consolidation

**Date:** 2026-09-03
**Branch:** `feat/ai-platform-foundation`
**Status:** Implemented; focused execution blocked by an unrelated pre-existing compile error.

## Summary

- Completed the recoverable `RagQueryService` boundary edit to depend on the canonical
  `KnowledgeSearch` and `ChatCompletionPort` contracts only.
- Removed the service's legacy `AiModelProvider` and scheduler fallback, so retrieval-backed
  answers cannot bypass the canonical chat boundary.
- Preserved backend AI-context propagation, tenant-scoped retrieval, security-failure
  propagation, safe empty-grounding behavior, and bounded retrieval/provider-unavailable
  responses.
- Added a blank-question regression test and fail-closed validation before retrieval or chat.
- Added an architecture guard that rejects legacy model-provider/scheduler dependencies in the
  RAG query service.

## TDD evidence

1. Added `rejectsBlankQuestionsBeforeSearchingOrCompleting` before the production validation.
2. Ran the focused Gradle test command. The build stopped during dependency compilation at the
   unrelated dirty file `modules/ai-platform/src/main/java/com/emme/ai/platform/configuration/AiProviderConfiguration.java:110`
   (`')' or ',' expected`), before the assistant tests could execute.
3. Added the minimal validation required by the test. Static diff and whitespace verification
   pass.

## Verification

| Check | Result |
|---|---|
| Focused `:modules:assistant:test` RAG and chat architecture tests | Blocked before test execution by unrelated `AiProviderConfiguration.java` syntax error |
| `git diff --check` | Pass |
| Scoped source/diff review | Pass; only Task 4 RAG files are staged |

## Concerns

- The focused and full assistant test suites cannot run until the unrelated dirty AI-platform
  syntax error is repaired. That file was not touched or staged.
- Existing dirty files outside the allowed RAG scope were preserved.
- No changes were made to semantic cache, persistence, or Spring AI adapter implementations;
  existing Task 6 guarantees remain unchanged.
