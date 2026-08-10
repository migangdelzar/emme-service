# MVP low-cost runtime and native-image design review

| Field | Detail |
|---|---|
| Specification | [MVP low-cost runtime and native-image design](../specs/2026-08-02-mvp-low-cost-native-image-design.md) |
| Decision record | [ADR-0006](../../adr/0006-mvp-low-cost-runtime-boundary.md) |
| Status | Accepted technical baseline; release evidence remains open |
| Date | 2026-08-03 |

## Review result

The specification is technically coherent with the current repository:

- `emme-platform` is the canonical deployable application.
- Identity, Tenancy, Customer, Catalog, local Calendar behavior, and Studio
  appointment operations form the low-cost first journey.
- Kafka, Payment, Notification, Assistant, Documents, external calendar sync,
  Kubernetes, and multi-region operations are not required startup dependencies.
- The JVM container is the reversible baseline.
- `emme.native-image` is an explicit opt-in build capability and is not applied
  by default.
- The CDD build-logic implementation is complete; the document now correctly
  points to runtime validation as the next phase.

## Evidence already available

- Repository-local architecture, Modulith, integration, boot-JAR, formatting,
  Markdown, and CI gates pass.
- PR #2 remote checks pass for the current branch commits, with deployment
  skipped because the PR is draft.
- Native capability registration and no-fallback behavior pass through TestKit.

## Open release gates

These are intentionally not marked complete without the required environment:

1. Credentialed Keycloak and external-provider execution.
2. PostgreSQL backup/restore, migration rollback, tenant-pool eviction, and
   provisioning recovery.
3. JVM container health/readiness and critical workday smoke tests.
4. GraalVM native executable/OCI build and same-limit memory comparison.
5. Kafka broker outage, replay, and deployed publication recovery.
6. Clean shutdown evidence for separately launched Spring contexts.

## Recommendation

Accepted as the technical MVP runtime boundary. Production promotion remains
blocked by the explicit release gates above; repository-local implementation
work must not be described as deployed production evidence.
