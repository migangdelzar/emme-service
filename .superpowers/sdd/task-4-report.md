# Lombok Task 4 — Immutable values, equality, builders, and immutable updates

| Field | Detail |
|---|---|
| **Date** | 2026-09-10 |
| **Branch** | `feat/ai-platform-foundation` |
| **Status** | DONE — audit-only |
| **Scope** | Whole-repository Lombok plan, Section 1 / Task 4 |

## Outcome

Task 4 completed as an audit-only slice. No production `@Value`, `@With`,
`@EqualsAndHashCode`, `@Builder`, `@Singular`, `@Builder.Default`, `@ToBuilder`,
or `@Jacksonized` annotation was safe or materially clearer for the reviewed
types. No Java record was changed and no constructor/logger work was included.

The shared Lombok policy now has focused coverage for the reviewed AI contract
and fixture boundaries. It rejects immutable/equality/copy/builder annotations
in the audited files and asserts the explicit enum and stateful construction
shapes that must remain visible.

## Audit scope and decisions

### AI contract graph types

The following remain explicit enums with domain-specific constructor fields and
accessors:

- `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/graph/GraphNodeType.java`
- `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/graph/GraphRelationshipType.java`
- `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/graph/GraphTraversalKind.java`

Lombok value/equality/copy annotations do not improve enum construction or
behavior. `GraphRelationshipType` and `GraphTraversalKind` expose explicit
allowlisted graph relationships and endpoint types; generated value semantics
would not add a safe construction API.

### AI contract graph, semantic, and RAG sources

All Java sources under these contract directories were included in the policy
audit:

- `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/graph`
- `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/semantic`
- `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/rag`

The response/value carriers are already records, with interfaces and enums
forming the remaining non-record boundary types. The inventory found no
non-record response/value object that needed Lombok. Records therefore retain
their existing immutable construction, equality, component/property names, and
serialization behavior.

### Assistant fixtures

The focused audit covers:

- `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiNailDesignExtractorTest.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ProcessDesignQuoteServiceTest.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ReviewQuoteServiceTest.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/SemanticRoutingServiceTest.java`

These files contain recording fakes and mutable observation state such as call
counters, captured requests, saved workflows, and captured semantic metrics.
Generated value/equality APIs would make test doubles compare by incidental
observation state and would not improve fixture construction. They remain
explicit.

### E2E fixtures

The focused audit covers:

- `applications/emme-platform/src/e2eTest/java/com/emme/client/UserSessionHelper.java`
- `applications/emme-platform/src/e2eTest/java/com/emme/client/UserSession.java`
- `applications/emme-platform/src/e2eTest/java/com/emme/client/SetupHelper.java`

`UserSession` has four construction paths, optional authentication behavior,
token normalization, and HTTP client/interceptor setup. A builder would obscure
those construction modes and could change authentication or client lifecycle
semantics. `UserSessionHelper` contains mutable default setup state, while
`SetupHelper` is an explicit session-bound orchestration helper. Neither is an
immutable value object and neither receives a builder or generated equality.

## TDD evidence

The accepted change is policy/audit coverage rather than a production
annotation. The focused cycle was still executed for the new policy behavior.

1. **Red:** Added `keepsTask4ImmutableBoundariesAndFixturesExplicit()` and its
   audit contract before the helper existed. The focused command failed during
   test compilation with:

   ```text
   error: cannot find symbol
   symbol:   method task4ImmutableAuditFindings(Path)
   location: class LombokUsagePolicyTest
   ```

   Command:

   ```bash
   ./gradlew :modules:shared:test \
     --tests com.emme.shared.architecture.LombokUsagePolicyTest \
     --no-parallel --no-configuration-cache
   ```

2. **Green:** Added the minimum source collector and immutable-annotation scan.
   The same focused policy command completed with `BUILD SUCCESSFUL`.

3. **Refactor:** Extracted deterministic source collection into
   `task4AuditSources(Path)` and used `LinkedHashSet` for stable audit results.
   The forced focused run completed with 10 tests, 0 skipped, 0 failures, and
   0 errors.

## Verification

| Check | Result |
|---|---|
| `./gradlew :modules:shared:test --tests com.emme.shared.architecture.LombokUsagePolicyTest --rerun-tasks --no-parallel --no-configuration-cache` | `BUILD SUCCESSFUL`; 10 tests, 0 skipped, 0 failures, 0 errors |
| `./gradlew :modules:shared:spotlessApply :modules:shared:spotlessCheck --no-parallel --no-configuration-cache` | `BUILD SUCCESSFUL` |
| `./gradlew :modules:shared:checkstyleTest :modules:shared:test --tests com.emme.shared.architecture.LombokUsagePolicyTest --no-parallel --no-configuration-cache` | `BUILD SUCCESSFUL` |
| `./gradlew :libraries:ai-contracts:compileJava :modules:shared:compileTestJava :applications:emme-platform:compileE2eTestJava --no-parallel --no-configuration-cache` | `BUILD SUCCESSFUL` |
| `git diff --check` | Passed |

The full affected compilation confirms that the graph contracts, shared policy,
and E2E fixture source set compile without production changes. No JSON
serialization check was needed for a new annotation because no builder/value
candidate was accepted; the existing records and property names remain
untouched.

## Changed files

- `modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java`
  — focused Task 4 audit and rejection contract.
- `docs/superpowers/plans/2026-09-10-lombok-whole-repository-refactor.md`
  — Task 4 checklist and implementation result.
- `tasks/todo.md` — Task 4 checklist and evidence.
- `.superpowers/sdd/task-4-report.md` — this complete report.

No files under the reviewed graph, Assistant fixture, or E2E fixture paths were
modified. The pre-existing untracked `tgrep/` directory was preserved,
untouched, and unstaged.

## Follow-up boundary

Future immutable Lombok adoption requires a separate focused contract test for
all fields, equality, immutability, copy behavior, and JSON/property names. Any
future builder must use `@Builder(setterPrefix = "with")` and must not cross
domain, JPA, tenant, security, workflow, configuration, provider, or API-record
boundaries without a new behavior-preserving review.

## Commit and remote evidence

The scoped audit, tracking updates, and this report are committed and pushed in
one logical slice. The final commit and remote-tip verification are recorded in
the handoff message after push.
