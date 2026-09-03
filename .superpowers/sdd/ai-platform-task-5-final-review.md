# Final Review: AI Platform Simplification Task 5

| Field | Value |
|---|---|
| Reviewed baseline | `23c19128` |
| Remediation commit | `fb48bbee fix(ai-platform): guard full queue before identity permits` |
| Prior reviews | `ai-platform-task-5-review.md`, `ai-platform-task-5-remediation-review.md` |
| Scope | F-01/F-02 remediation, scheduler behavior, focused Java 25 verification, and regressions |

## Findings

### F-03 — Medium — A zero-capacity queue now rejects immediate work

**Location:** `modules/ai-platform/src/main/java/com/emme/ai/platform/model/BoundedModelExecutionScheduler.java:93-95`

The new pre-acquisition guard treats `queueCapacity == 0` as full even when
`queuedCount == 0`. That changes the documented/established meaning of a zero
queue from “no waiting slots” to “no work may execute.” Immediate execution
must still be possible when capacity is available.

The regression is directly covered by the existing
`releasesAllPermitsWhenOperationFails()` test, which creates
`new ModelCapacityProfile(1, 1, 1, 1, 1, 0)` and expects the operation to run
and throw its `IllegalStateException`; `fb48bbee` instead throws
`ModelAdmissionRejectedException` before invoking the operation. The new
F-01 test has the same profile and cannot start its held request, failing its
`started.await(...)` assertion at line 80.

Preserve the pre-acquisition full-queue protection for positive queue sizes,
while allowing an immediate permit when the queue is empty and the configured
queue capacity is zero. Keep the no-waiting-slot rejection path for a zero
queue when immediate capacity is unavailable, without materializing unbounded
identity state.

## F-01/F-02 Disposition

- **F-01:** Remediated for the reported mixed-capability scenario. The new
  `doesNotMaterializeIdentityPermitsForMixedCapabilityRequestsRejectedByAFullQueue`
  test passes; full-queue rejection occurs before `computeIfAbsent`, and the
  tenant/user permit-map counts remain at their baselines.
- **F-02:** The baseline-count assertions and `finally` cleanup correctly fix
  the original assertion-before-release hang. However, the revised test still
  fails because its zero-queue fixture is rejected before the held operation
  starts. F-02 therefore cannot be considered fully green until F-03 is fixed
  or the fixture is changed to retain the intended immediate-execution
  semantics.

## Positive observations

- The queue-full check now precedes identity semaphore allocation, closing the
  high-cardinality mixed-capability rejection leak identified in F-01.
- The remediation test records permit-map baselines after the held operation
  starts and releases the latch from `finally`, so the previous hanging-test
  failure mode is removed.
- The code change is narrowly scoped to the scheduler and its focused tests;
  unrelated pre-existing worktree modifications were not touched.

## Verification

All commands below used Java 25 via `mise exec java@25.0.2`.

- Focused scheduler/profile/configuration tests:
  `:modules:ai-platform:test --tests ...BoundedModelExecutionSchedulerTest --tests ...ModelCapacityProfileTest --tests ...AiProviderConfigurationIntegrationTest`
  — **failed**: 15 tests completed, 2 failed (`releasesAllPermitsWhenOperationFails`
  and `doesNotMaterializeIdentityPermitsForRequestsRejectedByAFullQueue`).
- Full `:modules:ai-platform:test` — **failed**: 66 tests completed, 2 failed,
  the same two scheduler tests; no hang occurred.
- Mixed-capability F-01 regression alone — **passed** (`BUILD SUCCESSFUL`).
- Focused assistant observation/tracing tests — **passed** (`BUILD SUCCESSFUL`).
- `:modules:ai-platform:spotlessJavaCheck` — **failed** because the newly
  edited scheduler test has a formatting violation at the mixed-capability
  scheduler declaration (line 104 in the commit version).
- `git diff --check 23c19128 fb48bbee` — clean.

## Recommendation

**Needs revision** — F-03 is a Medium functional regression and must be
addressed before approval. The updated commit should be re-reviewed after the
zero-queue behavior is corrected and the focused ai-platform tests and
Spotless check pass.
