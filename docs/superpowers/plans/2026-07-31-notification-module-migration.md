# Notification Module Migration Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. The current module template is authoritative; the older Modulith notification plan is historical input only.

**Goal:** Migrate Notification from mixed `entity`, `application`, `provider`,
`config`, `web`, and top-level `event` packages to the canonical DDD + Hexagonal
structure while preserving channel selection, provider behavior, HTTP contracts,
event consumers, and database mappings.

**Architecture:** `Notification` becomes a framework-free domain model. JPA
representation and Spring Data access move behind persistence ports/adapters.
Email, SMS, and push implementations are grouped by external capability under
`adapter/out/client`, while `NotificationDelivered` becomes a public past-tense
event under `api/event` because Studio consumes it.

**Tech Stack:** Java 21, Spring Boot 3, Spring Modulith 2.1.0 baseline, Spring
Data JPA, OkHttp, Jakarta Mail, Jackson, JUnit 5, MockMvc, WireMock/provider fakes.

## Current inventory

```text
com.emme.notification
├── application/NotificationService.java
├── config/NotificationProperties.java
├── entity/{Notification,NotificationStatus,NotificationRepository}.java
├── event/NotificationDeliveredEvent.java
├── provider/{email,sms,push implementations}.java
└── web/NotificationController.java
```

Cross-module consumer inventory must confirm and update the known Studio
`DashboardBroadcaster` production import and its architecture test in the same
commit as the event move.

## Target ownership

```text
com.emme.notification
├── api/{result,usecase,event,exception,type}
├── application/{service,port/out,mapper}
├── domain/{model,exception}
├── adapter/in/web/{controller,request,response,mapper,advice}
├── adapter/out/persistence/{entity,repository,adapter,mapper}
├── adapter/out/client/{email,sms,push}
└── configuration/{NotificationConfiguration,NotificationProperties}
```

Do not create `api/command` or `api/query` unless a real state-changing or read
contract is required. The existing request/list/get/deliver/cancel behavior must
drive materialization. `deliver` and `cancel` are use cases even if no endpoint
currently exposes them; do not add endpoints.

## Normalized names and contracts

```text
NotificationInfo
RequestNotificationUseCase
ListNotificationsUseCase
GetNotificationUseCase
DeliverNotificationUseCase
CancelNotificationUseCase
NotificationDelivered
NotificationNotFoundException
NotificationRepository
EmailDeliveryPort / SmsDeliveryPort / PushDeliveryPort
<Technology><Channel>Adapter
SpringDataNotificationRepository
NotificationPersistenceAdapter
NotificationEntity
NotificationPersistenceMapper
NotificationWebMapper
NotificationExceptionHandler
```

Provider failures retain current semantics: SMS providers return their current
error strings, while email/push typed provider exceptions remain adapter-local.
`NotificationInfo` exposes only the existing six response fields and never body
content unless an approved contract changes that behavior.

## Tasks

### Task 1: Establish red architecture and compatibility tests

- [ ] Add `NotificationPackageConventionTest` for package ownership, API grouping,
  domain framework isolation, adapter direction, and package-info coverage.
- [ ] Add/retain MockMvc tests for request/list/get response fields and status codes.
- [ ] Add provider contract tests for success, provider failure, and current SMS
  non-throwing error behavior.
- [ ] Run the baseline notification tests and record the result before moving types.

### Task 2: Extract domain and persistence

- [ ] Create framework-free `domain/model/Notification.java` and
  `NotificationStatus.java` with existing transitions.
- [ ] Create `NotificationEntity`, Spring Data repository, persistence port,
  persistence adapter, and mapper under the canonical persistence packages.
- [ ] Preserve table/column names, enum values, timestamps, tenant filtering, and
  managed-entity update behavior.
- [ ] Add domain and mapper round-trip tests before deleting `entity/*`.

### Task 3: Normalize public contracts and application services

- [ ] Move `NotificationDeliveredEvent` to `api/event/NotificationDelivered.java`
  only after confirming all consumers; update Studio imports atomically.
- [ ] Move `NotificationInfo` to `api/result` and define use-case interfaces.
- [ ] Move orchestration to `application/service/NotificationService` or split
  services by use case if dependency count exceeds four.
- [ ] Add outbound delivery ports and application mappers; application code must
  not import provider classes.
- [ ] Add typed not-found exception without changing existing 404 behavior.

### Task 4: Normalize provider adapters and configuration

- [ ] Move configuration to `configuration` and replace generic `provider/` with
  channel-owned external-system packages.
- [ ] Keep provider selection and conditional activation unchanged.
- [ ] Keep credentials in managed configuration; no provider reads secrets from
  source-controlled defaults or exposes them in logs.
- [ ] Add provider fakes/contract tests and preserve current error semantics.

### Task 5: Normalize inbound web adapters and metadata

- [ ] Extract request/response DTOs and mapper from the controller.
- [ ] Move controller to `adapter/in/web/controller` and add advice under
  `adapter/in/web/advice`.
- [ ] Add `package-info.java` to every materialized package; expose API and events
  with `@NamedInterface("api")` and `@NamedInterface({"api", "events"})`.
- [ ] Delete legacy packages only after repository-wide reference checks.

### Task 6: Verify and document

- [ ] Run `./gradlew :modules:notification:compileJava :modules:notification:test :modules:notification:integrationTest --no-daemon --no-configuration-cache`.
- [ ] Run Studio compilation/tests and `ApplicationModules.verify()` after the
  event package move.
- [ ] Run service `ci`, formatting, Checkstyle, architecture, and boot-JAR gates.
- [ ] Record tenancy, transaction timing, provider idempotency/retry behavior,
  observability, and recovery evidence.
- [ ] Update `tasks/todo.md`, `tasks/lessons.md` when needed, and create the
  verification report before merging.

## Definition of done

- [ ] No legacy Notification implementation package remains.
- [ ] Domain, persistence, provider, and web boundaries are executable rules.
- [ ] Studio's event consumer uses only the public event contract.
- [ ] Existing behavior and provider semantics are preserved and verified.
