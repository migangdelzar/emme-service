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

- [ ] Enumerate every `api/event` contract in every module and classify it as
  internal-only, Modulith-only, or externally published.
- [ ] Record event owner, schema version, tenant field, topic, key, ordering
  requirement, retention expectation, and consumer list.
- [ ] Add a source-boundary test preventing adapter/domain events from being
  externalized directly.
- [ ] Confirm no RabbitMQ/AMQP dependency, configuration, or documentation
  remains in the unreleased service.

### Task 2: Verify topic, key, and serialization contracts

- [ ] Add contract tests for every approved event's topic, partition key, JSON
  field names, nullability, and timestamp representation.
- [ ] Verify tenant-scoped events partition by tenant and document exceptions.
- [ ] Verify schema evolution rules for additive fields and incompatible
  changes; record a versioning decision before changing a public event.
- [ ] Confirm external event payloads never contain persistence entities,
  framework types, secrets, or unbounded personal data.

### Task 3: Prove publication, replay, and consumer idempotency

- [ ] Test publication only after the surrounding transaction commits.
- [ ] Test publication retry after broker unavailability and recovery after an
  application restart.
- [ ] Identify every current consumer and add duplicate-delivery/replay tests
  for its application use case.
- [ ] Define the durable idempotency key and storage boundary for consumers that
  cause side effects.
- [ ] Define failure handling for poison messages, exhausted retries, and
  dead-letter or quarantine behavior; do not silently discard failures.

### Task 4: Production operations and configuration

- [ ] Bind broker settings through typed configuration properties with safe
  defaults for local development and fail-closed production validation.
- [ ] Document TLS, authentication, producer acknowledgements, idempotence,
  retry limits, timeout limits, consumer group identity, and offset policy.
- [ ] Add metrics/logging/tracing for publication state, retries, consumer
  lag, duplicate claims, and dead-letter/quarantine outcomes without logging
  payload-sensitive data.
- [ ] Document operational recovery: broker outage, publication backlog,
  consumer restart, replay, rollback, and schema incompatibility.

### Task 5: Integration and final verification

- [ ] Run the real Kafka Testcontainers suite with all approved event contracts.
- [ ] Run module tests, application integration tests, Modulith verification,
  formatting, Checkstyle, CI, and boot-JAR packaging.
- [ ] Verify the event publication Liquibase migration against a clean database,
  an upgraded database, and rollback/recovery expectations.
- [ ] Publish a verification report containing event catalog, test commands,
  broker configuration evidence, failure/replay results, and known warnings.
- [ ] Update the plan registry and mark this plan complete only after the P5
  service-wide gate accepts the evidence.

## Definition of done

- [ ] Every externalized event has an approved owner, topic, key, schema, and
  consumer contract.
- [ ] Publication, retry, restart, duplicate delivery, replay, and failure
  behavior are executable tests or documented operational evidence.
- [ ] All consumers are idempotent at their side-effect boundary.
- [ ] Production Kafka configuration and secrets handling are typed and tested.
- [ ] RabbitMQ/AMQP is absent from source, dependencies, configuration, and docs.
- [ ] The verification report is committed and pushed with the final service
  gate evidence.
