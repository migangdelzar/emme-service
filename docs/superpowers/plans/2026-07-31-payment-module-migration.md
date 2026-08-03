# Payment Module Migration Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. The current module template is authoritative; the older Modulith payment plan is historical input only.

**Goal:** Migrate Payment from mixed `entity`, `application`, `provider`, `config`,
and `web` packages to canonical DDD + Hexagonal boundaries while preserving
payment state transitions, provider selection, HTTP contracts, and tenant
ownership. Unreleased webhook behavior is normalized to the provider's current
signature and delivery-id contract rather than retained as a compatibility
surface.

**Architecture:** `Payment` and `PaymentStatus` become framework-free domain
models. Persistence is isolated behind a repository port and explicit entity,
mapper, Spring Data repository, and persistence adapter. The five payment
providers are grouped under external-system packages and implement one
application-owned `PaymentProvider` port. Mercado Pago callback handling is an
inbound webhook adapter.

## Historical pre-migration inventory

```text
com.emme.payment
├── application/PaymentService.java
├── config/PaymentProviderConfiguration.java
├── entity/{Payment,PaymentStatus,PaymentRepository}.java
├── provider/{PaymentProvider,PaymentProperties,provider implementations}.java
└── web/{PaymentController,MercadoPagoWebhookController}.java
```

There are no known consumers outside Payment; verify this before changing any
public package name. `GetPaymentUseCase` preserves the original Optional/404
behavior, while authorize/capture/refund keep typed not-found failures.

## Target ownership

```text
com.emme.payment
├── api/{command,query,result,usecase,event,exception,type}
├── application/{service,port/out,mapper}
├── domain/{model,exception}
├── adapter/in/{web/controller,web/request,web/response,web/mapper,web/advice,webhook}
├── adapter/out/persistence/{entity,repository,adapter,mapper}
├── adapter/out/provider/{stripe,paypal,mercado-pago,conekta,mock}
└── configuration/{PaymentConfiguration,PaymentProperties,PaymentProviderConfiguration}
```

Materialize `api/event` only if a Payment fact is actually published or consumed.
Do not create unused API kinds just to fill the tree.

## Normalized contracts

```text
PaymentInfo
GetPaymentUseCase
InitiatePaymentUseCase
AuthorizePaymentUseCase
CapturePaymentUseCase
RefundPaymentUseCase
PaymentProvider
PaymentResult
PaymentNotFoundException
PaymentPersistenceAdapter
PaymentEntity
SpringDataPaymentRepository
MercadoPagoWebhookController
```

Provider implementations use `<Technology>PaymentAdapter` or
`<Technology>PaymentClient` according to whether the class implements the port or
only performs transport. Provider DTOs remain inside their external-system package.

## Tasks

### Task 1: Baseline and boundary tests

- [x] Confirm no cross-module imports of `com.emme.payment`.
- [x] Add `PaymentPackageConventionTest` covering naming, package-info, domain
  isolation, application-to-adapter direction, and entity leakage.
- [x] Preserve existing `PaymentModuleTest` and `PaymentIntegrationTest` as
  behavior baselines.

### Task 2: Domain and persistence isolation

- [x] Create pure `domain/model/Payment` and `PaymentStatus` with existing
  authorize/capture/decline/refund transitions and error messages.
- [x] Create `PaymentEntity`, persistence mapper, Spring Data repository, port,
  and adapter. Preserve schema and tenant ownership.
- [x] Add pure state-transition tests and entity/domain round-trip tests.
- [x] Delete legacy `entity` classes after all references are migrated.

### Task 3: API and application orchestration

- [x] Create grouped commands, queries, results, use cases, exceptions, and types
  only for real public/payment workflow operations.
- [x] Move `PaymentService` to use-case-oriented application services or a focused
  service implementing the public interfaces.
- [x] Return `PaymentInfo`/Optional where existing endpoints require it; never
  return entities from application services.
- [x] Add application mapper and provider port contracts.

### Task 4: Provider and webhook adapters

- [x] Move `PaymentProvider` to `application/port/out` and `PaymentProperties` to
  `configuration`.
