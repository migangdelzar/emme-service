# Re-review: AI Platform Simplification Task 5 Remediation

| Field | Value |
|---|---|
| Reviewed range | `d6ab7069..23c19128` |
| Remediation commit | `23c19128 fix(ai-platform): lazily allocate identity permits` |
| Baseline review | [`ai-platform-task-5-review.md`](ai-platform-task-5-review.md) |
| Scope | F-01 remediation, scheduler regressions, and verification evidence |

## Findings

### F-01 — Medium — Identity state can still be materialized by full-queue rejections

**Location:** `modules/ai-platform/src/main/java/com/emme/ai/platform/model/BoundedModelExecutionScheduler.java:163-178`

The new short-circuit order correctly avoids creating tenant/user semaphores
when global or capability capacity is unavailable. However, `awaitPermit()`
still calls `tryAcquire()` before checking whether the queue is full. If a
request uses a capability with available capacity while another capability has
already filled the queue, this request acquires global and capability permits,
then `computeIfAbsent` creates both identity semaphores. Those permits are
released immediately and the request is rejected at lines 97-101, but the map
entries remain forever.

For example, with global capacity 2, generation capacity 1, embedding capacity
2, and queue capacity 1: hold one generation execution, enqueue a second
generation request, then submit distinct embedding contexts. Each embedding
request can materialize new tenant/user entries before being rejected by the
full queue. Thus the original unbounded high-cardinality rejection risk remains
for mixed-capability traffic; the remediation only covers the narrower case
where global or the requested capability is already exhausted.

The admission path should check the bounded-queue condition before any
identity-state allocation when a queued waiter already exists, or otherwise
remove/evict identity state created by a request that is ultimately rejected.
Add a regression test for the mixed-capability scenario in addition to the
global-capacity-held case.

### F-02 — Medium — The added regression test fails and can hang the test task

**Location:** `modules/ai-platform/src/test/java/com/emme/ai/platform/model/BoundedModelExecutionSchedulerTest.java:70-107`

The test starts a running request using a random tenant and principal at lines
80-88. That request necessarily materializes one tenant permit and one user
permit. The assertions at lines 102-103 nevertheless require both counts to be
zero, so the test fails before reaching `release.countDown()` at line 105.
Because the test uses try-with-resources around a virtual-thread executor, the
executor close waits for the still-blocked first request, causing the test task
to hang instead of reporting the assertion promptly.

Capture the permit counts after the first request starts and assert that the
counts are unchanged after the rejected requests (or use a stable context and
assert the expected active-state baseline). Ensure cleanup is in a `finally`
block so a failed assertion cannot strand the latch-held task.

## Clarification questions

None.

## Positive observations

- `tryAcquire()` now acquires global and capability permits before invoking
  `computeIfAbsent` for tenant and user identities, which is the correct
  direction for lazy allocation.
- The package-visible permit counters make the intended state-boundary
  invariant directly testable.
- The change is narrowly scoped to the scheduler and its focused test; no
  unrelated production files were modified in the reviewed range.
- `git diff --check d6ab7069 23c19128` is clean.

## Verification

- Reviewed both requested documents and the complete `d6ab7069..23c19128`
  diff.
- The clean detached worktree at `23c19128` compiled the targeted
  `:modules:ai-platform:test` inputs successfully through `:modules:ai-platform:test`.
- The test execution did not complete: a thread dump showed the test worker
  waiting in `ThreadPerTaskExecutor.close()` from the new test after its
  zero-count assertion aborted before releasing the first operation. The run
  was stopped to avoid leaving the blocked executor alive.
- No approval claim is made for the full suite or formatting checks because
  the targeted test task did not complete.

## Recommendation

**Needs revision** — address F-01 and F-02, then route the updated remediation
back for re-review. Approval is not appropriate while either Medium finding
remains.
