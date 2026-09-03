# Task 5 — Current graph/workflow/image audit

**Date:** 2026-09-03
**Branch:** `feat/ai-platform-foundation`
**Status:** Focused fixes implemented; full verification is blocked by unrelated dirty compilation failures.

## Scope and audit

- AGE remains optional at configuration level and recommendation-focused. `AgeGraphAdapter` fails projection when AGE is unavailable, returns an empty recommendation set for unavailable retrieval, bounds traversal limits, and requires the bound backend tenant context. `JdbcAgeGraphClient` uses fixed traversal templates with tenant predicates and does not provide authoritative business state.
- LangGraph checkpoints are tenant/workflow/conversation scoped and require an authenticated `AiExecutionContext`; staff review access is role-gated in the JDBC saver. The generic and quote graphs persist interruptible state through the checkpoint boundary.
- Image metadata is tenant/workflow scoped. `TenantImageWriter.delete` has one production implementation through catalog's `ImageStorage` and the existing controller callers; no implementation was missing.
- Quote workflow persistence already applies tenant predicates and optimistic version predicates, but its idempotent insert used `ON CONFLICT DO NOTHING RETURNING` with `.single()`. A concurrent duplicate key therefore produced no row and raised instead of returning the durable workflow.
- Quote graph execution uses the `<workflow UUID>:quote` LangGraph thread namespace. Resume used the bare workflow UUID, so it could not address the paused quote checkpoint.

## Changes in this slice

- Added a regression test proving duplicate quote workflow inserts return the existing durable row.
- Changed the quote insert conflict action to a no-op update, preserving the existing row while allowing PostgreSQL `RETURNING` to supply it.
- Added a regression assertion for the quote resume thread namespace and aligned the resume adapter with quote execution.

## Verification

- `./gradlew :modules:assistant:test --tests '...JdbcQuotePersistenceAdapterTest.returnsTheExistingWorkflowWhenAnIdempotentInsertConflicts'` — blocked before test execution by unrelated dirty `modules/ai-platform/.../AiProviderConfiguration.java:110` syntax error.
- Same focused command with `-x :modules:ai-platform:compileJava` — blocked by unrelated dirty tenancy compilation errors (`TenantRealmReady` and `TenantProvisioningRepository` symbols).
- Same focused command additionally excluding tenancy compilation — blocked by pre-existing dirty `libraries/ai-contracts` provider-boundary class output/source mismatch for `ChatModel` and `EmbeddingModel`.
- `git diff --check` — no whitespace errors in the current worktree.

## Concerns

The full focused test suite cannot execute until the unrelated provider-boundary and identity/tenancy dirty changes are made compilable. Those files were not modified or staged. Existing allowed-scope formatter changes were also preserved unstaged; this commit contains only the focused behavioral fixes, their tests, and this report.