- [x] Group Stripe, PayPal, Mercado Pago, Conekta, and Mock implementations under
  `adapter/out/provider/<external-system>`.
- [x] Preserve provider request/response/error behavior and conditional activation.
- [x] Move Mercado Pago callback entry point to `adapter/in/webhook` with a thin
  controller, signature validation, mapper, and use-case invocation.
- [x] Add provider fakes and webhook regression tests.

### Task 5: Web adapters and metadata

- [x] Extract request/response records and mappers from `PaymentController`.
- [x] Move controller and advice to canonical inbound web packages.
- [x] Add package-info to each materialized package and named API interfaces.
- [x] Delete legacy packages after a complete reference scan.

### Task 6: Verification

- [x] Run Payment compilation, unit/module, integration, formatting, and
  Checkstyle gates.
- [x] Verify tenant isolation, transaction boundaries, webhook signature and
  duplicate-event handling, provider idempotency, and secret handling.
- [ ] Complete credentialed live-provider, replay/recovery, service-wide CI,
  Modulith, and boot-JAR evidence.
- [x] Update migration evidence and commit the verification report.

## Definition of done

- [x] Payment public contracts are grouped and implementation packages are private.
- [x] Payment domain is framework-free and providers are replaceable adapters.
- [x] Existing HTTP, webhook, provider, and database behavior is regression-tested.

## Completed technology-owned provider normalization — 2026-08-01

- [x] Moved Conekta, Mercado Pago, PayPal, Stripe, and Mock implementations
  under technology-owned `adapter/out/provider/<technology>` packages.
- [x] Added package metadata and updated provider configuration source-boundary
  tests.
- [x] Payment compilation, focused provider-boundary tests, and formatting pass.

Remaining evidence is live provider contract execution and final financial
webhook/replay verification.

## Completed typed provider configuration slice — 2026-08-01

- [x] Reused `PaymentProperties` as the constructor boundary for Conekta,
  Mercado Pago, PayPal, and Stripe credentials.
- [x] Bound the Mercado Pago webhook secret through typed configuration.
- [x] Removed all direct process-environment reads from Payment providers and
  the Mercado Pago webhook controller.
- [x] Preserved provider selection, error semantics, and webhook verification
  behavior while changing only configuration ownership.
- [x] Added a source-boundary regression test and verified Payment unit and
  integration tests.

The broader Payment package/domain/application migration remains tracked above;
this slice changes only configuration ownership.

## Completed canonical package-boundary slice — 2026-08-01

- [x] Added red/green package guard coverage for legacy package removal.
- [x] Moved the JPA representation to `PaymentEntity`, status vocabulary to
  `domain/model`, and Spring Data access to
  `SpringDataPaymentRepository`.
- [x] Added a framework-free Payment domain model and initial
  `InitiatePaymentUseCase` boundary.
- [x] Moved provider configuration, provider implementations, payment provider
  port, HTTP controllers, and webhook controller into canonical ownership
  packages.
- [x] Verified Payment compilation, formatting, and package guard tests.

The remaining Payment work is to add the persistence adapter/mapper, replace the
temporary multi-operation service with focused use-case services, extract web
DTOs, and complete webhook signature/replay and provider evidence.

## Completed domain/application boundary slice — 2026-08-01

- [x] Added framework-free Payment lifecycle transitions and domain tests.
- [x] Added application-owned persistence and provider ports.
- [x] Added persistence mapper and adapter so application services no longer
  depend on JPA entities or Spring Data repositories.
- [x] Replaced the multi-operation `PaymentService` with one service per public
  use case: initiate, authorize, capture, refund, get, list, and callback.
- [x] Added grouped commands, queries, results, API exceptions, HTTP DTOs, and
  web mappers.
- [x] Added idempotent initiation service coverage and re-ran Payment
  formatting, compilation, and unit/module tests.

Remaining work is limited to tenant-scoped endpoint enforcement, webhook replay
and signature evidence, provider contract tests, database integration coverage,
and service-wide verification.

## Completed production webhook boundary slice — 2026-08-01

