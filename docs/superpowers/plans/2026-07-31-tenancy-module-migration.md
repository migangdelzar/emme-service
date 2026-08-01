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

- [ ] Inventory all Tenancy consumers and every context entry/exit path.
- [ ] Capture baseline tests for tenant resolution, cross-tenant rejection,
  connection routing, provisioning retries, pool eviction, and rate limiting.
- [ ] Add architecture rules that forbid domain/application imports of pool,
  JPA, web, or configuration implementations.

### Task 2: Domain and persistence split

- [ ] Create framework-free Tenant and lifecycle/status models.
- [ ] Move `Tenant`, `DatabaseRegistry`, and `AuditEvent` database mappings to
  `adapter/out/persistence/entity` with explicit entities and mappers.
- [ ] Move Spring Data repositories to `adapter/out/persistence/repository` and
  application ports to `application/port/out`.
- [ ] Preserve schema, tenant status values, registry behavior, and audit fields.

### Task 3: Application/provisioning boundaries

- [ ] Move TenantService and provisioning services to `application/service`.
- [ ] Model the long-running worker as a focused process manager only if its
  current retry/lifecycle behavior requires that representation.
- [ ] Add ports for database registry, database creation, pool lifecycle, and
  event publication.
- [ ] Keep transaction boundaries and event-after-commit behavior explicit.

### Task 4: Context and web adapters

- [ ] Move TenantController and TenantProvisioningController to inbound web
  controller packages with request/response/mappers.
- [ ] Move TenantContextFilter, TenantContextAspect, TrustedTenantResolver, and
  rate-limit interception to the correct inbound filter/configuration packages.
- [ ] Do not put tenant context primitives in public API unless a consumer truly
  requires them.

### Task 5: Pool/database technical adapters

- [ ] Move `TenantRoutingDataSource` and `DatabasePoolManager` under outbound
  database/pool adapter ownership.
- [ ] Keep connection credentials and pool settings in typed configuration.
- [ ] Add integration tests for routing, pool lifecycle, eviction, and failure
  recovery; no test may disable tenant filtering to make assertions pass.

### Task 6: API metadata and verification

- [ ] Normalize API packages, event named interface, package-info files, and
  allowed dependencies.
- [ ] Run Tenancy unit/integration/architecture/Modulith/CI checks.
- [ ] Verify migration rollback, provisioning replay/idempotency, audit correlation,
  secret redaction, and cross-tenant isolation evidence.
- [ ] Update all consumers atomically and record a verification report.

## Definition of done

- [ ] Tenant isolation and database routing remain protected by executable tests.
- [ ] Domain/application code has no direct JPA/pool/web implementation dependency.
- [ ] Public APIs/events are grouped and named according to the current template.

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
  `JdbcTenantProvisioningService`.
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
