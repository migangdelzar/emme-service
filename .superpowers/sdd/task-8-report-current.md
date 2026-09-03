# Task 8 — Current admission, observability, learning evaluation, and persistence report

**Date:** 2026-09-03
**Branch:** `feat/ai-platform-foundation`
**Scope:** Audit and concrete-gap remediation for Task 8

## Status

Complete for the audited Task 8 scope. Existing admission, trace/metric, candidate-evidence,
offline-evaluation, idempotency, and promotion-separation work was retained. This slice adds
only the persistence isolation gaps found during the audit.

## Audit outcome

- `BoundedModelExecutionScheduler` already provides bounded global, capability, tenant, and
  principal admission; FIFO per-tenant queues; round-robin tenant fairness; deadline timeout;
  interruption cleanup; permit release on failure; and no internally created executor. Existing
  tests use virtual-thread callers, so no scheduler implementation change was required.
- Existing assistant trace and metrics adapters already redact payloads and enforce bounded
  labels without tenant/principal metric cardinality.
- Existing learning policy and lifecycle tests cover PII/policy/evidence gates, durable capture
  before asynchronous evaluation dispatch, idempotent evaluation versions, optimistic state
  transitions, asynchronous evaluation, and promotion separation.

## Changes in this slice

- Added `FORCE ROW LEVEL SECURITY` to the durable candidate migration (`019`) and evaluation
  migration (`020`).
- Changed `JdbcLearningCandidateEvaluationStore` to insert evaluation evidence only when the
  candidate exists in the bound tenant, preventing a tenant-scoped evaluation row from being
  attached to another tenant's candidate ID.
- Added focused regression/contract tests for forced RLS and candidate ownership scoping.

## TDD evidence

1. Added the migration and JDBC contract tests before implementation changes.
2. The migration test failed on the missing forced-RLS clauses.
3. Added the minimum migration and SQL ownership predicates.
4. The focused migration contract suite passed.

## Verification

| Check | Result |
|---|---|
| `:database:test --tests com.emme.database.AiLearningCandidateMigrationContractTest` | **PASS — 6 tests, zero failures/skips** |
| `git diff --check` | **PASS** |
| Full AI-platform focused command | **BLOCKED by pre-existing unrelated dirty syntax error** in `AiProviderConfiguration.java:110`; provider-boundary file was not touched |

## Concerns

- The AI-platform Java test suite cannot currently compile until the unrelated existing
  `AiProviderConfiguration.java` syntax error is resolved. This report intentionally does not
  modify that provider-boundary file because it is outside Task 8's allowed scope and already
  dirty at task start.
- The JDBC ownership test is committed with the adapter change but could not execute while the
  same unrelated AI-platform compilation error remains.
- Existing unrelated worktree changes in identity, tenancy, subscriptions, assistant provider/
  graph/workflow/persistence, contracts, database tests, and task tracking were preserved and
  are not part of this slice.