- [x] Added an application-owned `PaymentWebhookEventRepository` port and a
  tenant/provider/event uniqueness boundary for durable delivery claims.
- [x] Added `PaymentWebhookEventEntity`, Spring Data repository, persistence
  adapter, and Liquibase change `011-payment-webhook-events.sql`.
- [x] Moved `MercadoPagoWebhookController` to `adapter/in/webhook` and added
  package metadata plus a focused signature verifier.
- [x] Implemented MercadoPago's documented `ts`/`v1` manifest verification with
  constant-time comparison; raw-body Base64 HMAC is not used.
- [x] Rejected malformed payloads, missing webhook configuration, missing
  signatures, and missing tenant context before invoking the use case.
- [x] Added unit coverage for signature verification, malformed/configuration
  failures, tenant propagation, and duplicate callback suppression.
- [x] Verified `:modules:payment:check` successfully.

Remaining Payment evidence is provider contract coverage, tenant-scoped read
coverage for every endpoint, database/integration execution against the new
webhook table, and the final service-wide verification gate.

## Completed tenant-scoped payment operation slice — 2026-08-01

- [x] Added tenant identity to payment get, authorize, capture, and refund
  commands/queries.
- [x] Removed the unscoped payment repository lookup from the application port
  and persistence adapter.
- [x] Updated payment services and controller endpoints to load through tenant
  predicates and the current tenant context.
- [x] Added source-boundary coverage and verified Payment formatting, tests, and
  module checks.

Remaining Payment evidence is provider contract coverage, database/integration
execution against the webhook table, and the final service-wide verification
gate.

## Completed provider composition-root hardening — 2026-08-01

- [x] Added a capability-owned `PaymentHttpClient` and configuration bean.
- [x] Injected HTTP and Jackson dependencies into all HTTP-backed Payment
  providers; provider classes no longer construct transport or serializers.
- [x] Added source-boundary regression coverage for the composition-root rule.
- [x] Verified Payment and Notification-focused tests after the provider boundary
  change.

Remaining Payment evidence is deterministic provider contract coverage,
webhook replay/signature coverage, and credentialed live-provider verification.

## Completed provider namespace normalization — 2026-08-02

- [x] Moved Conekta, Mercado Pago, PayPal, Stripe, and Mock payment provider
  implementations to `adapter/out/provider/{conekta,mercadopago,paypal,stripe,mock}`.
- [x] Kept `PaymentProvider` as the application-owned outbound port and retained
  provider-role class names such as `StripeProvider`.
- [x] Kept `PaymentHttpClient` as the transport-focused composition-root client;
  it is not confused with the provider strategy boundary.
- [x] Updated package metadata, provider contract tests, and source-boundary
  assertions to reject the old `adapter/out/client` provider location.
- [x] Verified Payment unit/module checks and PostgreSQL integration tests.

## Completed payment request-validation boundary slice — 2026-08-03

- [x] Added a red/green architecture regression test for validating payment
  initiation requests at the HTTP boundary.
- [x] Applied `@Valid` to `InitiatePaymentRequest` in `PaymentController`, so
  its `@NotBlank`, `@NotNull`, and `@Positive` constraints execute before the
  application use case.
- [x] Verified the focused Payment application-boundary test.

Remaining Payment evidence is deterministic provider contract depth, webhook
replay/signature execution evidence, credentialed live-provider verification,
and the final service-wide gate.

## Repository-local closure — 2026-08-03

- [x] Verified Stripe provider request/error contracts and Mercado Pago webhook
  signature, tenant, duplicate, and replay boundaries.
- [x] Verified Payment integration, service-wide CI, platform Modulith, boot JAR,
  and Markdown gates.
- [x] Preserved tenant predicates and application-owned provider/persistence
  ports throughout payment use cases.

Credentialed financial-provider execution, live webhook replay/recovery, and
backup/rollback drills remain environment-dependent release evidence. The
repository-local migration is complete; consolidated evidence is recorded in
`docs/superpowers/reviews/2026-08-03-final-service-verification.md`.
