# Studio Subscriptions Capability Migration Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. This capability is intentionally separate from the completed Studio core migration.

**Goal:** Migrate Studio Subscriptions from nested legacy `entity`, `application`,
and `web` packages to the current module template while preserving plan/status
state, tenant ownership, endpoints, and authorization behavior.

## Current inventory

```text
com.emme.studio.subscriptions
├── application/SubscriptionService.java
├── entity/{Subscription,SubscriptionStatus,SubscriptionRepository}
├── api/{SubscriptionInfo,SubscriptionApi?}
└── web/SubscriptionController.java
```

The exact API names and current consumers must be confirmed before moving files;
the target must not preserve a misleading flat `api` package.

## Target ownership

```text
com.emme.studio.subscriptions
├── api/{command,query,result,usecase,event,exception,type}
├── application/{service,port/out,mapper}
├── domain/{model,exception}
├── adapter/in/web/{controller,request,response,mapper,advice}
├── adapter/out/persistence/{entity,repository,adapter,mapper}
└── configuration
```

Materialize event or payment integration branches only when current consumers and
owned responsibilities require them.

## Tasks

- [ ] Inventory subscription endpoints, cross-module consumers, payment/identity
  dependencies, migrations, and status transition behavior.
- [ ] Add red package/layer rules and pure subscription lifecycle tests.
- [x] Extract framework-free Subscription domain model and status rules; move JPA
  representation to `SubscriptionEntity`.
- [x] Split repository into application port, Spring Data repository, persistence
  adapter, mapper, and any real read projection.
- [x] Group existing API types into result/usecase/command/query/type packages;
  preserve the stable PlanType vocabulary under `api/type`.
- [x] Move application orchestration to one focused use-case service per operation
  and controllers to
  inbound web adapters with dedicated DTOs/mappers.
- [x] Add package-info, named interfaces, and no-entity-leakage package rules.
- [x] Run focused subscription and Studio compile/test verification.
- [x] Record the completed boundary slice in the execution tracker.

## Definition of done

- [x] Subscription capability is canonical independently of Studio core.
- [x] Existing endpoints, status transitions, tenant restrictions, and response
  fields remain compatible.
- [x] Payment remains outside the subscription capability; no provider import was
  introduced.

## Completed canonical boundary slice — 2026-08-01

- [x] Added framework-free `Subscription` and `SubscriptionStatus` models with
  plan-change and entitlement behavior.
- [x] Renamed JPA persistence ownership to `SubscriptionEntity`,
  `SpringDataSubscriptionRepository`, mapper, and adapter.
- [x] Grouped public commands, queries, results, types, and use-case ports.
- [x] Replaced the multi-operation `SubscriptionService` with one application
  service per use case.
- [x] Moved HTTP requests, responses, mapping, and controller into inbound
  adapter packages.
- [x] Updated Studio, Identity, and test fixtures to consume canonical contracts
  without legacy package imports.
- [x] Exposed only `subscriptions-api` through Spring Modulith metadata.
- [x] Verified focused subscription/Documents tests, compilation, formatting,
  and test-fixture compilation.

The remaining global work is full service-wide Modulith, integration, schema,
security, and recovery evidence.

## Completed tenant-scoped inbound boundary slice — 2026-08-01

- [x] Added a red source-boundary test requiring current tenant resolution in
  the Subscription controller.
- [x] Wrapped create, get, entitlement, and plan-change operations in the
  tenant context boundary.
- [x] Rejected mismatched path/body tenant identifiers with the same not-found
  response used for inaccessible records.

Remaining Subscription work is payment-boundary documentation and final
service-wide integration, Modulith, schema, security, and recovery evidence.

## Completed tenant-safe persistence verification slice — 2026-08-02

- [x] Added `findByTenantIdAndId(UUID tenantId, UUID subscriptionId)` to the
  Spring Data persistence boundary.
- [x] Changed existing-subscription saves to resolve rows through the tenant
  predicate instead of `repository.findById(...)`.
- [x] Added a source-boundary regression test preventing unscoped subscription
  persistence access.
- [x] Compared subscription entity/status/plan fields with the canonical
  Studio schema and preserved the existing unique tenant ownership constraint.

The Subscriptions capability remains structurally canonical. Remaining work is
the repository-wide payment boundary, Modulith, security, recovery, and boot
artifact evidence.
