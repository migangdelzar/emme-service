# Enterprise Module Template Conformance Inventory

| Field | Value |
|---|---|
| Repository | `emme-service` |
| Branch | `feat/enterprise-module-template-conformance` |
| Commit | `04d1f7f` baseline; cross-module dependency evidence is updated by the current conformance slice |
| Date | 2026-08-04 |
| Scope | Production Java sources under `modules/` and the `emme-platform` architecture tests |
| Source of truth | [Architecture naming conventions](../../architecture/00-project/naming-conventions.md) and [module template](../../templates/module-package-structure-template.md) |

## Purpose

This inventory records the current repository shape after public-contract naming
normalization. Counts intentionally exclude `package-info.java`; package metadata
is reported separately. The inventory is evidence, not a quality score: a large
module is not automatically wrong, and an empty contract boundary can be correct.

## Production type inventory

The architectural role is derived from the owning package path. Nested capabilities
such as Assistant AI and Studio Documents remain owned by their parent module and
are not counted as separate business modules.

| Module | Production Java types | Primary role | Current conformance signal |
|---|---:|---|---|
| `assistant` | 116 | Assistant conversations, actions, WhatsApp, and AI capability | Grouped API, application services, domain model, inbound/outbound adapters |
| `audit` | 0 | Reserved metadata-only boundary | No business implementation; ownership remains ADR-controlled |
| `booking` | 0 | Contract boundary | No production implementation |
| `calendar` | 94 | Calendar synchronization and Google integrations | Grouped API, synchronization adapters, persistence ports |
| `catalog` | 48 | Catalog and hybrid search | Catalog-owned search port and outbound adapters |
| `customer` | 0 | Contract boundary | No production implementation |
| `identity` | 161 | Authentication, membership, authorization, and feature flags | Security boundary, grouped contracts, persistence/process adapters |
| `notification` | 54 | Notification delivery | Provider ports, delivery lifecycle, tenant-safe persistence |
| `payment` | 54 | Payment lifecycle and provider callbacks | Provider boundary, webhook adapter, persistence ports |
| `shared` | 15 | Technical shared infrastructure | Persistence, JDBC, identity, search, time, and web primitives |
| `studio` | 267 | Studio core plus Documents and Subscriptions | Nested capability boundaries with grouped APIs |
| `tenancy` | 85 | Tenant lifecycle, routing, provisioning, and registry | Tenant-scoped persistence and provisioning process |
| `workforce` | 0 | Contract boundary | No production implementation |

**Total:** 894 production Java types across 14 module directories, including
the intentionally empty contract and metadata-only boundaries.

## Application-service inventory

The repository currently contains the following concrete application-service
counts. `ApplicationServiceArchitectureTest` verifies that each matching
`*Service.java` implements exactly one corresponding `*UseCase`.

| Module | Application services |
|---|---:|
| `assistant` | 16 |
| `calendar` | 14 |
| `catalog` | 5 |
| `identity` | 13 |
| `notification` | 5 |
| `payment` | 7 |
| `studio` | 51 |
| `tenancy` | 12 |

Modules without application services are contract-only or metadata-only. The
one-use-case-per-service rule remains the default for all future implementations;
process managers are workflow coordinators and are excluded when they are not
named `*Service`.

## Package metadata and named interfaces

- `333` production package directories have `package-info.java` metadata across
  `modules/`, `libraries/`, and `emme-platform` production sources.
- The package metadata architecture test passes for all materialized production
  packages.
- Named interfaces are explicit and capability-specific. Current interface names
  include `assistant-api`, `assistant-ai-api`, `calendar-events`,
  `calendar-results`, `calendar-types`, `calendar-usecases`, `catalog-api`,
  `documents-api`, `identity-api`, `identity-security`, `notification-api`,
  `notification-events`, `payment-api`, `persistence`, `persistence-jdbc`,
  `search`, `studio-api`, `studio-events`, `subscriptions-api`, `tenant-api`,
  `tenant-events`, and `time`.
