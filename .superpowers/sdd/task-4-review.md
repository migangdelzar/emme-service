# Task 4 Audit-Only Implementation Review

| Field | Detail |
|---|---|
| Base | `efbc0e31` |
| Head | `0725ec6b` |
| Scope | Read-only review of the supplied Task 4 brief, report, and complete diff |
| Verdict | Needs revision |

## Findings

1. **F-01 — Medium: the policy detector does not cover every annotation family the task claims to reject.** The new pattern includes only `Value`, `With`, `EqualsAndHashCode`, `Builder`, and `Jacksonized` (`modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java:41-43`). It has no explicit `Singular` or `ToBuilder` token, although the report says those APIs were rejected (`.superpowers/sdd/task-4-report.md:12-15`) and the plan records them as part of the audit decision (`docs/superpowers/plans/2026-09-10-lombok-whole-repository-refactor.md:421`). A future `@Singular` or `@ToBuilder` addition to an audited boundary can therefore pass the Task 4 guard.

2. **F-02 — Medium: the focused test does not prove the new detector rejects its immutable families.** `keepsTask4ImmutableBoundariesAndFixturesExplicit()` asserts that the current scan is empty and checks a few source-shape strings (`modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java:163-185`), but it supplies no synthetic `@Value`, `@With`, `@Jacksonized`, `@Singular`, or `@ToBuilder` inputs. The existing synthetic coverage exercises the older forbidden families and builder-prefix behavior (`modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java:78-136`), not these new tokens. Consequently, deleting or weakening the new regex could leave all ten tests green while the claimed rejection contract is gone.

## Clarification questions

None.

## Positive observations

- The base-to-head file set is limited to the Task 4 report, tracking plan, shared policy test, and `tasks/todo.md`; no production source appears in the supplied diff (`.superpowers/sdd/review-task-4.diff:4-9`). This supports the audit-only claim and preserves domain, JPA, tenant, security, workflow, configuration, and provider implementation boundaries.
- The reviewed graph/semantic/RAG carriers remain explicit records/enums/interfaces in the unchanged source tree; for example, `GraphProjection` is still a record (`libraries/ai-contracts/src/main/java/com/emme/ai/contracts/graph/GraphProjection.java:9`), and the graph enums retain explicit constructors and allowlisted fields (`libraries/ai-contracts/src/main/java/com/emme/ai/contracts/graph/GraphNodeType.java:4-23`; `GraphRelationshipType.java:4-27`; `GraphTraversalKind.java:4-29`).
- The report accurately records that the named E2E session/helper construction remains explicit, including overloaded `UserSession` constructors and client setup (`.superpowers/sdd/task-4-report.md:68-81`), and the policy test anchors those shapes (`modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java:177-184`).
- Tracking was updated consistently in the plan and todo ledger (`docs/superpowers/plans/2026-09-10-lombok-whole-repository-refactor.md:417-453`; `tasks/todo.md:6490-6514`), and the supplied commit range is at the claimed remote tip (`0725ec6b`).

## Recommendation

**Needs revision** — address all Medium findings by extending the detector to the complete Task 4 annotation set and adding synthetic focused cases that fail when each prohibited family is no longer detected. Route the updated audit coverage back for re-review. No broad suites were rerun for this read-only review.
