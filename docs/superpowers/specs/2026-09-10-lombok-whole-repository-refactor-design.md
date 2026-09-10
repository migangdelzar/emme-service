# Design: Whole-Repository Lombok Simplification

| Field | Detail |
|---|---|
| Date | 2026-09-10 |
| Repository | `emme-service` |
| Branch | `feat/ai-platform-foundation` |
| Status | Design proposed for user review |
| Baseline | `8819154b` |

## 1. Summary

Perform a repository-wide audit of Lombok opportunities across all owned Java
sources, then apply only behavior-preserving reductions that make the code
shorter and clearer. The audit includes production code, unit tests,
integration tests, test fixtures, and E2E sources under `src`; generated
`build/` output is inspected only as compiler/formatter evidence and is never
edited or committed.

The current repository already uses Lombok's compile-only convention in 13
modules. The existing constructor migration covers 106 production classes with
`@RequiredArgsConstructor`. This design extends the review to the rest of the
codebase without mechanically annotating every class.

## 2. Goals

- Reduce repetitive Java boilerplate as far as the code's semantics safely
  allow.
- Evaluate every agreed Lombok feature against real repository classes.
- Keep application, domain, contract, persistence, tenant, configuration, and
  provider boundaries understandable and behaviorally stable.
- Preserve public constructor signatures, property names, JSON names, command
  line/environment names, Spring bean wiring, JPA identity/lifecycle behavior,
  and authorization/tenant invariants.
- Keep Lombok compile-time only through the existing opt-in Gradle convention.
- Produce small, independently verifiable commits.

## 3. Non-goals and explicit exclusions

The following annotations are considered out of scope for this refactor and
must not be introduced:

- `@Data`
- `@Delegate`
- `@SneakyThrows`
- `@Synchronized`
- `@ExtensionMethod`
- generated exception annotations such as `@StandardException`

Broad generated `toString()` and setters are also not a default simplification
strategy. Domain aggregates, JPA entities, composite persistence identities,
tenant context holders, security state, workflow state, and provider clients
retain explicit behavior unless a separate design proves that generated code
preserves their invariants.

Java records remain preferred for immutable API, configuration, and contract
carriers. Lombok will not be added merely to replace an existing record.

## 4. Repository audit findings

The read-only audit covered repository-owned Java sources and excluded
`build/` trees from decisions:

- 2,465 owned Java sources were found across main, test, integration-test,
  test-fixture, and E2E source sets.
- 1,869 are production `main` sources; the remainder are test-oriented source
  sets.
- 106 production classes currently use the approved
  `@RequiredArgsConstructor` policy.
- 46 simple one-constructor classes have direct field-assignment patterns that
  require individual review.
- 50 production classes declare ordinary SLF4J logger fields through
  `LoggerFactory`.
- 34 non-domain/non-entity classes contain getter patterns worth review; most
  other getter/setter code belongs to domain models or JPA entities.
- No manual production builders were found.
- Most configuration and API data carriers are already records or have custom
  binding/default/validation behavior.
- Five test/E2E helpers have constructor assignment patterns worth a separate
  fixture review.

These counts are inventory signals, not automatic conversion targets.

## 5. Annotation policy

### 5.1 Constructors

Use `@RequiredArgsConstructor` when all required fields are final and the
existing constructor only assigns dependencies. Preserve explicit constructors
when they contain validation, defaulting, overload delegation, `super(...)`,
qualifiers that Lombok would not safely preserve, optional dependency policy,
or other initialization behavior.

Use `@NoArgsConstructor` only when a framework or test fixture genuinely
requires no-argument construction and no final/invariant field would be
silently defaulted. Do not use `force = true` for domain or persistence
objects.

Use `@AllArgsConstructor` only for simple carriers whose all-field constructor
is already the intended API. It is not a replacement for domain factories or
validated configuration constructors.

### 5.2 Accessors and field defaults

`@Getter` may replace mechanical accessors on stable, non-sensitive classes
when method names and visibility remain unchanged. `@Setter` must be scoped to
individual intentionally mutable fields; it must not bypass state transitions,
authorization, tenant context, or persistence lifecycle rules.

`@Accessors` and `@FieldDefaults` are conditional. They may be used only when
they do not change JavaBean, Spring, Jackson, or public API conventions and
when the resulting source is clearer than explicit modifiers/accessors.

### 5.3 Values and equality

Use `@Value` only for genuinely immutable, non-persistent value objects that do
not already fit naturally as records. Use `@With` only when immutable copy
updates are an established part of the type's API.

Use `@EqualsAndHashCode` only with an explicit identity decision. It is not
allowed to replace custom equality for JPA entities, composite IDs, domain
aggregates, or mutable objects without dedicated equality tests.

### 5.4 Builders

`@Builder`, `@SuperBuilder`, `@Singular`, `@Builder.Default`, `@ToBuilder`, and
`@Jacksonized` are evaluated only where a builder solves an actual construction
problem. No production manual builders currently exist, so a builder is not
introduced solely to use Lombok.

Every approved Lombok builder must declare:

```java
@Builder(setterPrefix = "with")
```

`@SuperBuilder` requires a real inheritance hierarchy whose builder API is
clearer than constructors or records. `@Jacksonized` requires an existing
Jackson deserialization need and tests proving serialized/deserialized names
are unchanged. `@Builder.Default` requires tests for both omitted and explicit
values. `@Singular` requires collection-shape tests.

Builders must not change environment-variable, property-binding, command-line,
JSON, or public API names.

### 5.5 Logging

Prefer `@Slf4j` for ordinary class-scoped SLF4J logging when it removes only
the logger field/import boilerplate. Existing logger references must be
preserved or deliberately renamed in the same focused slice. Logging must not
include secrets, tokens, tenant credentials, or sensitive payloads.