- The variation is deliberate legacy boundary history, not an invitation to add
  another naming style. New modules must use the canonical named-interface rules
  in the module template and document deliberate exceptions.

## Boundary scans

| Scan | Result | Evidence |
|---|---|---|
| Direct `SpringData*Repository` imports from application/inbound code | None found | `rg` source scan; DDD/Hexagonal architecture test passes |
| Production `DataSource#getConnection()` calls | None found | `rg` source scan; ten remaining calls are integration-test setup only |
| Public API declarations ending in `Info`, `View`, `State`, or `Kind` | None found for the normalized contract set | Naming architecture test and stale-name scan pass |
| Public API event declarations ending in `Event` | None found | Event contract architecture test; Studio appointment facts omit the redundant suffix and dashboard SSE data is no longer an API event |
| Removed `studio-api` project references | None in active build | `ApplicationServiceArchitectureTest` and platform target validator |
| Domain imports of Spring/JPA/Kafka/adapter infrastructure | No violations | `DddHexagonalArchitectureTest` passes |
| Application imports of technical adapters | No violations | `DddHexagonalArchitectureTest` passes |
| Inbound adapters importing outbound adapters | No violations | `DddHexagonalArchitectureTest` passes |
| Cross-module business implementation imports | No violations | `CrossModuleDependencyArchitectureTest` passes; Calendar now uses Shared's authenticated-subject context instead of Identity's web adapter |
| Empty source directories | One stale test directory removed | `modules/tenancy/src/test/java/com/emme/tenancy/application/audit` was empty after audit ownership normalization; no production capability directory was empty |

The direct connection calls under `src/integrationTest` are intentional test
fixture/setup operations. Production connection acquisition remains behind the
shared throwable connection executor; converting test setup to that helper is a
separate cleanup task and must not be confused with a production boundary leak.

The source inventory contains 56 command records, 35 query records, 56 result
records, 135 use-case interfaces, 10 public event records, 19 public API
exceptions, and 16 public API types. A full per-module contract matrix remains
open for the next API vertical-slice review; these aggregate counts are not a
substitute for checking each contract's semantic pairing.

## Verification performed

The following commands passed on this branch:

```text
./gradlew :modules:assistant:test :modules:calendar:test :modules:catalog:test :modules:identity:test :modules:notification:test :modules:payment:test :modules:studio:test :modules:tenancy:test --no-daemon --no-configuration-cache
./gradlew :applications:emme-platform:test --tests com.emme.NamingConventionArchitectureTest --tests com.emme.DddHexagonalArchitectureTest --tests com.emme.ModularityTest --no-daemon --no-configuration-cache
./gradlew spotlessApply --no-daemon --no-configuration-cache
./gradlew :modules:identity:test :applications:emme-platform:test --tests com.emme.NamingConventionArchitectureTest --tests com.emme.DddHexagonalArchitectureTest --tests com.emme.ModularityTest spotlessCheck --no-daemon --no-configuration-cache
node scripts/validate-markdown.mjs
git diff --check
```

The commit pre-push gate also passed the repository test, coverage, and coverage
verification lifecycle.

The dependency-boundary slice additionally passed:

```text
./gradlew :modules:shared:test --tests com.emme.shared.web.security.CurrentUserContextHolderTest
./gradlew :modules:calendar:test
./gradlew :applications:emme-platform:test --tests com.emme.CrossModuleDependencyArchitectureTest --tests com.emme.ModularityTest --tests com.emme.NamedInterfaceArchitectureTest
```

## Remaining inventory work

The following are intentionally still open in the conformance plan because this
document does not claim evidence that was not collected:

1. Empty-directory and obsolete-file deletion review across generated/build
   output and test fixtures.
2. Provider, replay, rollback, and recovery evidence for the high-risk modules.
3. Final Kafka/Spring Modulith delivery and production deployment verification.
