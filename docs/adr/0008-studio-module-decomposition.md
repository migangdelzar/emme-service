# ADR 0008: Studio module decomposition into DDD bounded contexts

| Field | Decision |
|---|---|
| Status | Accepted — implementation in progress |
| Date | 2026-08-05 |
| Scope | `studio` monolithic module split, empty module renames |

## Current implementation baseline

The accepted decision is now being executed against the actual repository tree,
not the historical `studio-api` application. The current source still contains
`modules/studio` with 341 production Java files and 28 tests. Documents and
Subscriptions are nested under that module, while `customer` and `workforce`
are still empty contract modules. The migration therefore has to preserve
behavior while moving source ownership and updating every Gradle consumer.

The old `applications/studio-api` project has already been removed and must not
be recreated. Any remaining `studio-api` text in historical plans, validator
fixtures, or named-interface vocabulary is not an application dependency.

## Context

The `modules/studio` module evolved into a monolith holding four distinct DDD
bounded contexts under one package:

- **Service catalog** — Service offerings, categories, prices, artist capabilities
- **Customer CRM** — Staff-managed client profiles, search, history
- **Appointment scheduling** — Lifecycle, collision detection, slot search
- **Business configuration** — Profile, hours, booking policy, notification prefs

Additionally, `studio` hosts two nested capabilities (`documents/` and
`subscriptions/`) with their own DDD layers that should be standalone modules.

Four empty contract-only modules exist with poor naming:

- `workforce` — Intended for staff scheduling, but "workforce" is corporate HR language
- `customer` — Intended for customer self-service, but "customer" overlaps with CRM
- `booking` — Customer self-service booking (name is fine)
- `audit` — Metadata placeholder (name is fine, codified in ADR-0004)

## Decision

### Module renames

| Old name | New name | Rationale |
|---|---|---|
| `workforce` | `staffing` | "Staffing" reflects salon staff scheduling, shifts, and capacity — natural domain language for a service business |
| `customer` | `clients` | "Clients" is the salon's CRM bounded context. The customer self-service bounded context is `booking` |

### Nested capability extraction

| Source | Target module | Rationale |
|---|---|---|
| `studio/documents/` | `documents` | Document upload, processing, chunking, and RAG retrieval is a standalone capability consumed by `assistant` |
| `studio/subscriptions/` | `subscriptions` | Subscription plans, entitlements, and billing are consumed by `identity` (feature flags, plan-based gating) |

### Studio decomposition

| New module | Extracted from `studio` | Bounded context |
|---|---|---|
| `services` | Service, Artist, ArtistCapability domain | Service catalog — what the salon offers and who can deliver it |
| `clients` | Customer domain (was `studio` CRM) | Client CRM — staff-managed profiles, history, loyalty |
| `appointments` | Appointment domain, events, collision detection | Appointment scheduling — lifecycle, slots, status flow |
| `salon` | BusinessProfile, OperatingHours, BookingPolicy, NotificationPreference | Business configuration — tenant's identity, schedule, and rules |

### Remaining modules

- `studio` is dissolved after extraction
- `booking` stays (customer self-service, already declared deps on extracted modules)
- `audit` stays (metadata per ADR-0004)
- `staffing` stays (future staff scheduling)

### Dependency direction

```mermaid
flowchart LR
    salon --> shared
    salon --> tenancy
    services --> shared
    services --> tenancy
    clients --> shared
    clients --> tenancy
    appointments --> shared
    appointments --> tenancy
    appointments --> services
    appointments --> clients
    documents --> shared
    documents --> tenancy
    subscriptions --> shared
    subscriptions --> tenancy
    booking --> appointments
    booking --> services
    booking --> clients
    booking --> staffing
    booking --> catalog
    assistant --> documents
    calendar --> appointments
    calendar --> clients
    identity --> salon
    identity --> subscriptions
```

## Consequences

- `modules/studio` is removed; its source files move to `services`, `appointments`, `clients`, `salon`, `documents`, and `subscriptions`
- Java package declarations change: `com.emme.studio.*` → `com.emme.<module>.*`
- Cross-module imports in `identity`, `calendar`, and `assistant` are updated
- `settings.gradle.kts` lists the new modules and removes `studio`
- `applications/emme-platform/build.gradle.kts` adds new module dependencies
- Architecture tests (`ModularityTest`, cross-module dependency tests) are updated
- Existing use cases, requirements, and entity model docs are updated

## Implementation sequencing

The migration is executed in dependency-safe slices:

1. rename the empty `customer` and `workforce` modules;
2. create the target module shells and package metadata;
3. copy the Studio production/test sources exactly once using the typed mapping;
4. update Gradle dependencies and cross-module consumers;
5. remove the old Studio module;
6. run structural, compilation, test, and formatting gates;
7. run deployment, E2E, recovery, and native-image evidence gates.

The migration script is intentionally copy-first. It must not delete the source
module, must be idempotent, and must fail when it encounters an unmapped
production type. Deletion happens only after the target modules compile and the
repository-wide import audit is clean.

Tests currently stored under `com.emme.salon.*` are mapped by test class to the
same target bounded context as their production behavior; they are not treated
as Salon tests merely because of their historical package name.

## Verification

- `./gradlew :applications:emme-platform:test --tests '*ModularityTest'` passes
- `./gradlew :applications:emme-platform:check` passes (all tests, lint, boundary checks)
- No `com.emme.studio` imports remain outside the deleted module
- All cross-module consumers (`identity`, `calendar`, `assistant`, `booking`,
  shared test fixtures, and the platform application) compile against the new
  module APIs
