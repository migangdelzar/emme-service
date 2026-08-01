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
- [ ] Extract framework-free Subscription domain model and status rules; move JPA
  representation to `SubscriptionEntity`.
- [ ] Split repository into application port, Spring Data repository, persistence
  adapter, mapper, and any real read projection.
- [ ] Group existing API types into result/usecase/command/query/event packages;
  preserve public names through compatibility types or an ADR where necessary.
- [ ] Move application orchestration to use-case services and controllers to
  inbound web adapters with dedicated DTOs/mappers.
- [ ] Add package-info, named interfaces, tenant/authorization architecture rules,
  and no-entity-leakage assertions.
- [ ] Run focused subscription, Studio, Modulith, service CI, and schema tests.
- [ ] Record completion in the Studio plan and registry.

## Definition of done

- [ ] Subscription capability is canonical independently of Studio core.
- [ ] Existing endpoints, status transitions, tenant restrictions, and response
  fields remain compatible.
- [ ] Any payment boundary is a named public contract, not a provider import.
