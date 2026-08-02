# Backend Events

> **Naming contract:** Follow the [canonical architecture naming catalog](../00-project/naming-conventions.md) for package names, filenames, Java/Kotlin types, methods, and tests. Local examples on this page must not introduce a conflicting convention.

## Purpose

Events communicate completed facts across module boundaries. Event meaning and delivery mode are separate decisions: delivery may be synchronous in-memory, asynchronous local, or durable Kafka streaming after commit. An event is not a substitute for a synchronous decision the caller needs immediately.

## Decision rule

```text
Must the caller know the result now?       → synchronous API
Is this a completed fact with independent reactions? → event
Does the consumer need independent retry?  → durable asynchronous event delivery
Is the event only an internal implementation detail? → keep it internal
```

## Internal versus public events

| Package | Audience | Contract |
|---|---|---|
| `domain.event` | The owning module only | Internal domain fact; may change with the model |
| `api.event` | Other modules | Stable public fact; joins `module :: api` and, whenever this package exists, the narrower `module :: events` named interface |
| `adapter.in.messaging.consumer` | Receiving module | Local Modulith listener or external broker consumer that delegates to a receiving use case |
| `adapter.out.messaging.*` | Broker/provider boundary | Versioned transport schema and publication mechanics |

Translate deliberately between these forms. Do not publish a domain event, aggregate, JPA entity, or broker DTO merely because its current fields resemble the public event.

```mermaid
flowchart LR
    DOMAIN[domain.event\ninternal fact] --> APP[application service]
    APP --> PUBLIC[api.event\npublic fact]
    PUBLIC --> LOCAL[adapter.in.messaging.consumer\nSpring Modulith listener]
    PUBLIC --> MAPPER[adapter.out.messaging.mapper]
    MAPPER --> BROKER[Broker schema]
```

## EMME default: Spring Modulith plus Kafka

EMME uses Spring Modulith's event publication registry as the durable publication
boundary and Kafka as the external transport. The application includes
`spring-modulith-events-kafka`, the JDBC publication registry, and Spring Kafka.
Only stable public facts under `module.api.event` are annotated with
`@Externalized`; internal domain events and local-only coordination events stay
inside the Modulith.

The annotation declares the logical Kafka topic and tenant partition key:

```java
@Externalized("emme.studio.appointment-created::#{#this.tenantId()}")
public record AppointmentCreatedEvent(/* immutable public fields */) {}
```

Spring Modulith uses the logical target before `::` as the Kafka topic and the
expression after `::` as the Kafka message key. Tenant-scoped events therefore
preserve per-tenant ordering while allowing independent partitions. Topic names,
keys, payload versions, ownership, retention, and consumers are part of the
event contract.

### Durable after-commit flow

```text
aggregate state change
        ↓ publish/register completed fact inside producer transaction
state + publication/outbox record commit atomically
        ↓
registry/dispatcher delivers after commit
        ↓
consumer handles idempotently and marks completion
```

```mermaid
sequenceDiagram
    participant U as Use case
    participant TX as Database transaction
    participant REG as JDBC publication registry
    participant K as Kafka externalizer
    participant B as Kafka broker
    participant H as Consumer handler
    participant OBS as Metrics + operations

    U->>TX: Mutate aggregate
    U->>REG: Register completed fact
    TX-->>REG: Commit state + publication record
    REG->>K: Deliver externalized event after commit
    K->>B: Publish topic + tenant key
    B-->>K: Acknowledge
    K->>REG: Mark publication complete
    B->>H: Deliver at least once
    H->>H: Deduplicate + handle
    H->>OBS: Outcome / retry / failure
```

## Event rules

- Name events in the past tense: `AppointmentScheduled`.
- Create and publish the fact only after the in-memory state transition succeeds. When durable delivery is required, publish while the producer transaction is active so state and the publication/outbox record commit atomically; execute the durable consumer after commit.
- Select and document synchronous in-memory, asynchronous best-effort, or durable asynchronous delivery independently from the event's meaning.
- Include stable identifiers, tenant identity, occurrence time, and correlation metadata as required.
- Consumers on retryable/at-least-once paths must be idempotent and tolerate duplicate delivery.
- Version payloads when compatibility requires it; do not silently change event meaning.
- Keep event handlers thin and delegate to application use cases.
- Test publication, retry, duplicate delivery, and failure observability.
- Name consumers after the received fact (`AppointmentScheduledConsumer`) and publishers after the concrete technology (`KafkaAppointmentEventPublisher`).
- Do not inject `KafkaTemplate` into domain or application services. Kafka belongs to the infrastructure/composition boundary; application services publish through `ApplicationEventPublisher` or an application-owned event port.
- Do not annotate every event automatically. `@Externalized` is an explicit public-streaming decision, not a replacement for local Modulith listeners.

Spring application-event publication is synchronous by default. `@ApplicationModuleListener` is Spring Modulith's shortcut for asynchronous transactional consumption; with the configured persistent registry, listener entries are recorded in the original business transaction and remain recoverable. Kafka externalization is the default durable transport for independently consumable facts. See the [official event reference](https://docs.spring.io/spring-modulith/reference/events.html).

## Event contract and delivery

Every published event documents:

| Field | Requirement |
|---|---|
| Event name/version | Stable past-tense name and compatibility policy |
| Event ID | Unique identifier for deduplication |
| Aggregate ID | Stable source identity |
| Tenant ID | Included when the event is tenant-scoped |
| Occurred/causation time | UTC timestamp and causal correlation |
| Schema | Immutable payload, additive evolution preferred |
| Classification | Data sensitivity and allowed destinations |
| Consumer behavior | Retry, idempotency, ordering, and failure policy |

### Publication reliability

- Publish/register after the primary in-memory state transition but before the producer transaction commits when atomic state + publication recording is required.
- Use the JDBC publication registry plus Kafka externalizer when a committed state change must not lose its event; never publish only after commit and assume registration is atomic.
- Treat asynchronous consumers as independently failing operations.
- Configure retry limits, backoff, stale-publication handling, and replay/resubmission procedures.
- Keep handlers idempotent and bounded; do not hold the producer transaction open for slow work.
- Monitor publication backlog, failure count, processing age, and terminal failures.

### Event security and governance

- Do not publish secrets, access tokens, unnecessary personal data, or persistence entities.
- Validate external event signatures, schema, tenant scope, and replay keys.
- Version incompatible changes and support old consumers during rollout.
- Record event ownership and retention requirements.

### Event checklist

- [ ] Producer and consumers have explicit contracts.
- [ ] Publication timing and durability are documented.
- [ ] Duplicate, out-of-order, retry, and poison-message behavior is tested.
- [ ] Event payloads are classified and redacted appropriately.
- [ ] Metrics, trace/causation IDs, and operational replay procedures exist.

### Kafka production checklist

- [ ] `spring.kafka.bootstrap-servers` is supplied from deployment configuration.
- [ ] Producers use `acks=all`, idempotence, bounded retries, compression, and TLS/SASL configuration where required.
- [ ] Topic creation and retention are managed outside application startup.
- [ ] Each tenant-scoped event uses a stable tenant key; consumers tolerate duplicate and out-of-order delivery.
- [ ] Publication backlog, failed publications, producer errors, consumer lag, and replay actions are observable.
- [ ] Integration tests run against a real Kafka container and verify topic, key, JSON payload, and after-commit delivery.

Use the [application template](../../templates/modulith-application-template.md) for project-wide event governance and the [module template](../../templates/module-package-structure-template.md) for module approval evidence.
