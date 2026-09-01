# Task 1 Implementation Report: Canonical RAG Contract

## Status

Complete. Task 1 adds the canonical `KnowledgeSearch` contract and a focused contract test proving its signature and result type.

## Scope

Changed only the Task 1 files:

- `libraries/ai-contracts/src/test/java/com/emme/ai/contracts/rag/RagContractTest.java`
- `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/rag/KnowledgeSearch.java`
- `.superpowers/sdd/task-1-report.md`

Unrelated pre-existing dirty files were not modified or staged. No later task files were touched.

## Implementation

Added the framework-independent `KnowledgeSearch` interface in the existing `com.emme.ai.contracts.rag` package:

```java
List<RetrievedDocument> search(KnowledgeQuery query, AiExecutionContext context);
```

The focused test creates a `KnowledgeQuery`, canonical `AiExecutionContext`, and `RetrievedDocument`; it verifies that an implementation receives the same query and context and returns the retrieved document list.

## TDD Evidence

1. Added `RagContractTest` before production code.
2. Ran the required focused test and observed the expected compile failure because `KnowledgeSearch` did not exist (`cannot find symbol`).
3. Added the minimal interface.
4. Reran the focused test successfully.

## Verification

The repository’s build logic requires Java 25 or newer. The default Java 17 runtime could not configure Gradle, so verification used the installed Java 26 runtime via `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home`.

| Command | Result |
|---|---|
| `./gradlew :libraries:ai-contracts:test --tests '*Rag*'` with Java 17 | Not reached compilation; Gradle requires JVM 25+ |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home ./gradlew :libraries:ai-contracts:test --tests '*Rag*'` | Passed |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home ./gradlew :libraries:ai-contracts:test` | Passed |

No `--no-verify` override was used.

## Concerns

- Local default Java is 17 while the current Gradle build logic requires Java 25+; contributors and CI need a compatible JDK or explicit `JAVA_HOME`.
- The interface intentionally has no implementation or migration adapter; those belong to later tasks in the approved plan.

## Git Handoff

The Task 1 files will be committed with:

`feat(ai-contracts): add canonical knowledge search port`
