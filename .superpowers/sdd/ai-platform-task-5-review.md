# Review: AI Platform Simplification Task 5

| Field | Value |
|---|---|
| Reviewed range | `c123cfa5..d6ab7069` |
| Commit | `d6ab7069 refactor(ai-platform): delegate observations and clarify admission` |
| Scope | Correctness, simplification, tenant/security/observability behavior, tests, and scope |

## Findings

### F-01 — Medium — Rejected requests permanently allocate per-tenant and per-user state

**Location:** `modules/ai-platform/src/main/java/com/emme/ai/platform/model/BoundedModelExecutionScheduler.java:155-174`

`tryAcquire()` now constructs `permitsFor(waiter)` before attempting the first
semaphore. `permitsFor()` eagerly calls `computeIfAbsent` for both
`tenantPermits` and `userPermits`, even when the global or capability permit is
already unavailable. `awaitPermit()` invokes this path before checking whether
the bounded queue is full, so every rejected request with a new tenant or
principal inserts two semaphores into maps that have no eviction or cleanup.

This is a regression from the parent commit, where tenant/user entries were
created only after global and capability permits had been acquired. An attacker
or simply a high-cardinality stream of rejected contexts can therefore grow
process memory without being constrained by `queueCapacity`; the queue bounds
waiting work, but not this newly introduced metadata allocation.

Preserve ordered/lazy acquisition: do not create tenant/user semaphores until
the preceding global and capability acquisitions succeed, or otherwise bound
and evict per-identity permit state. Add a regression test that holds global
capacity, submits many distinct contexts that are rejected by a zero/full
queue, and verifies that rejected admission does not materialize identity
state (preferably through a package-visible count or an equivalent bounded
state contract).

## Clarification questions

None.

## Positive observations

- The capability boundary is clearer: `ModelCapacityProfile.limitFor(...)`
  makes the shared `GENERATION`/`VISION` capacity explicit and keeps the
  scheduler mapping in one place.
- The acquisition rollback is shorter and uniform for partial acquisition,
  while the focused scheduler tests cover capacity isolation, timeout,
  interruption cleanup, operation failure, release, and fairness.
- Spring AI receives the active `ObservationRegistry` for chat and embedding
  paths. The embedding convention retains Spring’s default fields and adds the
  configured model name needed by the Ollama shorthand.
- The commit does not remove the assistant tracing wrappers, so durable,
  redacted execution records and business outcome fields remain intact.
- The change is scoped to admission/observation wiring plus focused tests and
  the Task 5 report; no unrelated implementation files were modified by the
  reviewed commit.

## Verification

The exact commit was tested in a detached clean worktree:

- `mise exec java@25.0.2 -- ./gradlew :modules:ai-platform:test --tests com.emme.ai.platform.model.BoundedModelExecutionSchedulerTest --tests com.emme.ai.platform.model.ModelCapacityProfileTest --tests com.emme.ai.platform.configuration.AiProviderConfigurationIntegrationTest` — `BUILD SUCCESSFUL`.
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests com.emme.assistant.ai.configuration.SpringAiChatConfigurationTest --tests com.emme.assistant.ai.configuration.SpringAiEmbeddingConfigurationTest --tests com.emme.assistant.ai.application.provider.TracingEmbeddingModelPortTest` — `BUILD SUCCESSFUL`.
- `mise exec java@25.0.2 -- ./gradlew :modules:ai-platform:spotlessJavaCheck` — `BUILD SUCCESSFUL`.
- `git diff --check c123cfa5 d6ab7069` — clean.

The new tests pass, but they do not cover the identity-state allocation
regression described in F-01. The aggregate assistant formatting task was not
rerun independently; the report documents unrelated pre-existing worktree
violations there.

## Recommendation

**Needs revision** — address F-01 before merge, then route the updated commit
back for re-review. All High and Medium findings must be resolved before
approval.
