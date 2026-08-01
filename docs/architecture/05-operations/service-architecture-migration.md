# Service Architecture Migration Specification

| Field | Value |
|---|---|
| Status | Approved implementation baseline |
| Scope | `emme-service` architecture, infrastructure, and module-boundary hardening |
| Related handbook | [`docs/architecture/`](../) |
| Canonical module template | [`docs/templates/module-package-structure-template.md`](../../templates/module-package-structure-template.md) |
| Build-logic model | Capability-Driven Build Logic (CDD) |
| Date | 2026-07-31 |

## Objective

Bring the separated service repository to the architecture already defined by the
Modulith handbook while preserving behavior:

- business modules use DDD + Hexagonal Architecture;
- `api/` is the only cross-module contract surface;
- persistence types live in `adapter/out/persistence`;
- build logic remains capability-first and declarative at module call sites;
- infrastructure manifests are statically verifiable and production-safe;
- module-boundary tests are executable gates rather than documentation only.

The migration is incremental. A module is migrated through a real vertical slice,
then its architecture test and focused tests become the evidence for the next slice.
The full module template is a target shape, not a reason to create empty packages.

## Architecture decisions

### Business modules and build logic are different models

```mermaid
flowchart LR
    BUILD[Gradle build.gradle.kts] --> CONVENTION[Convention plugin]
    CONVENTION --> CAPABILITY[CDD build capability]
    CAPABILITY --> TASK[Task / provider port]
    TASK --> TOOL[External build tool]

    IN[HTTP / event / scheduler] --> API[Module api]
    API --> APP[Application service]
    APP --> DOMAIN[Pure domain]
    APP --> PORT[Outbound port]
    ADAPTER[Outbound adapter] -.implements.-> PORT
    ADAPTER --> DATABASE[(Database)]
```

Business modules do not receive `plugin/`, `task/`, or `provider/` packages. Gradle
build logic does not receive `domain/`, `application/`, or `adapter/` packages.
Both models protect boundaries, but their organizing units are different.

## Studio migration evidence — 2026-07-31

The core Studio migration is incremental. The following vertical slices now use
the canonical DDD + Hexagonal boundary:

```mermaid
flowchart LR
    Web[Inbound web adapter] --> App[Application service]
    App --> Domain[Appointment / configuration domain]
    App --> Port[Application-owned outbound port]
    Port --> Adapter[Persistence or messaging adapter]
    Adapter --> JPA[(JPA entity / Spring Data)]
```

- Appointment lifecycle is represented by `domain.model.Appointment`; JPA
  state is isolated in `adapter.out.persistence.entity.AppointmentEntity`.
- Appointment, collision detection, operating hours, business profile, and
  booking policy dependencies are expressed through `application.port.out`.
- Public appointment events are published through
  `AppointmentEventPublisher`, implemented by the messaging adapter.
- `AppointmentController`, `BusinessConfigController`, and `SalonApiImpl` no
  longer depend on persistence entities or Spring Data repositories.
- `StudioPackageConventionTest` enforces the application-to-adapter boundary.

The remaining nested `documents` and `subscriptions` capabilities stay
explicitly deferred until their own public contracts and ownership boundaries
are migrated; no empty architecture layers are created for them.

### Migration order

1. Restore deterministic CI prerequisites (dependency verification and dependency
   graph integration).
2. Migrate the catalog pilot’s persistence boundary.
3. Replace identity’s cross-module implementation imports with public APIs/events.
4. Keep the architecture tests strict and update their messages to the canonical
   package names.
5. Audit deployment manifests and record safe follow-up work separately from code
   migrations.

## Module migration contract

For each migrated module:

- `domain` has no Spring, JPA, HTTP, broker, or persistence imports;
- `application.service` implements `api.usecase` and coordinates workflows;
- `application.port.out` contains application-owned interfaces;
- `adapter.in` contains transport entry points;
- `adapter.out` contains technology-specific implementations;
- public events live in `api.event` and use past-tense names;
- `package-info.java` documents each materialized package;
- the module’s focused unit/integration tests and `ModularityTest` pass.

## Infrastructure baseline

The service owns runtime manifests, database migration execution, observability
rules, and release-facing container configuration. Every infrastructure change must
be checked with the appropriate static validator (`kustomize build`, Terraform
format/validate, or container build) and must not introduce plaintext production
credentials. Local development credentials remain explicitly scoped to the local
overlay.

## Web i18n boundary

The web application owns translated strings and locale preference. The service owns
stable error codes and machine-readable problem details. A backend response must not
require a specific UI language; the frontend maps an error code to a localized
message. Locale detection follows this precedence:

```text
explicit persisted preference -> browser language preferences -> en-US fallback
```

The locale catalog must have identical leaf keys for every supported locale and the
test-id catalog must remain independent from translated text.

## Calendar vertical-slice evidence

The Calendar module is the first complete application slice migrated to the module
template. Its ownership is now explicit:

```mermaid
flowchart LR
    WEB[adapter.in web or messaging] --> API[api use case / event]
    API --> APP[application service]
    APP --> DOMAIN[domain model]
    APP --> PORT[application.port.out]
    PORT --> PERSIST[adapter.out.persistence]
    PORT --> GOOGLE[adapter.out.google]
```

- Public contracts are grouped under `api/result`, `api/usecase`, `api/type`, and
  `api/event`.
- Domain models are framework-independent and tested in isolation.
- JPA entities and Spring Data repositories are named with `Entity` and
  `SpringData...Repository` suffixes and remain under persistence adapters.
- Application services depend on `application.port.out`, including
  `GoogleCalendarPort`; they do not import concrete adapter classes.
- The migration is guarded by `CalendarPackageConventionTest`, Modulith tests,
  ArchUnit tests, persistence adapter tests, and the Calendar module test suite.
- The web client parses RFC 9457-compatible `application/problem+json` responses,
  preserves machine-readable `code` values, and maps Calendar/Google failures to
  locale-owned messages.

## Success criteria

- `./gradlew ci -x test -x integrationTest -x e2eTest --no-daemon --no-configuration-cache`
  passes in the service repository.
- `./gradlew :applications:studio-api:test --tests com.emme.ModularityTest
  --tests com.emme.LayerConventionTest --no-daemon --no-configuration-cache` passes.
- Catalog persistence classes are under `adapter/out/persistence/entity` and domain
  classes are framework-independent.
- Identity consumes only tenancy API types and studio public events.
- `bun run quality` passes in the web repository, including catalog validation and
  locale behavior tests.
- Infrastructure validation is documented and executable where tooling is available.

## Deliberate non-goals

- Do not rename every legacy module in one change.
- Do not expose JPA entities as a replacement public API.
- Do not translate backend error messages by duplicating UI catalogs in Java.
- Do not weaken architecture tests to make legacy packages pass.
- Do not enable production deployment or mutate external cloud resources as part of
  this migration.
