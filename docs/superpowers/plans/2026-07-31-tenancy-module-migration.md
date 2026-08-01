# Tenancy Module Migration Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. Treat tenant isolation and database provisioning as high-risk boundaries.

**Goal:** Migrate Tenancy's tenant model, provisioning workflow, database-pool
management, context infrastructure, and web endpoints to the current template
without weakening tenant isolation or changing provisioning behavior.

**Architecture:** Tenant business state and lifecycle belong in `domain/model`.
Database entities/repositories and pool implementations remain outbound adapters.
Tenant context filters/aspects are inbound infrastructure adapters. Provisioning
services are application orchestration and use outbound ports for registry,
database, and pool operations. `TenantCreated` remains a public past-tense event.

## Current inventory

```text
com.emme.tenancy
├── api/{TenantApi,TenantInfo,event/TenantCreatedEvent}
├── application/{AuditService,TenantService}
├── config/{DataSourceConfig,TenantPoolingConfig,WebMvcConfig}
├── entity/{Tenant,DatabaseRegistry,AuditEvent and repositories/services}
├── pool/{DatabasePoolManager,TenantRoutingDataSource}
├── service/{TenantApiImpl,provisioning services/worker}
├── web/{TenantController,TenantProvisioningController,rate-limit classes}
└── root context classes/{TenantContextAspect,TenantContextFilter,TrustedTenantResolver}
```

## Target ownership

```text
com.emme.tenancy
├── api/{command,query,result,usecase,event,exception,type}
├── application/{service,port/out,mapper,process}
├── domain/{model,service,event,exception}
├── adapter/in/{web/controller,web/filter,messaging/consumer}
├── adapter/out/{persistence,client/database,cache,observability}
└── configuration/{DataSourceConfiguration,TenantPoolingConfiguration,WebMvcConfiguration}
```

`application/process` is materialized only for the existing long-running
provisioning worker; it is not a generic process bucket. Database pool and routing
classes are technical adapters, not domain services. The current Tenant aggregate
uses `application/port/out` for the persistence capability and
`adapter/out/persistence/{entity,mapper,repository}` for its Spring-specific
implementation.

## Current execution status — 2026-08-01

| Area | Status | Evidence or remaining work |
|---|---|---|
| Public API and event ownership | Complete | Grouped API packages, named interfaces, past-tense `TenantCreated`, package metadata, and consumer updates |
| Tenant domain and persistence ownership | Complete | Framework-free `Tenant`, application repository port, entity/mapper/adapter, and repository tests |
| Application orchestration and inbound web adapters | Complete | Services/process manager, controllers, request/response records, filters, resolver, and web tests |
| Database registry, routing, and pool ownership | Structurally complete | `DatabaseRegistryAdapter`, `TenantDatabasePoolProvider`, and `TenantRoutingDataSource` are canonical outbound adapters |
| Typed configuration and secret boundary | Complete for database connection settings | `TenantDatabaseConnectionProperties` owns the existing `spring.datasource` credential and driver keys; pooling/rate-limit properties remain typed |
| Provisioning outbound ports | Complete for registry and schema migration | `TenantProvisioningRepository` and `TenantSchemaMigrationPort` isolate registry lifecycle and Liquibase/schema work from the process manager |
| Isolation and operational evidence | Open | Add routing/pool lifecycle/eviction/failure-recovery, replay/idempotency, rollback, and audit-correlation evidence |

## Public contract and naming decisions

- `TenantApi` remains a public use-case contract or is replaced by grouped
  `api/usecase` interfaces only with consumer updates in the same commit.
- `TenantInfo` moves to `api/result`.
- `TenantCreatedEvent` becomes `api/event/TenantCreated` if its consumers and
  event schema permit the normalized name; otherwise record a compatibility ADR.
- `Tenant`, `DatabaseRegistry`, and `AuditEvent` are not API types.
- Provisioning classes use `*ProcessManager` only for the real long-running worker;
  normal orchestration uses `<Verb><Subject>Service`.

## Tasks

### Task 1: Tenant isolation baseline

- [x] Inventory all Tenancy consumers and every context entry/exit path.
- [ ] Capture baseline tests for tenant resolution, cross-tenant rejection,
  connection routing, provisioning retries, pool eviction, and rate limiting.
- [ ] Add architecture rules that forbid domain/application imports of pool,
  JPA, web, or configuration implementations.

### Task 2: Domain and persistence split

- [x] Create framework-free Tenant and lifecycle/status models.
- [x] Move `Tenant`, `DatabaseRegistry`, and `AuditEvent` database mappings to
  `adapter/out/persistence/entity` with explicit entities and mappers.
- [x] Move Spring Data repositories to `adapter/out/persistence/repository` and
  application ports to `application/port/out`.
- [x] Preserve schema, tenant status values, registry behavior, and audit fields.

### Task 3: Application/provisioning boundaries

