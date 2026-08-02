# Notification Module Migration Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. The current module template is authoritative; the older Modulith notification plan is historical input only.

**Goal:** Migrate Notification from mixed `entity`, `application`, `provider`,
`config`, `web`, and top-level `event` packages to the canonical DDD + Hexagonal
structure while preserving channel selection, provider behavior, HTTP contracts,
event consumers, and database mappings.

**Architecture:** `Notification` becomes a framework-free domain model. JPA
representation and Spring Data access move behind persistence ports/adapters.
Email, SMS, and push implementations are grouped by external capability under
`adapter/out/provider`, while `NotificationDelivered` becomes a public past-tense
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
├── adapter/out/provider/{email,sms,push}
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

- [x] Move `NotificationDeliveredEvent` to the normalized
  `api/event/NotificationDelivered.java` contract and update Studio imports
  atomically.
- [x] Move `NotificationInfo` to `api/result` and define use-case interfaces.
- [x] Replace the multi-operation orchestration with one focused application
  service per current use case.
- [x] Add outbound delivery ports and application mappers; application code
  does not import provider classes.
- [x] Add typed not-found exception without changing existing 404 behavior.

### Task 4: Normalize provider adapters and configuration

- [x] Move configuration to `configuration` and group provider implementations
  under `adapter/out/provider/{email,sms,push}`.
- [x] Keep provider selection and conditional activation unchanged.
- [x] Keep credentials in managed configuration; no provider reads secrets from
  source-controlled defaults or exposes them in logs.
- [x] Add provider fakes/contract tests and preserve current error semantics.

### Task 5: Normalize inbound web adapters and metadata

- [x] Extract request/response DTOs and mapper from the controller.
- [x] Move controller to `adapter/in/web/controller`; retain the existing global
  advice boundary because no Notification-specific translation is required.
- [x] Add `package-info.java` to every materialized package and expose API and
  event contracts through their dedicated named interfaces.
- [x] Delete legacy packages only after repository-wide reference checks.

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

- [x] No legacy Notification implementation package remains.
- [x] Domain, persistence, provider, and web boundaries are executable rules.
- [x] Studio's event consumer uses only the public event contract.
- [ ] Existing behavior and provider semantics are preserved and verified.

## Completed technology-owned client normalization — 2026-08-01

- [x] Moved email clients under `adapter/out/provider/email`.
- [x] Moved SMS clients under `adapter/out/provider/sms`.
- [x] Moved push clients under `adapter/out/provider/push`.
- [x] Added package metadata and updated configuration source-boundary tests.
- [x] Notification compilation, focused provider-boundary tests, and formatting
  pass.

Remaining evidence is provider contract execution and final service-wide
retry/idempotency verification.

## Completed typed provider configuration slice — 2026-08-01

- [x] Reused `NotificationProperties` as the constructor boundary for all
  external Notification providers.
- [x] Removed direct process-environment access from APNs, FCM, SMTP, SendGrid,
  SES, Twilio, MessageBird, and Vonage implementations.
- [x] Added typed provider settings for FCM project ID, APNs sandbox mode,
  MessageBird originator, and Vonage sender number.
- [x] Added configuration placeholders to both deployable application profiles
  without committing secret material.
- [x] Added a source-boundary regression test and verified Notification unit and
  integration tests.

The broader package/domain/application migration remains tracked above; this
slice changes only configuration ownership and preserves provider contracts.

## Completed canonical package-boundary slice — 2026-08-01

- [x] Added red/green package guard coverage for legacy package removal and
  canonical Notification locations.
- [x] Moved the JPA representation to `NotificationEntity`, Spring Data access
  to `SpringDataNotificationRepository`, and status vocabulary to the domain
  model package.
- [x] Added a framework-free Notification domain model with delivery lifecycle
  invariants.
- [x] Moved typed configuration to `configuration`, providers to
  `adapter/out/provider`, the controller to the inbound web adapter, and the
  delivered event to the public `api/event` contract.
