# Engineering lessons

## 2026-08-01 — Patch target validation

- Failure mode: an edit was accidentally targeted at a similarly named path
  outside the active repository.
- Detection signal: the patch result referenced a path outside the current
  workspace even though the intended repository file was unchanged.
- Prevention rule: validate every absolute `apply_patch` target against the
  active repository root before editing, then verify the target with `git
  status` immediately afterward.

## 2026-08-01 — Shared-context integration test isolation

- Failure mode: a new permission integration test reused the subject from an
  existing current-user test, leaving two active memberships in the shared
  application context.
- Detection signal: the existing current-user assertion expected one result
  but received two after the new test was added.
- Prevention rule: integration fixtures that persist state across test methods
  must use dedicated subject/resource identifiers unless the test explicitly
  owns cleanup or rollback.

## 2026-08-01 — Cross-module technical type renames

- Failure mode: renaming a persistence entity/repository left shared fixtures
  and a downstream module test importing the old technical names.
- Detection signal: targeted production compilation passed, but downstream test
  compilation failed on stale imports.
- Prevention rule: after a persistence type rename, search the entire repository
  for both the old fully-qualified type and simple type name, then compile all
  known consumers before declaring the slice complete.

## 2026-07-31 — GitHub Actions job conditions and secrets

- Failure mode: referencing a job-level `env` value in `jobs.<job>.if` caused
  GitHub Actions to reject the workflow before any job started.
- Detection signal: a workflow run completed with no jobs and no job logs.
- Prevention rule: evaluate optional secrets in an explicit first step, expose
  only a boolean through `$GITHUB_OUTPUT`, and apply `if` to later steps. Never
  expose or print the secret value itself.

## 2026-07-31 — JPA managed identity in persistence adapters

- Failure mode: mapping a domain object to a new JPA entity and passing it to
  `save()` while an entity with the same identifier was already managed caused
  `DuplicateKeyException`/multiple-object identity failures.
- Detection signal: the Calendar persistence adapter test failed when the
  application loaded an entity, changed the domain representation, and saved it
  in the same transaction.
- Prevention rule: when an identifier already exists, load and mutate the
  managed entity in place; only create a new entity for new identifiers. Keep a
  persistence adapter regression test for both paths.

## 2026-07-31 — ArchUnit guardrail checkpoints

- Failure mode: a newly added architecture rule initially failed at test
  compilation because its static rule factory import was missing.
- Detection signal: the test task failed before executing the architecture
  assertions with an unresolved `noClasses()` symbol.
- Prevention rule: compile and execute each new ArchUnit guardrail immediately
  after adding it; keep intentionally red architectural assertions local until
  the corresponding production boundary is implemented.

## 2026-07-31 — Application result models at inbound boundaries

- Failure mode: migrating an application service away from JPA entities left the
  controller without the related display names required by the existing HTTP
  response.
- Detection signal: the service compiled only after the adapter response was
  changed to an application-owned read model carrying IDs, names, and status.
- Prevention rule: when a use case needs transport-friendly denormalized data,
  assemble an application result from domain models and ports; never return a
  persistence entity merely to preserve response fields.

## 2026-07-31 — Spring transaction proxies and service naming refactors

- Failure mode: renaming a transactional service and making it `final` prevented
  Spring's configured CGLIB proxy from being created during context startup.
- Detection signal: full application-context tests failed with
  `Cannot subclass final class` even though compilation and focused source-tree
  tests passed.
- Prevention rule: after moving or renaming Spring-managed transactional classes,
  run at least one full context test; keep proxied services non-final unless the
  project explicitly uses interface-based transaction proxies.

## 2026-08-01 — Boundary validation must run before resource lookup

- Failure mode: a web test tried to prove an oversized update name using an
  unknown tenant ID, so the request reached the resource lookup and returned
  `404` before the intended validation assertion.
- Detection signal: the test failed with `Status expected:<400> but was:<404>`.
- Prevention rule: test record constraints with a validator unit test, or use a
  valid resource fixture when verifying the HTTP mapping. Do not rely on an
  unrelated not-found path to prove boundary validation.

## 2026-08-01 — Rename-aware architecture tests

- Failure mode: a package-convention test retained the old source path after a
  technical type was moved and renamed.
- Detection signal: the test failed with `NoSuchFileException` while reading the
  legacy path, even though compilation and the new package checks were correct.
- Prevention rule: update source-tree assertions in the same red-green slice as
  a rename, and run the focused convention test before the broader module gate.

## 2026-08-01 — Source-tree tests must distinguish empty legacy directories

- Failure mode: a migration removed every Java source from a legacy package, but
  the filesystem directory remained and a `Files.exists(directory)` assertion
  incorrectly reported the legacy package as present.
- Detection signal: the canonical package test failed only on the legacy-directory
  assertion after all source moves and compilation succeeded.
- Prevention rule: architecture tests should assert the absence of legacy source
  files (`hasJavaSources`) rather than the incidental existence of directories.

## 2026-08-01 — Verify BOM module availability before dependency alignment

- Failure mode: aligning the Testcontainers version to Spring Boot's resolved
  core version assumed that the PostgreSQL and JUnit integration modules existed
  at the same version.
- Detection signal: Gradle could resolve `testcontainers:2.0.5` transitively but
  could not resolve `postgresql:2.0.5` or `junit-jupiter:2.0.5`.
- Prevention rule: inspect the published module set before changing a dependency
  BOM; revert speculative alignment changes when the artifact coordinates do not
  exist and record the shared-infrastructure issue separately.

## 2026-08-01 — Spring proxyability applies to persistence adapters

- Failure mode: a Spring-managed persistence adapter declared `final` and could
  not be proxied by the configured class-based AOP infrastructure during a full
  application-context test.
- Detection signal: context startup failed with `Cannot subclass final class`
  and the failing adapter changed as each earlier final adapter was corrected.
- Prevention rule: keep Spring-managed adapters non-final when class-based
  proxies are enabled, and verify the complete context after package migrations.