- [x] Move TenantService and provisioning services to `application/service`.
- [x] Model the long-running worker as a focused process manager only if its
  current retry/lifecycle behavior requires that representation.
- [x] Add explicit application ports for database creation and provisioning
  registry lifecycle; registry lookup remains represented by
  `DatabaseRegistryPort`.
- [ ] Keep transaction boundaries and event-after-commit behavior explicit for
  provisioning and audit publication.

### Task 4: Context and web adapters

- [x] Move TenantController and TenantProvisioningController to inbound web
  controller packages with request/response/mappers.
- [x] Move TenantContextFilter, TenantContextAspect, TrustedTenantResolver, and
  rate-limit interception to the correct inbound filter/configuration packages.
- [x] Do not put tenant context primitives in public API unless a consumer truly
  requires them.

### Task 5: Pool/database technical adapters

- [x] Move `TenantRoutingDataSource` and the current
  `TenantDatabasePoolProvider` implementation under outbound database adapter
  ownership; the legacy `DatabasePoolManager` name does not exist in the current
  source tree.
- [x] Keep connection credentials and pool settings in typed configuration;
  replace the remaining field-level `@Value` credentials.
- [ ] Add integration tests for routing, pool lifecycle, eviction, and failure
  recovery; no test may disable tenant filtering to make assertions pass.

### Task 6: API metadata and verification

- [x] Normalize API packages, event named interface, package-info files, and
  allowed dependencies.
- [x] Run Tenancy unit/integration checks and Studio Modulith verification.
- [x] Run Tenancy Checkstyle/Spotless; the service-wide CI gate remains part of
  the final evidence pass.
- [ ] Verify migration rollback, provisioning replay/idempotency, audit correlation,
  secret redaction, and cross-tenant isolation evidence.
- [x] Update all consumers atomically.
- [ ] Record a committed Tenancy verification report.

## Definition of done

- [ ] Tenant isolation and database routing remain protected by executable tests.
- [x] Domain/application code has no direct JPA/pool/web implementation
  dependency; provisioning database work is isolated behind application ports.
- [x] Public APIs/events are grouped and named according to the current template.

## Completed incremental slice — 2026-07-31

- [x] Grouped `TenantApi` under `api/usecase` and `TenantInfo` under
  `api/result`.
- [x] Preserved the `tenant-api` and `tenant-events` named-interface identifiers
  while moving contract ownership to grouped packages.
- [x] Renamed the public event to the normalized past-tense `TenantCreated` and
  updated its Identity consumer.
- [x] Added a source-tree convention test and verified Tenancy tests plus Studio
  Modulith verification.

The tenant isolation, domain/persistence, provisioning, pool, and web-adapter
migration remains open; this slice does not claim the full Tenancy plan is
complete.

## Completed persistence-ownership slice — 2026-07-31

- [x] Moved Tenancy JPA entities and persistence enums under
  `adapter/out/persistence/entity`.
- [x] Moved Spring Data repository interfaces under
  `adapter/out/persistence/repository`.
- [x] Moved the bootstrap registry integration under
  `adapter/out/client/database`.
- [x] Added package metadata and source-tree ownership tests for the new
  outbound persistence boundary.
- [x] Updated production, test-fixture, and consumer imports without changing
  schema mappings or tenant-routing behavior.
- [x] Verified module tests, Checkstyle, Spotless, compilation, and Studio
  Modulith verification.

## Completed Tenant aggregate boundary slice — 2026-07-31

- [x] Added framework-free `domain/model/Tenant` and `TenantStatus` with explicit
  lifecycle behavior and rehydration semantics.
- [x] Added the application-owned `application/port/out/TenantRepository`.
- [x] Renamed the Spring Data implementation to `SpringDataTenantRepository` and
  introduced `TenantEntity`, `TenantPersistenceMapper`, and
  `TenantPersistenceAdapter` under the canonical outbound packages.
- [x] Updated TenantService, TenantApiImpl, tenant resolution infrastructure,
  HTTP controllers, test fixtures, and consumers to use the domain aggregate or
  application port rather than JPA types.
- [x] Preserved the `emme_core.tenant` table mapping, status values, UUID/timestamp
  lifecycle, public HTTP response shape, and tenant lookup behavior.
- [x] Added domain and mapper tests, updated persistence/architecture tests, and
  verified Tenancy check plus Studio Modulith verification.

Tenant provisioning, database-pool/routing adapters, audit ownership, and the
remaining Identity security/domain boundary remain future slices.

## Completed application orchestration boundary slice — 2026-07-31

- [x] Moved TenantService, AuditService, and TenantApiService under
  `application/service` and removed the generic top-level application/service
  implementation namespaces.
- [x] Moved the provisioning capability and JDBC implementation under
  `application/service`, with the technology-specific implementation named
  `TenantProvisioningApplicationService`.
