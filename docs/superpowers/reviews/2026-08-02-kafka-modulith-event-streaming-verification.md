# Kafka + Spring Modulith Event-Streaming Verification

| Field | Value |
|---|---|
| Date | 2026-08-02 |
| Scope | `emme-platform`, module public events, `emme.messaging`, production configuration |
| Status | Verified for the current unreleased MVP boundary |
| Transport | Spring Modulith JDBC publication registry + Kafka externalizer |

## Result

The deferred Kafka slice is implemented and verified. Module code publishes
public application facts without importing Kafka. Spring Modulith records the
publication in the producer transaction, and the Kafka externalizer delivers
only events explicitly annotated with `@Externalized`.

RabbitMQ/AMQP is not an application dependency, plugin, configuration, or
runtime integration. Spring Boot's dependency-verification file retains the
AMQP BOM checksums because Boot dependency management resolves that metadata
even when no AMQP library is selected; no AMQP artifact is present in a runtime
or compile dependency graph.

## Approved event catalog

| Owner | Event | Topic | Key | Delivery | Consumer boundary |
|---|---|---|---|---|---|
| `tenancy` | `TenantCreated` | `emme.tenancy.tenant-created` | `tenantId` | Durable Kafka | Identity tenant-provisioning consumer |
| `studio` | `AppointmentCreatedEvent` | `emme.studio.appointment-created` | `tenantId` | Durable Kafka | Identity, Calendar, dashboard |
| `studio` | `AppointmentCancelledEvent` | `emme.studio.appointment-cancelled` | `tenantId` | Durable Kafka | Calendar, dashboard |
| `studio` | `AppointmentRescheduledEvent` | `emme.studio.appointment-rescheduled` | `tenantId` | Durable Kafka | Calendar |
| `calendar` | `CalendarSyncRequested` | — | — | Local Modulith | Google Calendar adapter |
| `notification` | `NotificationDelivered` | — | — | Local application event | Dashboard projection |
| `studio` | `DashboardEvent` | — | — | SSE projection | Browser subscribers |

The contract test asserts that exactly the four approved public facts are
externalized, all payloads are immutable records, routing declarations remain
stable, and payload components do not expose framework, persistence, or Kafka
types.

## Configuration boundary

- `emme.messaging` owns the Spring Kafka and Spring Modulith Kafka dependencies.
- `app.messaging.kafka` is bound to typed `KafkaEventStreamingProperties`.
- Local/test profiles are disabled by default and use `localhost:9092` only as a
  development default.
- Production has no bootstrap-server fallback and requires a non-local broker
  plus encrypted transport when event streaming is enabled.
- Producer settings use `acks=all`, idempotence, bounded retries, and `zstd`
  compression.
- Topic creation and retention remain deployment-owned.

## Verification evidence

| Command | Result |
|---|---|
| `./gradlew :applications:emme-platform:test --tests com.emme.KafkaEventContractTest --tests com.emme.KafkaEventStreamingPropertiesTest` | Passed |
| `./gradlew :applications:emme-platform:integrationTest --tests com.emme.KafkaEventStreamingIntegrationTest` | Passed against a real Kafka Testcontainer |
| `./gradlew :applications:emme-platform:test --tests com.emme.ModularityTest --tests com.emme.LayerConventionTest` | Passed |
| `./gradlew ci --no-daemon --no-configuration-cache --console=plain` | Passed; shutdown warnings recorded below |
| `node scripts/validate-markdown.mjs` | Passed |

The integration test publishes all approved event types inside committed
database transactions and verifies topic, tenant partition key, and JSON
payload. The current consumers use application-owned use cases and
existing-state checks for duplicate-safe behavior: membership creation is
idempotent, Keycloak create operations treat existing resources as success, and
calendar synchronization skips an existing appointment link.

Spring Modulith is configured with
`spring.modulith.events.republish-outstanding-events-on-restart=true`, so
incomplete JDBC publications are eligible for resubmission when the application
restarts. A local Testcontainers stop/start chaos test was deliberately not
promoted to a product gate: Spring Boot's cached `@ServiceConnection` binds the
Kafka producer to the test broker lifecycle, and restarting that managed bean
inside one cached application context does not model a real application
restart. Broker-outage chaos evidence belongs in the deployment environment
where the application and broker are restarted independently.

## Known non-blocking warnings

- H2 test shutdown can emit Hibernate table/constraint-drop errors while the
  test schema is closing. The test result is successful and the production
  PostgreSQL/Liquibase schema is unaffected; this remains a cleanup-quality
  follow-up.
- Gradle/Java may report restricted native access for Zstandard and Gradle's
  native platform. These are runtime warnings, not event-contract failures.

## Operational policy

Kafka delivery is at-least-once. Publication backlog and failed publication
records are recovered through Spring Modulith's JDBC registry. A consumer must
keep side effects idempotent and treat duplicate, delayed, and replayed records
as normal inputs. Any future incompatible event change requires a new approved
contract decision; no compatibility alias is introduced for this unreleased
service.
