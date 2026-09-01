# RAG Contract Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the unused and duplicate RAG retrieval contracts with one framework-neutral `KnowledgeSearch` contract without changing behavior.

**Architecture:** `RagQueryService` depends on one shared `ai-contracts.rag.KnowledgeSearch` interface. The Spring AI/pgvector adapter remains behind that interface. No Spring AI, Redis, JDBC, or vector-store types enter the shared contract.

**Tech Stack:** Java 25+, Gradle, Spring Boot, Spring AI, PostgreSQL/pgvector, JUnit 5, ArchUnit.

## Global Constraints

- Preserve tenant filtering and retrieval abstention behavior.
- Do not modify tools, chat, graph, extraction, workflow, or payment behavior.
- Do not stage unrelated dirty-worktree changes.
- Use strict TDD: failing test, minimal implementation, refactor, verification.
- Delete old contracts only after repository-wide caller search and compilation.

## Files and responsibilities

| File | Responsibility |
|---|---|
| `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/rag/KnowledgeSearch.java` | Canonical shared retrieval port |
| `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/rag/KnowledgeRetriever.java` | Removed unused duplicate |
| `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/KnowledgeRetrievalPort.java` | Removed duplicate local port |
| `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/RagQueryService.java` | Consumes canonical port |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/DocumentKnowledgeRetrievalAdapter.java` | Implements canonical port |
| `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiRagConfiguration.java` | Wires canonical port |
| Existing RAG tests | Verify migration and unchanged behavior |

### Task 1: Add the canonical contract and migration test

**Files:**

- Create: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/rag/KnowledgeSearch.java`
- Test: existing `libraries/ai-contracts/src/test/java/com/emme/ai/contracts/ContractValidationTest.java` or a focused RAG contract test following repository conventions

- [ ] **Step 1: Write the failing test** asserting a `KnowledgeSearch` implementation can accept `KnowledgeQuery` and `AiExecutionContext` and return `List<RetrievedDocument>`.
- [ ] **Step 2: Run the focused contract test** with `./gradlew :libraries:ai-contracts:test --tests '*Rag*'`; expect failure because `KnowledgeSearch` does not exist.
- [ ] **Step 3: Add the minimal interface:**

```java
public interface KnowledgeSearch {
  List<RetrievedDocument> search(KnowledgeQuery query, AiExecutionContext context);
}
```

- [ ] **Step 4: Run the focused contract test** and confirm it passes.
- [ ] **Step 5: Commit** with `feat(ai-contracts): add canonical knowledge search port`.

### Task 2: Migrate assistant RAG callers

**Files:**

- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/RagQueryService.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/DocumentKnowledgeRetrievalAdapter.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiRagConfiguration.java`
- Modify: existing RAG unit/configuration tests

- [ ] **Step 1: Update or add a failing test** expecting `RagQueryService` and the adapter wiring to use `KnowledgeSearch`.
- [ ] **Step 2: Run focused RAG tests** with `./gradlew :modules:assistant:test --tests '*Rag*'`; expect compilation or type failures from the old port.
- [ ] **Step 3: Change the adapter and service dependency to `KnowledgeSearch`; implement `search(query, context)` by preserving the existing tenant-scoped retrieval behavior.**
- [ ] **Step 4: Update configuration and test doubles to expose `KnowledgeSearch`.
- [ ] **Step 5: Run focused tests** and confirm all pass.
- [ ] **Step 6: Commit** with `refactor(assistant): use canonical knowledge search port`.

### Task 3: Remove duplicate contracts and enforce the boundary

**Files:**

- Delete after verification: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/rag/KnowledgeRetriever.java`
- Delete after verification: `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/KnowledgeRetrievalPort.java`
- Modify: RAG package-info files if names are documented
- Test: assistant architecture/package convention tests

- [ ] **Step 1: Add a failing architecture assertion** that assistant application code depends on `com.emme.ai.contracts.rag.KnowledgeSearch`, not a local retrieval port.
- [ ] **Step 2: Run the architecture test** and confirm it fails while duplicate references remain.
- [ ] **Step 3: Search callers** with `rg -n '\b(KnowledgeRetriever|KnowledgeRetrievalPort)\b' libraries modules`; verify only declarations or migration remnants remain.
- [ ] **Step 4: Delete both unused duplicate files and update imports/documentation.**
- [ ] **Step 5: Run verification:**

```bash
./gradlew :libraries:ai-contracts:test :modules:assistant:test
./gradlew :modules:assistant:check
git diff --check
```

- [ ] **Step 6: Commit** with `refactor(ai): remove duplicate rag retrieval contracts`.

## Definition of Done

- [ ] One canonical `KnowledgeSearch` contract exists.
- [ ] No assistant-local RAG retrieval port remains.
- [ ] No unused `KnowledgeRetriever` contract remains.
- [ ] Tenant filtering and abstention behavior are unchanged.
- [ ] Focused tests, module checks, and formatting pass.
- [ ] Unrelated dirty files remain unstaged.
- [ ] Commits are pushed to `feat/ai-platform-foundation`.
