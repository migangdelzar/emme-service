# Payment Module Migration Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. The current module template is authoritative; the older Modulith payment plan is historical input only.

**Goal:** Migrate Payment from mixed `entity`, `application`, `provider`, `config`,
and `web` packages to canonical DDD + Hexagonal boundaries without changing
payment state transitions, provider selection, webhook behavior, HTTP responses,
or persistence schema.

**Architecture:** `Payment` and `PaymentStatus` become framework-free domain
models. Persistence is isolated behind a repository port and explicit entity,
mapper, Spring Data repository, and persistence adapter. The five payment
providers are grouped under external-system packages and implement one
application-owned `PaymentProvider` port. Mercado Pago callback handling is an
inbound webhook adapter.

## Current inventory

```text
com.emme.payment
├── application/PaymentService.java
├── config/PaymentProviderConfig.java
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
├── adapter/out/client/{stripe,paypal,mercado-pago,conekta,mock}
└── configuration/{PaymentConfiguration,PaymentProperties,PaymentProviderConfig}
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

- [ ] Confirm no cross-module imports of `com.emme.payment`.
- [ ] Add `PaymentPackageConventionTest` covering naming, package-info, domain
  isolation, application-to-adapter direction, and entity leakage.
- [ ] Preserve existing `PaymentModuleTest` and `PaymentIntegrationTest` as
  behavior baselines.

### Task 2: Domain and persistence isolation

- [ ] Create pure `domain/model/Payment` and `PaymentStatus` with existing
  authorize/capture/decline/refund transitions and error messages.
- [ ] Create `PaymentEntity`, persistence mapper, Spring Data repository, port,
  and adapter. Preserve schema and tenant ownership.
- [ ] Add pure state-transition tests and entity/domain round-trip tests.
- [ ] Delete legacy `entity` classes after all references are migrated.

### Task 3: API and application orchestration

- [ ] Create grouped commands, queries, results, use cases, exceptions, and types
  only for real public/payment workflow operations.
- [ ] Move `PaymentService` to use-case-oriented application services or a focused
  service implementing the public interfaces.
- [ ] Return `PaymentInfo`/Optional where existing endpoints require it; never
  return entities from application services.
- [ ] Add application mapper and provider port contracts.

### Task 4: Provider and webhook adapters

- [ ] Move `PaymentProvider` to `application/port/out` and `PaymentProperties` to
  `configuration`.
- [ ] Group Stripe, PayPal, Mercado Pago, Conekta, and Mock implementations under
  `adapter/out/client/<external-system>`.
- [ ] Preserve provider request/response/error behavior and conditional activation.
- [ ] Move Mercado Pago callback entry point to `adapter/in/webhook` with a thin
  controller, signature validation, mapper, and use-case invocation.
- [ ] Add provider fakes and webhook regression tests.

### Task 5: Web adapters and metadata

- [ ] Extract request/response records and mappers from `PaymentController`.
- [ ] Move controller and advice to canonical inbound web packages.
- [ ] Add package-info to each materialized package and named API interfaces.
- [ ] Delete legacy packages after a complete reference scan.

### Task 6: Verification

- [ ] Run `./gradlew :modules:payment:compileJava :modules:payment:test :modules:payment:integrationTest --no-daemon --no-configuration-cache`.
- [ ] Run service Modulith, architecture, formatting, Checkstyle, CI, and boot-JAR gates.
- [ ] Verify tenant isolation, transaction boundaries, webhook replay/signature
  behavior, provider retry/idempotency, secret handling, and operational logging.
- [ ] Update migration evidence and commit the verification report.

## Definition of done

- [ ] Payment public contracts are grouped and implementation packages are private.
- [ ] Payment domain is framework-free and providers are replaceable adapters.
- [ ] Existing HTTP, webhook, provider, and database behavior is regression-tested.

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
