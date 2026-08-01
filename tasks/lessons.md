# Engineering lessons

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