- [x] Updated Studio's Dashboard broadcaster and event wiring tests to consume
  the public event package.
- [x] Materialized the initial `RequestNotificationUseCase` boundary and
  package metadata.
- [x] Verified Notification compilation, formatting, and package guard tests.

The remaining Notification work is to replace the temporary entity-backed
application service with focused use-case services and application-owned
delivery ports, then complete DTO mapping, provider contract evidence,
idempotency/retry evidence, and service-wide Modulith verification.

## Completed domain/application boundary slice — 2026-08-01

- [x] Added grouped Notification commands, queries, results, exceptions, and
  use-case contracts.
- [x] Added framework-free persistence mapping and an application-owned
  repository port/adapter.
- [x] Moved email, SMS, and push capability ports into the application layer.
- [x] Replaced the multi-operation `NotificationService` with focused request,
  delivery, cancellation, get, and list services.
- [x] Added an event publisher port with a Spring outbound adapter.
- [x] Extracted web request, response, and mapper types.
- [x] Verified Notification formatting, compilation, package guards, and tests.

Remaining work is retry/idempotency evidence, tenant-scoped endpoint enforcement,
provider contract coverage, and full service-wide verification.

## Completed tenant-scoped read boundary slice — 2026-08-01

- [x] Added tenant identity to `GetNotificationQuery`.
- [x] Added the application-owned tenant-scoped repository query and persistence
  adapter implementation.
- [x] Updated `NotificationController` to resolve the current tenant before
  reading a notification; cross-tenant reads now return the same not-found
  result as an absent record.
- [x] Added a red/green application-service regression test for cross-tenant
  isolation.
- [x] Verified the focused Notification test after the boundary change.

Remaining Notification evidence is durable delivery idempotency/retry policy,
provider contract coverage, and the final service-wide verification gate.

## Completed tenant-scoped mutation and delivery idempotency slice — 2026-08-01

- [x] Added tenant identity to cancellation and delivery commands.
- [x] Removed the unscoped Notification repository lookup from the application
  port and persistence adapter.
- [x] Made cancellation and delivery load through the tenant-scoped repository
  query.
- [x] Made delivery idempotent for already-delivered notifications so a replay
  cannot call an external provider twice.
- [x] Added source-boundary and service tests for tenant-scoped mutations and
  duplicate delivery handling.
- [x] Verified Notification formatting and module checks.

Remaining Notification evidence is provider contract coverage, retry policy for
transient failures, and the final service-wide verification gate.

## Completed provider composition-root hardening — 2026-08-01

- [x] Added a capability-owned `NotificationHttpClient` and configuration bean.
- [x] Injected HTTP and Jackson dependencies into all HTTP-backed Notification
  providers; provider classes no longer construct transport or serializers.
- [x] Added source-boundary regression coverage for the composition-root rule.
- [x] Verified Notification and Payment-focused tests after the provider boundary
  change.

Remaining Notification evidence is deterministic provider contract coverage,
retry/idempotency behavior, and credentialed live-provider verification.

## Completed provider namespace and public event naming slice — 2026-08-02

- [x] Renamed the public event from `NotificationDeliveredEvent` to the
  normalized past-tense `NotificationDelivered` contract.
- [x] Updated the Notification publisher port, Spring publisher adapter, and
  Studio dashboard consumer/test atomically.
- [x] Moved Notification provider implementations to
  `adapter/out/provider/{email,sms,push}`.
- [x] Moved Assistant AI provider implementations to
  `ai/adapter/out/provider/{groq,ollama,mock}` while retaining the raw WhatsApp
  transport under `adapter/out/client/whatsapp`.
- [x] Updated architecture templates to distinguish provider capability
  adapters from transport-only client packages.
- [x] Verified Notification and Assistant checks, Studio compilation, Markdown
  validation, and whitespace validation.

Remaining Notification evidence is provider contract depth, transient-failure
retry policy, credentialed live-provider verification, and the final
service-wide gate.
