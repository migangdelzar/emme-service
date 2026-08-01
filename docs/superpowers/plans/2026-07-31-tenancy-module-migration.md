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
classes are technical adapters, not domain services.

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
