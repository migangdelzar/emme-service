# Enterprise Module Template Conformance Baseline

| Field | Value |
|---|---|
| Repository | `emme-service` |
| Branch | `feat/enterprise-module-template-conformance` |
| Baseline branch | `feat/module-plans-normalization` |
| Reference | `enterprise-module-template-ddd-hexagonal-spring-modulith.md` |
| Date | 2026-08-03 |
| Scope | Backend modules and the `emme-platform` composition root |

## Executive summary

The repository already has the intended high-level backend architecture in most
modules: grouped public contracts, application services, domain packages,
inbound/outbound adapters, module metadata, and module-specific convention
tests. The remaining work is conformance and evidence work rather than a blind
rebuild of every package tree.

The downloaded template is compatible with the local handbook. The local
handbook already distinguishes backend DDD + Hexagonal Architecture from
capability-first Gradle build logic, documents one use case per application
service, defines API grouping, and describes Spring Modulith visibility. The
execution plan adds the missing repository-wide inventory, explicit naming
audit, tactical DDD decision rules, and module-specific closure work.

## Current module inventory

| Module | Production Java source files | Current architectural signal |
|---|---:|---|
| `assistant` | 166 | Canonical API/application/domain/adapter layout with a nested AI capability. |
| `audit` | 1 | Reserved metadata-only boundary. |
| `booking` | 2 | Small contract boundary. |
| `calendar` | 89 | Canonical module with Google client, adapters, synchronization, and tests. |
| `catalog` | 79 | Canonical module with catalog-owned search port and external adapters. |
| `customer` | 2 | Small contract boundary. |
| `identity` | 193 | Canonical security, API, application, domain, persistence, and process areas. |
| `notification` | 79 | Canonical provider-oriented module requiring delivery evidence. |
| `payment` | 81 | Canonical provider/webhook module requiring financial recovery evidence. |
| `shared` | 19 | Technical persistence, JDBC, identity, search, time, and web primitives. |
| `studio` | 310 | Core module with nested Documents and Subscriptions capabilities. |
| `tenancy` | 112 | Tenant lifecycle, routing, provisioning, and database adapters. |
| `workforce` | 2 | Small contract boundary. |

The counts are an inventory signal, not an approval metric. A small module is
correct when it has a small responsibility; a large module requires stronger
ownership and dependency evidence.

## Confirmed conventions already present

- `application/service` classes are generally named after one use case and the
  repository already contains a one-use-case-per-service rule in the handbook
  and lessons.
- Module roots use Spring Modulith `ApplicationModule` metadata.
- Several modules expose grouped API named interfaces and nested Studio
  capabilities have explicit named interfaces.
- `package-info.java` coverage is broad, with 306 package metadata files in the
  current source tree.
- `emme-platform` is the canonical composition root and the obsolete
  `applications/studio-api` project has been removed.
- Kafka + Spring Modulith event externalization is already implemented for the
  current MVP direction; final replay/recovery evidence remains a closure task.
- The generic throwable JDBC connection execution boundary is already present
  and has focused tests; the remaining work is repository-wide usage evidence.

## Findings requiring execution

### Public interface consistency

Named-interface declarations are not uniform. Some modules expose one grouped
`api` interface, some expose child interfaces such as `events` or `usecases`,
and some intentionally have no API named interface because they are empty
contract boundaries. The migration must preserve least privilege and remove
duplicate or empty declarations rather than applying one annotation everywhere.

### Naming consistency

The repository contains configuration names such as `*Config` and previously
contained a generic shared `BaseEntity`. The first naming slices converted the
calendar property record and the shared persistence primitive. The target is
typed `*Properties` for external settings, `*Configuration` for bean wiring,
and semantic persistence primitives rather than generic `Base*` names. The
shared primitive is now `PersistedEntity`.

`CatalogMatchService` implemented `MatchCatalogItemsUseCase`, which was the
first confirmed service-name mismatch. The red-green rename slice completed on
this branch as `MatchCatalogItemsService`; the focused Catalog convention test
now reads and verifies the canonical source path.

### Module-specific closure

Identity, Tenancy, Assistant, Notification, Payment, Studio Documents, and
Studio Subscriptions have canonical source slices but still require live
provider, recovery, replay, rollback, or final service-wide evidence documented
in their existing migration plans.

### Shared and Audit ownership

Shared must remain technical infrastructure and must not become a business
module. Audit requires an explicit ownership ADR before implementation grows.

### Build-logic boundary

Gradle build logic is already organized by build capability and remains outside
this backend conformance migration. Its CDD plan and verification are separate
from the module template.

## Required evidence commands

The following commands are the baseline and final gates for the migration:

```text
./gradlew spotlessCheck checkstyleMain test
./gradlew :applications:emme-platform:test
./gradlew :applications:emme-platform:check
./gradlew ci --no-daemon
./gradlew :applications:emme-platform:bootJar
```

Additional module-specific tests must cover Web MVC, persistence/Testcontainers,
provider contracts, tenant predicates, event publication, replay, and recovery
where the module owns those responsibilities.

## Decision

Proceed with incremental conformance. Do not create empty packages or perform a
repository-wide mechanical rename. Every rename must have a failing source or
architecture test first, update all consumers, pass focused and full checks,
and remove the obsolete name because the system is unreleased.
