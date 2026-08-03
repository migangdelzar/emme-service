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
├── api/{command,query,result,usecase,event}
├── application/{mapper,service,port/out,process}
├── config/{DataSourceConfig,TenantPoolingConfig,WebMvcConfig}
├── entity/{Tenant,DatabaseRegistry,AuditEvent and repositories/services}
├── pool/{DatabasePoolManager,TenantRoutingDataSource}
├── service/{focused tenant and provisioning use cases}
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
| Isolation and operational evidence | Open | Structural tests pass; add live routing/pool lifecycle/eviction/failure-recovery, replay/idempotency, rollback, and audit-correlation evidence |

## Completed managed bootstrap JDBC boundary — 2026-08-03

- [x] Routed `DatabaseRegistryAdapter` tenant lookups through the generic,
  throwable-aware `JdbcConnectionExecutor.withConnection` callback.
- [x] Kept datasource construction and connection acquisition in the
  composition-root `BootstrapJdbcConfiguration`, using a dedicated unpooled
  bootstrap datasource so tenant routing cannot participate in registry
  initialization.
- [x] Extended typed `TenantDatabaseConnectionProperties` with the bootstrap
  URL used by the default registry entry.
- [x] Kept the registry application port available in H2 and lightweight test
  contexts; production-only bootstrap executor beans are conditional on a
  non-H2 JDBC URL.
- [x] Added unit coverage proving callback-based registry access and typed
  configuration binding while preserving default-database behavior.

The remaining Tenancy work is operational evidence: live pool eviction and
recovery, provisioning replay/idempotency and rollback, audit correlation,
secret redaction, and service-wide verification.

## Public contract and naming decisions

- The legacy multi-operation `TenantApi` is replaced by focused grouped
  `api/usecase` interfaces; consumers are updated in the same commit.
- `TenantInfo` moves to `api/result`.
- `TenantCreatedEvent` becomes `api/event/TenantCreated` if its consumers and
  event schema permit the normalized name; otherwise record a compatibility ADR.
- `Tenant`, `DatabaseRegistry`, and `AuditEvent` are not API types.
- Provisioning classes use `*ProcessManager` only for the real long-running worker;
  normal orchestration uses `<Verb><Subject>Service`.

## Tasks

### Task 1: Tenant isolation baseline

- [x] Inventory all Tenancy consumers and every context entry/exit path.
- [x] Capture focused tests for tenant resolution, cross-tenant rejection,
  connection routing, provisioning retries, pool recovery, and rate limiting;
  live eviction evidence remains open.
- [x] Add architecture/source rules that forbid domain/application imports of
  pool, JPA, web, or configuration implementations.

### Task 2: Domain and persistence split

- [x] Create framework-free Tenant and lifecycle/status models.
- [x] Move `Tenant`, `DatabaseRegistry`, and `AuditEvent` database mappings to
  `adapter/out/persistence/entity` with explicit entities and mappers.
- [x] Move Spring Data repositories to `adapter/out/persistence/repository` and
  application ports to `application/port/out`.
- [x] Preserve schema, tenant status values, registry behavior, and audit fields.

### Task 3: Application/provisioning boundaries

- [x] Split tenant lifecycle, tenant reads, realm updates, and provisioning
  operations into focused `application/service` implementations.
- [x] Model the long-running worker as a focused process manager only if its
  current retry/lifecycle behavior requires that representation.
- [x] Add explicit application ports for database creation and provisioning
  registry lifecycle; registry lookup remains represented by
  `DatabaseRegistryPort`.
- [x] Keep transaction boundaries and event-after-commit behavior explicit for
  tenant creation and audit publication; live replay evidence remains open.

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
- [x] Add deterministic integration/unit coverage for routing, pool lifecycle,
  default-pool recovery, and failure behavior; live eviction/recovery remains
  open and no test disables tenant filtering.

### Task 6: API metadata and verification

- [x] Normalize API packages, event named interface, package-info files, and
  allowed dependencies.
- [x] Run Tenancy unit/integration checks and Studio Modulith verification.
- [x] Run Tenancy Checkstyle/Spotless; the service-wide CI gate remains part of
  the final evidence pass.
