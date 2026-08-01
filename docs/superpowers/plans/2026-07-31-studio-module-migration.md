# Studio Module Migration Plan

> Execute with `superpowers:executing-plans`. Each implementation task follows
> Red → Green → Refactor and ends with a verification checkpoint.

| Field | Value |
|---|---|
| Design | [`2026-07-31-studio-module-migration-design.md`](../specs/2026-07-31-studio-module-migration-design.md) |
| Module | `modules/studio` |
| Branch | `feat/studio-module-migration` |
| Status | In progress — public contracts normalized; core migration continuing |
| Date | 2026-07-31 |

## Scope

Migrate the core Studio capability. `documents` and `subscriptions` remain
explicitly deferred nested capabilities until the core module boundary is
verified.

## Tasks

### 1. Add architecture guardrails

- ✅ Commit `StudioPackageConventionTest` after the core root package move.
- ✅ Reject root production ownership in `entity`, `event`, and `web`.
- ✅ Require grouped public API types.
- Tighten the guardrail to forbid application services from importing
  persistence adapters after application-owned ports are introduced.

### 2. Normalize public contracts

- ✅ Move `api/AppointmentInfo`, `BusinessProfileInfo`, and `CustomerInfo` to
  `api/result`.
- ✅ Move `api/SalonApi` to `api/usecase`.
- ✅ Keep events under `api/event` and move `DashboardEvent` there because it is a
  cross-module contract.
- ✅ Add package metadata and update all consumers, including Calendar and
  Identity imports.

### 3. Extract core domain models

- ✅ Move framework-independent status and policy enums to `domain/model`.
- ✅ Extract the Customer domain model with persistence-independent state and
  lifecycle behavior.
- ✅ Extract the Service Catalog domain model with persistence-independent
  state and retirement behavior.
- Create framework-independent domain models for the core Studio aggregates.
- Keep domain enums and invariants under `domain/model`.
- Add pure tests for appointment lifecycle, customer/artist/service status, and
  tenant ownership invariants.
- Do not allow Spring, JPA, HTTP, or JSON imports in `domain`.

### 4. Introduce application-owned ports and persistence adapters

- ✅ Move JPA types to `adapter/out/persistence/entity` with `Entity` suffixes.
- ✅ Move Spring Data interfaces to `adapter/out/persistence/repository` with
  `SpringData...Repository` names.
- ✅ Create the Customer repository port under `application/port/out`.
- ✅ Add the Customer persistence mapper and adapter, including managed-entity
  update behavior.
- ✅ Create the Service repository port and persistence mapper/adapter,
  including managed-entity update behavior.
- Create the remaining repository ports under `application/port/out`.
- Add the remaining persistence mappers and adapters.
- Test both new-entity and managed-entity update paths.

### 5. Move application services and inbound web adapters

- ✅ Relocate services to `application/service` and controllers to
  `adapter/in/web` without changing routes or runtime behavior.
- ✅ Migrate `CustomerService` to the Customer domain model and repository
  port; its inbound controller no longer exposes a persistence entity.
- ✅ Migrate `ServiceCatalogService` to the Service domain model and repository
  port; its inbound controller no longer exposes a persistence entity.
- Make services implement public use-case
  interfaces.
- Ensure controllers call use cases and do not access repositories.
- Preserve route paths, response shapes, tenant context, and authorization.

The relocation is a green checkpoint; use-case ports and repository isolation
remain part of the next structural slice.

### 6. Normalize nested metadata and architecture rules

- ✅ Add `package-info.java` to the newly materialized application and inbound
  adapter packages.
- Add `package-info.java` to each remaining materialized core package.
- Preserve `documents` and `subscriptions` as deferred nested capabilities.
- Update Modulith and layer tests without weakening unrelated module rules.

### 7. Verify and document

- Run focused Studio unit, repository, web, and integration tests.
- Run Studio Modulith/layer tests and Calendar regressions.
- Run `./gradlew ci -x test -x integrationTest -x e2eTest`.
- Update `docs/architecture/05-operations/service-architecture-migration.md`,
  `tasks/todo.md`, and `tasks/lessons.md` if new failures occur.
- Commit and push the feature branch.

## Definition of done

- [ ] Core Studio production classes use canonical packages.
- [ ] Public contracts are grouped by API kind.
- [ ] Domain models are framework-independent.
- [ ] Persistence entities are isolated behind application-owned ports.
- [ ] Controllers are inbound adapters and preserve HTTP behavior.
- [ ] Documents and subscriptions are explicitly deferred, not partially moved.
- [ ] Studio focused tests, architecture tests, and service CI pass.
- [ ] Documentation and lessons are updated.
- [ ] Feature branch is committed and pushed.
