# Review: AI Platform Simplification Task 6

| Field | Value |
|---|---|
| Reviewed range | `988cc73d..2d128c55` |
| Commit | `2d128c55 test(ai-platform): verify asynchronous learning boundaries` |
| Scope | Asynchronous learning, PostgreSQL durability, existing coordination, production-routing safety, tests, and change scope |
| Date | 2026-09-03 |

## Findings

None. No High, Medium, or Low-severity findings were identified in the reviewed
range.

## Clarification questions

None.

## Positive observations

- `LearningCandidateService` now requires an explicit
  `LearningCandidateEvaluationRequester`; the removed two-argument constructor can
  no longer silently discard evaluation dispatch.
- `LearningCandidateEvaluationWorker` now requires an explicit
  `LearningCandidateEvaluationStore`; the removed one-argument constructor can no
  longer silently discard durable evaluation evidence.
- The service test verifies the candidate is persisted before the evaluation
  request is issued. Production wiring supplies the Spring Modulith publisher,
  and the event is `@Externalized` with a tenant-partitioned key and an envelope
  containing trusted identifiers/correlation metadata only.
- The JDBC adapters and existing Liquibase migrations provide the expected
  PostgreSQL durability boundary: tenant-scoped tables, `JSONB` evidence/metrics,
  `TIMESTAMPTZ` timestamps, RLS policies, and idempotent unique keys. The commit
  strengthens the corresponding SQL/migration contract tests without changing
  the persistence behavior.
- Evaluation processing transitions through `EVALUATING` and completion only;
  the worker has no promotion or routing dependency, and the test explicitly
  verifies that `promote` is not invoked. Canary-gated promotion remains a
  separate lifecycle operation.
- No learning-specific executor, queue, scheduler, or production-routing
  mutation was added. The existing Spring Modulith publication and application
  AI-job coordination remain outside the customer interaction path, while the
  deferred evaluator/report transport is correctly left for a later phase.
- The seven changed paths are directly related to Task 6. Unrelated dirty
  worktree files were not included in the reviewed commit.

## Verification

The exact target commit was checked in a clean detached worktree using Java 25:

- `:modules:ai-platform:test` filtered to learning tests and
  `:database:test` filtered to `AiLearningCandidateMigrationContractTest` —
  **BUILD SUCCESSFUL**.
- `:modules:assistant:test` filtered to
  `SpringLearningCandidateEvaluationEventPublisherTest` and
  `SpringAiLearningConfigurationTest` — **BUILD SUCCESSFUL**.
- `:applications:emme-platform:test` filtered to `KafkaEventContractTest` and
  `SchedulingConfigurationTest` — **BUILD SUCCESSFUL**.
- `git diff --check 988cc73d 2d128c55` — clean.
- The aggregate `:modules:assistant:test` run reports 18 failures in unrelated
  existing package-convention, datasource, and application-context tests; none
  is in a file changed by Task 6. The focused changed tests pass.

## Recommendation

**Approved** — the change satisfies the Task 6 boundaries and introduces no
High/Medium findings.
