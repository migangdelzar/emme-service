# RAG Contract Consolidation — Task 3 Report

**Date:** 2026-09-01
**Branch:** `feat/ai-platform-foundation`
**Status:** Implemented; focused verification passed; broader branch checks remain blocked by unrelated failures.

## Changes

- Deleted the unused shared duplicate `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/rag/KnowledgeRetriever.java`.
- Deleted the unused assistant-local `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/KnowledgeRetrievalPort.java`.
- Updated both RAG/application package descriptions to identify the canonical shared `KnowledgeSearch` boundary.
- Added `AssistantPackageConventionTest.usesTheCanonicalSharedKnowledgeSearchContract`, which asserts:
  - `KnowledgeSearch` exists in `ai-contracts`;
  - both duplicate declarations are absent;
  - `RagQueryService` imports the shared `KnowledgeSearch`; and
  - assistant AI application sources contain neither legacy retrieval identifier.
- No production imports required changes: repository-wide source search found the two removed types only in their declarations, and after removal only the intentional architecture-test assertions remain.

## TDD evidence

1. Added the architecture assertion before deleting either declaration.
2. Ran the focused test under the project-compatible JDK; it failed at the expected assertion because `KnowledgeRetriever.java` still existed.
3. Deleted the two unused declarations and updated package documentation.
4. Reran the focused assertion; it passed.

## Verification

| Command | Result |
|---|---|
| `./gradlew :modules:assistant:test --tests 'com.emme.assistant.AssistantPackageConventionTest.usesTheCanonicalSharedKnowledgeSearchContract'` with JDK 26 | PASS |
| `./gradlew :libraries:ai-contracts:test :modules:assistant:compileJava` with JDK 26 | PASS |
| `git diff --check` | PASS |
| `rg -n '\\b(KnowledgeRetriever|KnowledgeRetrievalPort)\\b' libraries modules` | Only intentional architecture-test guards remain |
| `./gradlew :libraries:ai-contracts:test :modules:assistant:test` with JDK 26 | BLOCKED: 428 tests completed, 18 failures outside this task's five-file diff |
| `./gradlew :modules:assistant:check` with JDK 26 | BLOCKED by the same test failures plus 15 pre-existing Spotless violations and 8 pre-existing Checkstyle star-import violations |

The full-suite failures include stale documents-contract expectations, missing package metadata for the existing assistant storage package, a tenant datasource-name expectation, and JPA application-context failures. They were not changed or fixed because they are outside Task 3 and the dirty worktree must be preserved.

The default shell JDK is 17, while this build requires Java 25 or newer. Verification used the installed JDK 26. No `--no-verify` bypass was used during this task.

## Scope preservation

Only these five implementation files are included in the task commit: two deletions, two package-info updates, and one architecture test update; this report is also committed as requested. Existing unrelated dirty files remain unstaged.