- [ ] Verify live migration rollback, provisioning replay/idempotency, audit
  correlation, secret redaction, and deployment-level cross-tenant isolation.
- [x] Update all consumers atomically.
- [x] Record the committed Tenancy verification report.

## Definition of done

- [ ] Tenant isolation and database routing remain protected by executable tests.
- [x] Domain/application code has no direct JPA/pool/web implementation
  dependency; provisioning database work is isolated behind application ports.
- [x] Public APIs/events are grouped and named according to the current template.

## Completed incremental slice — 2026-07-31

- [x] Grouped tenant commands, queries, results, and focused use cases under
  `api`, with `TenantInfo` under `api/result`.
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
- [x] Updated focused tenant use-case services, tenant resolution infrastructure,
  HTTP controllers, test fixtures, and consumers to use the domain aggregate or
  application port rather than JPA types.
- [x] Preserved the `emme_core.tenant` table mapping, status values, UUID/timestamp
  lifecycle, public HTTP response shape, and tenant lookup behavior.
- [x] Added domain and mapper tests, updated persistence/architecture tests, and
  verified Tenancy check plus Studio Modulith verification.

Tenant provisioning, database-pool/routing adapters, audit ownership, and the
remaining Identity security/domain boundary remain future slices.

## Completed application orchestration boundary slice — 2026-07-31

- [x] Moved tenant orchestration, audit, and tenant lookup capabilities under
  `application/service` and removed generic top-level implementation
  namespaces.
- [x] Split provisioning request and status into focused application services:
  `RequestTenantProvisioningService` and
  `GetTenantProvisioningStatusService`.
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

## Completed e2e tenant CRUD contract slice — 2026-08-02

- [x] Aligned the e2e tenant update client with the server's `PATCH`
  `/api/tenants/{id}` contract.
- [x] Added a first-class `UserSession.patch` helper.
- [x] Removed the old `405` compatibility escape from the tenant CRUD test.
- [x] Verified `:applications:emme-platform:compileE2eTestJava`.

### Reconciliation evidence

## Completed database identifier boundary slice — 2026-08-02

- [x] Centralized tenant schema-name validation in the outbound database
  adapter boundary.
- [x] Applied the same validation before the PostgreSQL RLS/search-path aspect
  uses a registry-provided schema identifier.
- [x] Moved `TenantContextAspect` beside the routing, pool, and Liquibase
  database adapters; it is not persistence-domain behavior.
- [x] Added regression coverage for accepted identifiers, SQL fragments, the
  Liquibase adapter, and canonical package ownership.
- [x] Verified the focused Tenancy tests and formatting.

Remaining Tenancy work is operational pool/routing/recovery evidence,
provisioning replay and rollback evidence, transaction/after-commit evidence,
and the committed final verification report.

## Completed scheduler resilience slice — 2026-08-02

- [x] Added a regression test for a provisioning-registry lookup failure.
- [x] Made `TenantProvisioningProcessManager` log a bounded diagnostic and
  return when pending-work loading fails, allowing the next scheduled poll to
  retry safely.
- [x] Reverified the focused process-manager tests and the Studio module checks.

The remaining operational evidence is limited to live pool/routing recovery,
provisioning rollback/replay under a real deployment, audit correlation, and
the final service-wide verification report.

## Completed managed JDBC connection boundary slice — 2026-08-02

- [x] Reused Shared's capability-owned `JdbcConnectionExecutor` with generic
  `ThrowingSqlConnectionFunction<R, E extends Throwable>` and
  `ThrowingSqlConnectionConsumer<E extends Throwable>` forms.
- [x] Kept result-producing work on `withConnection` and side-effecting work on
  `consumeWithConnection` so callback overloads remain unambiguous.
- [x] Delegated connection lifecycle to Spring `JdbcTemplate.execute` rather
  than manually calling `DataSource#getConnection()` in production adapters.
- [x] Migrated tenant Liquibase schema creation and migration to the managed
  connection boundary.
- [x] Added Shared unit coverage for result-returning, side-effect, checked
  failure, fatal error, interruption, and null callback behavior, plus Tenancy
  adapter delegation coverage.
- [x] Verified Tenancy tests, Checkstyle, Spotless, integration tests,
  Markdown validation, and whitespace validation.