Other logger annotations are not introduced unless the project has a concrete
backend requirement that SLF4J cannot express.

### 5.6 Smaller reductions

`@FieldNameConstants` is considered only where field-name constants are
already duplicated and are part of a stable internal query/projection API.

`@NonNull` is considered only where Lombok's generated null-check timing and
exception semantics exactly match the current contract. It must not replace
domain validation or tenant/security checks.

`@Cleanup` is considered only for a resource with ordinary close semantics.
It must not replace context restoration, transaction cleanup, connection
checkout/release, or code with meaningful exception translation.

`@UtilityClass` is considered only for true stateless namespaces. It is not
used for Spring beans, classes with injectable collaborators, or classes that
may later need instance state.

## 6. Refactoring slices

Each slice is one module or one tightly bounded cross-module concern. Shared
contracts, tenant boundaries, migrations, composition roots, and overlapping
files remain sequential.

### Slice A — policy and inventory

- Extend the repository Lombok policy to represent approved annotations and
  excluded roles.
- Add source-inventory checks that ignore generated `build/` trees.
- Record candidate paths and decisions in the implementation plan.

### Slice B — safe constructor reductions

- Review the 46 direct-assignment candidates.
- Convert only plain constructors with no qualifiers, validation, defaults,
  overloads, superclass initialization, or custom lifecycle behavior.
- Include eligible controllers, listeners, publishers, adapters, and test
  helpers only after their focused module tests pass.

### Slice C — logging reductions

- Review the 50 logger declarations module by module.
- Convert ordinary SLF4J fields to `@Slf4j` where no logging behavior changes.
- Keep explicit logger construction where logger identity, custom factories,
  test injection, or security audit behavior matters.

### Slice D — immutable/value and builder review

- Review non-record value carriers, graph contracts, response types, and
  fixture objects.
- Adopt `@Value`, `@With`, controlled equality, or builders only for approved
  candidates with contract tests.
- Expect no builder changes where records or explicit factories are clearer.

### Slice E — resource and field reductions

- Review `@Cleanup`, `@FieldNameConstants`, `@FieldDefaults`, and `@NonNull`
  candidates individually.
- Preserve explicit context, tenant, transaction, security, and resource
  lifecycle code when generated behavior is less obvious.

### Slice F — test and E2E helpers

- Review fixture and E2E helper constructors and data carriers.
- Prefer Lombok when it makes test setup clearer without hiding assertions,
  user roles, tenant setup, or cleanup.
- Opt application modules into Lombok only if an accepted test/E2E candidate
  requires it.

### Slice G — boundary audit and closure

- Search for unauthorized annotations, runtime Lombok dependencies, changed
  public names, and generated code under source policy paths.
- Run affected module checks and phase-level quality gates.
- Update the Lombok plan and `tasks/todo.md` with adopted and rejected
  candidates.

## 7. Test strategy

Every behavior-affecting migration follows Red → Green → Refactor:

1. Add one focused failing source-policy or behavior test.
2. Run only that test and confirm the intended failure.
3. Apply the smallest Lombok change.
4. Run the focused test and affected compilation.
5. Refactor for clarity without changing generated behavior.
6. Run affected module checks and Spotless.

Required verification by annotation family:

| Family | Evidence |
|---|---|
| Constructors | Existing service/controller/adapter tests, compile-time wiring, and source policy |
| Getters/setters | Public method compatibility, state-transition tests, and no sensitive exposure |
| Values/equality | Equality symmetry, hash stability, immutability, and serialization tests |
| Builders | Required/optional/default fields, collection handling, JSON compatibility, and `with` method names |
| Logging | Compilation, logger behavior where tested, and source review for sensitive data |
| `@Cleanup` | Close-on-success, close-on-failure, and exception behavior |
| `@NonNull` | Null timing, exception type/message, and existing validation contract |
| `@UtilityClass` | Static API compilation and no framework-instantiation requirement |

Fast per-slice checks are focused tests, affected compilation, Spotless, diff
checks, and status. Architecture tests run for shared policy, module boundary,
dependency, contract, tenancy, migration, or composition-root changes. Full
repository checks run at phase checkpoints. Docker/Testcontainers gates remain
required when the relevant live PostgreSQL, Redis, Kafka, or provider behavior
is involved.

## 8. Build and dependency design

The existing `emme.lombok` convention remains opt-in and configures Lombok as
`compileOnly`, `annotationProcessor`, `testCompileOnly`, and
`testAnnotationProcessor`. No runtime dependency is introduced. New modules
opt in only when a source slice actually adopts Lombok.

This matches Lombok's documented Gradle setup for compile-only and annotation
processor scopes: [Lombok Gradle setup](https://projectlombok.org/setup/gradle).

## 9. Acceptance criteria

- All owned Java source sets have been inventoried and candidate decisions are
  recorded.
- Every adopted annotation has focused coverage and passes affected checks.
- Riskier annotations listed in Section 3 do not appear as annotation usages in
  production or test source; policy-test string literals that detect them are
  permitted.
- No Lombok appears in domain aggregates, JPA entities, or other explicitly
  protected boundaries without a separately approved exception.
- All builders use `setterPrefix = "with"`.
- Public constructor, property, command-line, environment, JSON, tenant, and
  provider contracts remain stable.
- Lombok remains absent from runtime classpaths.
- Each logical slice is committed with a conventional commit and pushed to
  `feat/ai-platform-foundation`.
- The final audit documents adopted, rejected, and deferred candidates.

## 10. Open design constraints

- No generated source under `build/` is edited or committed.
- No broad dependency upgrade is part of this work.
- No provider-specific names are introduced into application interfaces.
- No compatibility or deprecated class is deleted solely because Lombok was
  adopted; deletion requires an independent caller/bean/test/dependency audit.
