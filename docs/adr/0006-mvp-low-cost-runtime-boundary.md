# ADR-0006: Low-cost MVP runtime boundary

| Field | Value |
|---|---|
| Status | Accepted for the unreleased MVP baseline; release gates remain open |
| Date | 2026-08-03 |
| Owners | EMME service maintainers |
| Related | ADR-0001, ADR-0004, ADR-0005 |

## Context

EMME needs a small, inexpensive first deployment while the repository continues
to carry capabilities that are not required by the first salon workday journey.
The service must preserve DDD, Hexagonal Architecture, and Spring Modulith
boundaries without turning deferred integrations into MVP runtime dependencies.

The repository now has one canonical composition root, `emme-platform`, and the
current build-logic CDD implementation. The remaining uncertainty is operational:
credentialed identity/provider behavior, database recovery, native-image
measurements, and deployed failure recovery require a target environment.

## Decision

The MVP uses:

- one deployable backend: `emme-platform`;
- one JVM container as the production baseline and rollback artifact;
- PostgreSQL as the system of record, with Liquibase migrations;
- Keycloak and Redis as explicit runtime dependencies for authentication,
  tenant/security state, and rate limiting;
- Identity, Tenancy, Customer, Catalog, Calendar-local behavior, and Studio
  appointment operations as the first validated business path;
- Kafka externalization disabled by default in local/test profiles, while the
  Spring Modulith + Kafka capability remains available for the later streaming
  rollout;
- Payment, Notification, Assistant, Documents, external calendar
  synchronization, Kubernetes, multi-region deployment, and service extraction
  outside the MVP runtime boundary;
- `emme.native-image` as an opt-in delivery capability, adopted only after a
  GraalVM/Docker spike proves startup, health, critical-flow, migration,
  shutdown, and measured-memory acceptance criteria.

The CDD build-logic implementation is complete for the current unreleased
repository. Native-image execution and deployment validation are follow-up
environment gates, not reasons to put native wiring into every application.

## Alternatives considered

### Deploy every migrated capability in the MVP

Rejected. It increases cost and operational surface without improving the first
authenticated tenant workday journey.

### Make native image the default artifact immediately

Rejected. Native builds are architecture-specific and require environment-backed
proof for JPA, Jackson, OAuth/Keycloak, Liquibase, Kafka configuration, and
graceful shutdown. The JVM image provides a reversible baseline.

### Make Kafka mandatory for local and MVP startup

Rejected. Spring Modulith's JDBC publication registry and Kafka externalizer are
available, but local and MVP profiles must remain usable without a broker until
the deployment broker gate is accepted.

## Consequences

### Positive

- Lower initial infrastructure and operational cost.
- One composition root and one deployable artifact to monitor and roll back.
- Deferred capabilities remain structurally valid without becoming runtime
  prerequisites.
- Native optimization can be measured against a known JVM baseline.

### Negative

- The first deployment is intentionally one-replica until distributed rate-limit,
  pool, and recovery evidence is complete.
- Some repository-local checks cannot replace credentialed or deployed evidence.
- Deferred capabilities remain available in the codebase and require explicit
  review before entering the MVP runtime profile.

## Release gates

The MVP is not production-ready until the following evidence exists:

1. Credentialed Identity/Keycloak and provider contract checks.
2. PostgreSQL migration, backup/restore, tenant-isolation, and rollback evidence.
3. JVM container health/readiness and critical workday smoke tests.
4. Native executable/OCI measurements, or an explicit decision to retain JVM.
5. Kafka broker-outage/replay evidence before enabling externalization in the
   production profile.

## References

- [MVP low-cost runtime and native-image design](../superpowers/specs/2026-08-02-mvp-low-cost-native-image-design.md)
- [Final repository-local verification](../superpowers/reviews/2026-08-03-final-service-verification.md)
- [Build-logic CDD ADR](0001-build-logic-convention-plugins.md)
- [Spring Modulith + Kafka ADR](0005-spring-modulith-kafka-event-streaming.md)
