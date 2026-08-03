# Kafka + Spring Modulith Event-Streaming Closure Plan

> **For agentic workers:** Use `superpowers:executing-plans` or
> `superpowers:subagent-driven-development` to execute this plan task by task.
> The Kafka implementation slice already exists; this plan closes its
> production contract, recovery, and verification evidence.

**Goal:** Complete the production-grade Kafka and Spring Modulith event-streaming
boundary without reintroducing RabbitMQ/AMQP, while preserving module-owned public
event contracts and tenant-aware partitioning.

**Architecture:** Domain/application code emits module-owned public events. Spring
Modulith records publication state through the JDBC publication registry. The
Kafka externalizer transports only explicitly approved public events. Consumers
remain idempotent application entry points and do not depend on broker-specific
implementation details.

**Related architecture:**
[`module-communication.md`](../../architecture/03-integration/module-communication.md)
and [`build-logic.md`](../../architecture/00-project/build-logic.md).

**Related decision:** [ADR 0005 — Spring Modulith Kafka event
streaming](../../adr/0005-spring-modulith-kafka-event-streaming.md) selects Kafka
as the transport; RabbitMQ/AMQP is not a supported target for this unreleased
system.

## Scope and invariants

- Public events use stable past-tense names and immutable contract payloads.
- Only explicitly approved events are externalized.
- Topic names and partition keys are deterministic and documented.
- Tenant identity is present in every tenant-scoped event and is used as the
  partition key where ordering within a tenant matters.
- Publication is transactionally recorded and retriable.
- Consumers are safe under duplicate delivery and replay.
- Invalid, malformed, or unauthorized event payloads fail closed.
- Broker credentials and connection details are typed configuration, never
  hardcoded or logged.
- Local unit tests remain Kafka-independent; dedicated integration tests use a
  real Kafka broker through Testcontainers.

## Current implementation baseline

Already implemented and not to be recreated:

- `emme.messaging` composes Spring Kafka and Spring Modulith Kafka support.
- The deployable application uses the JDBC publication registry.
- The `event_publication` schema is owned by Liquibase.
- Approved tenant and appointment events use explicit externalization metadata.
- Production producer settings include acknowledgements, idempotence, bounded
  retries, and compression.
- A Kafka Testcontainers integration test proves committed publication, topic,
  key, and JSON payload behavior.

## Execution tasks

### Task 1: Inventory public event contracts

- [x] Enumerate every `api/event` contract in every module and classify it as
  internal-only, Modulith-only, or externally published.
- [x] Record event owner, schema version, tenant field, topic, key, ordering
  requirement, retention expectation, and consumer list.
- [x] Add a source-boundary test preventing adapter/domain events from being
  externalized directly.
- [x] Confirm no RabbitMQ/AMQP library, plugin, configuration, or runtime usage
  remains in the unreleased service. The ADR retains the rejected-transport
  decision and Gradle retains Boot's required AMQP BOM verification metadata.

### Task 2: Verify topic, key, and serialization contracts

- [x] Add contract tests for every approved event's topic, partition key, JSON
  field names, nullability, and timestamp representation.
- [x] Verify tenant-scoped events partition by tenant and document exceptions.
- [x] Verify schema evolution rules for additive fields and incompatible
  changes; record a versioning decision before changing a public event.
- [x] Confirm external event payloads never contain persistence entities,
  framework types, secrets, or unbounded personal data.

### Task 3: Prove publication, replay, and consumer idempotency

- [x] Test publication only after the surrounding transaction commits.
- [x] Configure and document publication retry/recovery at the application
  restart boundary with
  `spring.modulith.events.republish-outstanding-events-on-restart=true`.
  Broker-outage chaos testing is deployment-environment evidence, not a
  same-context Testcontainers stop/start test, because the cached
  `@ServiceConnection` producer does not represent an application restart.
- [x] Identify every current consumer and add duplicate-delivery/replay tests
  for its application use case.
- [x] Define the durable idempotency key and storage boundary for consumers that
  cause side effects.
- [x] Define failure handling for poison messages, exhausted retries, and
  failed-publication quarantine behavior through the JDBC publication registry;
  do not silently discard failures.

### Task 4: Production operations and configuration

- [x] Bind broker settings through typed configuration properties with safe
  defaults for local development and fail-closed production validation.
- [x] Document TLS, authentication, producer acknowledgements, idempotence,
  retry limits, timeout limits, consumer group identity, and offset policy.
- [x] Add metrics/logging/tracing for publication state, retries, consumer
  lag, duplicate claims, and dead-letter/quarantine outcomes without logging
  payload-sensitive data.
- [x] Document operational recovery: broker outage, publication backlog,
  consumer restart, replay, rollback, and schema incompatibility.

### Task 5: Integration and final verification

- [x] Run the real Kafka Testcontainers suite with all approved event contracts.
- [x] Run module tests, application integration tests, Modulith verification,
  formatting, Checkstyle, CI, and boot-JAR packaging.
- [x] Verify the event publication Liquibase migration against a clean database,
  an upgraded database, and rollback/recovery expectations.
- [x] Publish a verification report containing event catalog, test commands,
  broker configuration evidence, failure/replay results, and known warnings.
- [x] Update the plan registry and mark this plan complete only after the P5
  service-wide gate accepts the evidence.

## Definition of done

- [x] Every externalized event has an approved owner, topic, key, schema, and
  consumer contract.
- [x] Publication, retry, restart, duplicate delivery, replay, and failure
  behavior are executable tests or documented operational evidence. Application
  restart recovery is configured; broker-outage chaos remains a deployment
  acceptance test.
- [x] All current consumers are idempotent at their side-effect boundary.
- [x] Production Kafka configuration and secrets handling are typed and tested.
- [x] RabbitMQ/AMQP is absent from source, runtime dependencies, and configuration; the rejected-transport decision remains documented.
- [x] The verification report is committed and pushed with the final service
  gate evidence.
