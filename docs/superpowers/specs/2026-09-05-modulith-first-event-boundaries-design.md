# Design: Modulith-First Event Boundaries with Kafka Deferred

| Field | Value |
|---|---|
| Date | 2026-09-05 |
| Status | Approved |
| Scope | Emme Nails initial runtime |
| Related | `docs/adr/0005-spring-modulith-kafka-event-streaming.md`, `docs/adr/0006-mvp-low-cost-runtime-boundary.md` |

## 1. Goal

Use Spring Modulith for internal asynchronous work in the initial Emme Nails
runtime and defer Kafka until a real external consumer or independently deployed
worker exists. Kafka must not be an active application provider, startup
requirement, local dependency, or default production dependency during this
phase.

The future Kafka path remains a documented and reusable capability. Re-enabling
it will be an explicit event-boundary decision rather than an automatic effect
of publishing an internal event.

## 2. Current Context

The repository is a Spring Modulith modular monolith with one deployable
application, `applications/emme-platform`. The current code has:

- Spring Modulith core and JDBC publication registry enabled through the
  application convention plugin.
- Kafka and Spring Modulith Kafka dependencies applied through
  `emme.messaging`.
- Six event records annotated with `@Externalized`.
- No application-owned Kafka consumers (`@KafkaListener`) in the repository.
- Base and test configuration defaulting Kafka externalization off.
- Production configuration defaulting Kafka externalization on and requiring
  `KAFKA_BOOTSTRAP_SERVERS`.
- An optional Kafka Compose overlay, Kafka Testcontainers configuration, Kafka
  integration tests, and CI validation.
- A Liquibase-owned `event_publication` table for the Modulith publication
  registry.

## 3. Boundary Policy

### Internal events

Events whose consumers live inside the Emme application remain Spring Modulith
events. They are published through the existing provider-neutral ports and
Spring Modulith adapters, and consumed with `@ApplicationModuleListener` where
asynchronous transactional handling is required.

The initial internal event set includes the currently externalized events unless
an external consumer is identified before implementation begins:

- `TenantCreated`
- `TenantActivated`
- `AppointmentCreated`
- `AppointmentCancelled`
- `AppointmentRescheduled`
- `LearningCandidateEvaluationRequested`

Internal events do not use `@Externalized` and do not require Kafka.

### External events

An event may use `@Externalized` only when all of the following are true:

1. A consumer exists outside the `emme-platform` deployment boundary, or an
   independently deployed worker is explicitly approved.
2. The event has an owned topic, payload version, tenant/security
   classification, retention policy, and partition-key policy.
3. The consumer has idempotency, retry, poison-message, and replay behavior.
4. Kafka infrastructure and operational recovery evidence are available.

No event is externalized merely because it is public, asynchronous, or located
under an `api.event` package.

## 4. Runtime and Build Configuration

### Initial runtime

- Keep `spring-modulith-starter-core` and `spring-modulith-starter-jdbc`.
- Keep Liquibase ownership of the `event_publication` schema.
- Keep outstanding-publication republication enabled.
- Disable Kafka externalization in every application profile.
- Remove Kafka bootstrap-server, security, producer, and consumer-group
  requirements from active runtime configuration.
- Do not activate or wire a Kafka provider in the `emme-platform` runtime.
- Retain `emme.messaging` and its Kafka aliases as dormant build/test capability
  so the future externalization contract can be compiled and run explicitly,
  rather than deleting the reusable build logic immediately.

### Local containers and deployment

Kafka container creation is deferred. The existing Kafka Compose overlay,
Kubernetes Kafka secret references, and Kafka Testcontainers setup should remain
available as explicitly optional artifacts but must not participate in the
default local runtime, production deployment, or normal CI gate.

The Kafka Compose contract test and Kafka integration profile are therefore
commented out or gated as deferred checks, not run as part of the initial
Modulith-only validation path. The Kafka integration test may run only through
an explicit deferred-test Gradle property. They should not be silently deleted
because they document the future reactivation contract.

## 5. Expected File Impact

The implementation plan is expected to touch approximately 20–26 files:

| Area | Expected impact |
|---|---:|
| Six event records, provider cleanup, and event tests | 9–11 |
| Application build and runtime profiles | 4–5 |
| CI and Compose/Kubernetes activation gates | 3–4 |
| Canonical architecture/ADR/AI documentation | 5–6 |

The exact count depends on whether provider-only configuration is removed or
left as dormant validation code. The preferred approach removes unused active
provider configuration, keeps reusable Kafka test/build artifacts, and gates
their execution explicitly.

## 6. Testing Design

The implementation should verify:

- Internal event records have no `@Externalized` annotation.
- Internal listeners continue to receive events through Spring Modulith.
- Publication is recorded in the JDBC registry within the producer transaction.
- Listener failures remain recoverable through Modulith publication handling.
- The default application and production configuration do not require Kafka.
- Default local Compose and CI validation do not create or depend on Kafka.
- Deferred Kafka tests remain isolated and can be re-enabled when an external
  consumer is introduced.
- No domain or application service imports Kafka types.

## 7. Kafka Re-enable Criteria

Kafka may be activated for a specific event when a design review identifies a
real external boundary. The activation must include:

- An external consumer owner and deployment boundary.
- A versioned event contract and topic name.
- A documented partition key; tenant key is the default only when tenant-level
  ordering is required.
- Consumer idempotency and replay procedures.
- Broker credentials, retention, monitoring, and outage/recovery evidence.
- Kafka Testcontainers and deployed integration coverage.
- An explicit configuration change enabling Kafka only in the required
  environment.

## 8. Non-Goals

- No Kafka cluster provisioning in the initial Emme Nails runtime.
- No replacement broker.
- No direct `KafkaTemplate` usage in business modules.
- No change to PostgreSQL ownership or the Modulith publication schema.
- No redesign of event payloads beyond removing external transport metadata from
  events that are internal for this phase.

## 9. Acceptance Criteria

- The initial application starts and processes internal asynchronous events with
  PostgreSQL and Spring Modulith only.
- Kafka is not required by local, test, production, default Compose, or normal
  CI execution.
- No current internal event is annotated with `@Externalized`.
- The future Kafka capability remains discoverable and reactivatable through an
  explicit external-event decision.
- Documentation clearly explains when Kafka becomes appropriate.