Remaining Tenancy work is operational pool/routing/recovery evidence,
provisioning replay and rollback evidence, transaction/after-commit evidence,
and the committed final verification report.

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
- [x] Named provisioning implementations after their individual use cases
  rather than retaining a multi-operation provisioning service.
- [x] Added service delegation and source-boundary regression coverage.
- [x] Reverified Tenancy tests/check/integration, Studio Modulith, service CI,
  boot JARs, Markdown validation, and whitespace checks.

Remaining Tenancy work is operational evidence, transaction/event-after-commit
decisions, architecture rules, and the committed final verification report.

## Completed pool failure baseline slice — 2026-08-01

- [x] Added deterministic coverage for unresolved database registry lookup.
- [x] Added empty pool lifecycle and shutdown coverage.
- [x] Preserved the remaining integration-level routing, eviction, and recovery
  scenarios as open evidence work rather than treating unit coverage as a
  substitute.

## Completed deterministic routing boundary slice — 2026-08-02

- [x] Added regression coverage for default-database fallback when no database
  context is present.
- [x] Added regression coverage proving a resolved tenant database ID is used as
  the routing lookup key.
- [x] Added regression coverage proving target DataSource resolution is delegated
  lazily to `TenantDatabasePoolProvider`.
- [x] Verified the routing tests together with default-pool lifecycle and
  recovery tests.

Live pool eviction, routing-failure recovery, and deployment-level rollback
evidence remain open; these tests document the deterministic unit boundary but
do not substitute for the final operational evidence gate.

## Completed event-after-commit boundary slice — 2026-08-01

- [x] Kept `CreateTenantService` event publication inside its transaction.
- [x] Changed the Identity `TenantCreated` consumer to
  `@ApplicationModuleListener`, so cross-module realm provisioning is handled
  through the Spring Modulith after-commit event boundary.
- [x] Preserved the `TenantCreated` event schema and Identity realm-provisioning
  delegation.
- [x] Added delegation and source-boundary regression coverage.
- [x] Verified Identity/Tenancy tests, Modulith verification, service CI, both
  boot JARs, Markdown validation, and whitespace checks.

Remaining Tenancy work is live routing/eviction/recovery evidence, transaction
boundary evidence for provisioning, architecture rules, and the committed
verification report.

## Completed default-pool recovery slice — 2026-08-01

- [x] Added a regression test proving that an externally closed default pool is
  replaced on the next lookup.
- [x] Changed the default-pool compare-and-set operation to replace the exact
  stale reference, preserving safe concurrent initialization.
- [x] Preserved the non-evictable default-pool behavior and safe shutdown.
- [x] Verified Tenancy tests/check/integration, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and whitespace checks.

Remaining Tenancy work is live tenant-pool eviction and routing/recovery
evidence, transaction boundary evidence for provisioning, architecture rules,
and the committed verification report.

## Completed focused tenant use-case slice — 2026-08-01

- [x] Replaced the multi-operation `TenantService` façade with one application
  service per tenant lifecycle or read use case.
- [x] Replaced the legacy `TenantApi` contract with focused public use-case
  ports for tenant reads, slug resolution, and identity-realm updates.
- [x] Split provisioning request and provisioning-status operations into
  separate use-case contracts and services.
- [x] Updated inbound web adapters, Identity consumers, and test fixtures to
  depend on canonical contracts.
- [x] Added grouped commands, queries, result metadata, and focused boundary
  coverage while preserving existing HTTP and provisioning behavior.
- [x] Verified Tenancy and Identity unit/integration tests after the split.

## Repository-local closure — 2026-08-03

- [x] Verified the complete Tenancy integration test task in the service-wide
  integration matrix.
- [x] Verified managed bootstrap JDBC callbacks through the qualified generic
  executor and preserved H2 contexts without bootstrap infrastructure.
- [x] Verified platform Modulith, layer, application-parity, CI, boot JAR, and
  Markdown gates.

Live pool eviction, database-outage routing recovery, provisioning replay and
rollback, and audit-correlation drills remain environment-dependent release
evidence. The repository-local migration is complete; the consolidated evidence
is recorded in `docs/superpowers/reviews/2026-08-03-final-service-verification.md`.