- [x] Renamed the scheduled long-running worker to
  `TenantProvisioningProcessManager` under `application/process`, reserving the
  process package for real long-running coordination.
- [x] Updated all production, fixture, and cross-module test consumers without
  changing HTTP endpoints, provisioning SQL, scheduling, or transaction
  behavior.
- [x] Added source-tree ownership assertions and verified the full Tenancy check
  plus Studio Modulith verification.

The next Tenancy slices are inbound web/context adapter ownership and outbound
database-pool/registry ports; this slice intentionally does not claim those
boundaries are complete.

## Completed inbound HTTP adapter slice — 2026-07-31

- [x] Moved TenantController and TenantProvisioningController under
  `adapter/in/web/controller` without changing endpoint paths, status codes, or
  accepted provisioning payload fields.
- [x] Extracted HTTP request and response records into canonical `request` and
  `response` packages and added `TenantWebMapper` for domain-to-wire mapping.
- [x] Moved tenant context resolution, trusted hostname/JWT resolution, and
  tenant rate limiting under `adapter/in/web/filter`.
- [x] Moved rate-limit properties and MVC interceptor registration into the
  canonical `configuration` package, using explicit responsibility names.
- [x] Added package metadata and source-tree ownership assertions for the
  inbound web boundary.
- [x] Verified full Tenancy tests, formatting, Checkstyle, integration-test
  compilation, and Studio Modulith verification.

`TenantContextAspect`, datasource/pool configuration, and database-pool
implementations remain outbound/configuration work for the next slice.

## Checklist reconciliation — 2026-08-01

The original checklist is now reconciled with the current source tree and
verification evidence. Completed package, domain, persistence, orchestration,
web, and contract items are marked complete above. The remaining unchecked items
are intentional implementation or evidence gaps: transaction/event boundaries,
pool/routing failure coverage, architecture rules, and the committed evidence
report.

### Reconciliation evidence

- `:modules:tenancy:test` passed.
- `:modules:tenancy:integrationTest` passed.
- `:modules:tenancy:check` passed.
- Studio Modulith verification passed.
- Integration teardown emitted existing PostgreSQL/Testcontainers shutdown
  warnings after successful test completion.

## Completed typed database configuration slice — 2026-08-01

- [x] Added `TenantDatabaseConnectionProperties` under `configuration`, bound
  to the existing `spring.datasource.username`, `spring.datasource.password`,
  and `spring.datasource.driver-class-name` keys.
- [x] Replaced field-level `@Value` injection in
  `TenantDatabasePoolProvider` with constructor-injected typed configuration.
- [x] Preserved local defaults and existing Hikari pool creation behavior.
- [x] Added source-boundary and typed-properties regression tests.
- [x] Verified Tenancy tests, Checkstyle, Spotless, integration tests, Studio
  Modulith verification, service CI, both boot JARs, Markdown validation, and
  whitespace checks.

Remaining Tenancy work includes transaction or event-after-commit boundaries,
routing/pool lifecycle and failure-recovery
evidence, replay/idempotency and rollback evidence, architecture dependency
rules, and the committed final verification report.

## Completed provisioning port boundary slice — 2026-08-01

- [x] Added `TenantProvisioningRepository` for request creation, status lookup,
  pending-request lookup and
  registry lifecycle transitions.
- [x] Added `TenantSchemaMigrationPort` for schema creation and Liquibase
  migration.
- [x] Moved JDBC registry updates into `JdbcTenantProvisioningRepository`.
- [x] Moved schema creation and Liquibase execution into
  `LiquibaseTenantSchemaMigrationAdapter`.
- [x] Kept scheduling, tenant correlation, success/failure transitions, and
  bounded error messages in `TenantProvisioningProcessManager`.
- [x] Added process-manager, source-boundary, and unsafe-schema regression
  tests.
- [x] Verified Tenancy tests/check/integration, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and whitespace checks.

Remaining Tenancy work includes explicit transaction/event-after-commit
behavior, pool lifecycle and routing failure evidence, replay/idempotency and
rollback evidence, architecture dependency rules, and the committed final
verification report.

## Completed provisioning service boundary slice — 2026-08-01

- [x] Extended `TenantProvisioningRepository` with request creation and status
  lookup capabilities.
- [x] Moved request/status SQL into `JdbcTenantProvisioningRepository`.
- [x] Removed direct `JdbcTemplate` usage from the application service.
- [x] Renamed the implementation to `TenantProvisioningApplicationService` so
  the class name reflects application ownership rather than its adapter.
- [x] Added service delegation and source-boundary regression coverage.
- [x] Reverified Tenancy tests/check/integration, Studio Modulith, service CI,
  boot JARs, Markdown validation, and whitespace checks.

Remaining Tenancy work is operational evidence, transaction/event-after-commit
decisions, architecture rules, and the committed final verification report.
