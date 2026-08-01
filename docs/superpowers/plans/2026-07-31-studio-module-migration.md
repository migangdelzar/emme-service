# Studio Module Migration Plan

> Execute with `superpowers:executing-plans`. Each implementation task follows
> Red → Green → Refactor and ends with a verification checkpoint.

| Field | Value |
|---|---|
| Design | [`2026-07-31-studio-module-migration-design.md`](../specs/2026-07-31-studio-module-migration-design.md) |
| Module | `modules/studio` |
| Branch | `feat/studio-module-migration` |
| Status | Core Studio and one-use-case-per-service normalization complete; nested capabilities explicitly deferred |
| Date | 2026-07-31 |

**Canonical template:** [`../../templates/module-package-structure-template.md`](../../templates/module-package-structure-template.md), reviewed 2026-07-31.

**Template conformance:** Core Studio is complete against the current template.
The public Studio contract is split into focused business-profile, appointment,
and customer use cases under `api/usecase`; the legacy `SalonApi` facade is
removed. The existing public event contracts remain under `api/event`. `documents` and
`subscriptions` are not counted as migrated: each requires its own plan before
its JPA entities, repositories, controllers, and configuration can leave the
nested legacy packages. Those follow-up plans are
[`2026-07-31-studio-documents-module-migration.md`](2026-07-31-studio-documents-module-migration.md)
and
[`2026-07-31-studio-subscriptions-module-migration.md`](2026-07-31-studio-subscriptions-module-migration.md).

## Scope

Migrate the core Studio capability. `documents` and `subscriptions` remain
explicitly deferred nested capabilities until the core module boundary is
verified.

## Tasks

### 1. Add architecture guardrails

- ✅ Commit `StudioPackageConventionTest` after the core root package move.
- ✅ Reject root production ownership in `entity`, `event`, and `web`.
- ✅ Require grouped public API types.
- ✅ Tighten the guardrail to forbid the application layer from importing
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
- ✅ Extract Artist and Artist Capability domain models with explicit status and
  relationship lifecycle behavior.
- ✅ Create framework-independent domain models for the core Studio aggregates.
- ✅ Keep domain enums and invariants under `domain/model`.
- ✅ Add pure tests for appointment lifecycle, configuration invariants, and
  customer/artist/service status.
- ✅ Do not allow Spring, JPA, HTTP, or JSON imports in `domain`.

### 4. Introduce application-owned ports and persistence adapters

- ✅ Move JPA types to `adapter/out/persistence/entity` with `Entity` suffixes.
- ✅ Move Spring Data interfaces to `adapter/out/persistence/repository` with
  `SpringData...Repository` names.
- ✅ Create the Customer repository port under `application/port/out`.
- ✅ Add the Customer persistence mapper and adapter, including managed-entity
  update behavior.
- ✅ Create the Service repository port and persistence mapper/adapter,
  including managed-entity update behavior.
- ✅ Create Artist and Artist Capability repository ports and persistence
  mappers/adapters, including managed relationship resolution.
- ✅ Create the remaining core repository ports under `application/port/out`.
- ✅ Add the remaining Appointment and business-configuration persistence
  mappers and adapters, plus the application event publisher port/adapter.
- ✅ Test the Appointment new-entity and managed-entity update paths.

### 5. Move application services and inbound web adapters

- ✅ Relocate services to `application/service` and controllers to
  `adapter/in/web` without changing routes or runtime behavior.
- ✅ Migrate `CustomerService` to the Customer domain model and repository
  port; its inbound controller no longer exposes a persistence entity.
- ✅ Migrate `ServiceCatalogService` to the Service domain model and repository
  port; its inbound controller no longer exposes a persistence entity.
- ✅ Migrate `ArtistService` to Artist/Artist Capability domain models and
  repository ports; its inbound controller no longer exposes persistence
  entities.
- ✅ Public cross-module operations implement focused `api.usecase` interfaces
  (`GetBusinessProfileUseCase`, `ListAppointmentsUseCase`, and
  `ListCustomersUseCase`); the multi-operation `SalonApi` facade is removed.
- ✅ Ensure the migrated controllers call application services and do not access
  repositories or persistence entities.
- ✅ Move the dashboard SSE broadcaster into the inbound web adapter boundary.
- Preserve route paths, response shapes, tenant context, and authorization.

The relocation and use-case isolation are green checkpoints; nested capability
behavior remains governed by the Documents and Subscriptions plans.

### 6. Normalize nested metadata and architecture rules

- ✅ Add `package-info.java` to the newly materialized application and inbound
  adapter packages.
- ✅ Add `package-info.java` to each remaining materialized core package.
- Preserve `documents` and `subscriptions` as deferred nested capabilities.
- Update Modulith and layer tests without weakening unrelated module rules.

### 7. Verify and document

- ✅ Run focused Studio domain, persistence, web compilation, and architecture
  tests.
- ✅ Run Studio Modulith/layer tests and the existing Studio/Calendar regression
  suite through the repository verification gates.
- Run `./gradlew ci -x test -x integrationTest -x e2eTest`.
- Update `docs/architecture/05-operations/service-architecture-migration.md`,
  `tasks/todo.md`, and `tasks/lessons.md` if new failures occur.
- Commit and push the feature branch.

## Definition of done

- [x] Core Studio production classes use canonical packages.
- [x] Public contracts are grouped by API kind.
- [x] Migrated domain models are framework-independent.
- [x] Migrated persistence entities are isolated behind application-owned ports.
- [x] Migrated controllers are inbound adapters and preserve HTTP behavior.
- [ ] Documents and subscriptions are explicitly deferred, not partially moved.
- [x] Studio focused tests, architecture tests, and service CI pass.
- [x] Documentation and lessons are updated.
- [x] Feature branch is committed and pushed.

## One-use-case-per-service normalization — 2026-08-01

- [x] Replace the multi-operation `SalonApi` contract with focused public
  business-profile, appointment-list, and customer-list use cases.
- [x] Split customer, artist, service-catalog, business-configuration, and
  appointment operations into one service per use case.
- [x] Remove the legacy aggregate services and update inbound adapters and
  cross-module consumers to depend on focused use-case interfaces.
- [x] Add an executable convention test rejecting the removed aggregate service
  and facade names.
- [x] Verify Studio compilation, Spotless, Checkstyle, unit tests, and
  integration tests.
