# Service architecture migration checklist

## Acceptance criteria

- [x] CDD build-logic architecture is implemented and documented.
- [x] Catalog persistence is separated from the pure domain model.
- [x] Cross-module identity dependencies use public API/event contracts.
- [x] Architecture tests pass without weakening boundary rules.
- [x] Gradle dependency verification includes the CI-resolved JUnit metadata.
- [x] Infrastructure manifests have a deterministic validation path.
- [x] Web i18n follows the Clara reference pattern and its quality gate passes.
- [x] Changes are committed and pushed in logical commits.

## Working notes

- The Modulith handbook is the source of truth for module and build-logic structure.
- `emme-service` already contains the CDD conventions and strict architecture tests;
  this task completes the implementation migration rather than creating a second
  architecture model.
- The web repository already has a shared `@emme/i18n` package; the migration will
  improve its type safety and locale boundary instead of introducing a duplicate
  package.

## Results

- Migrated build-logic packages from type-first buckets into `core/`, `root/`,
  capability-owned packages, and `git/` while preserving plugin IDs.
- Added the missing `emme.security` convention entry point and registration test.
- Migrated catalog persistence entities and identity cross-module contracts.
- Hardened Terraform kubeconfig handling and removed public Kubernetes API access.
- Added CI rendering/validation for Kubernetes overlays and Terraform.
- Verified Modulith/service database migrations are semantically identical; only
  source line endings differ in the legacy comparison.
- Verified with `./gradlew ci -x test -x integrationTest -x e2eTest`, build-logic
  unit/functional checks, architecture tests, focused catalog tests, Markdown
  validation, both Kustomize overlays, and web `bun run quality`.
- Pushed service commits through `0894e9a`; the final remote CI run is green for
  infrastructure, quality, tests, build-logic, boundary verification, and boot
  JAR packaging.
- Made OWASP NVD access explicit: configure `NVD_API_KEY` for the dependency
  scan and use the persisted NVD cache; without the secret, the job skips
  deterministically instead of timing out on public NVD rate limits.

## Studio vertical slices — 2026-07-31

- [x] Appointment domain lifecycle and persistence boundary migrated.
- [x] Collision detection uses an application-owned port.
- [x] Operating hours, business profile, and booking policy use domain models
  and application-owned persistence ports.
- [x] Appointment event publication uses an application-owned port and adapter.
- [x] `SalonApiImpl` no longer imports Spring Data or persistence entities.
- [x] Dashboard SSE transport is owned by `adapter.in.web.sse`.
- [x] Application-layer ArchUnit guardrail passes.
- [x] Public cross-module use-case normalization and full service CI are complete.
- [ ] Migrate `documents` and `subscriptions` only after their public contracts
  and ownership boundaries are explicitly designed.

## Calendar vertical slice — 2026-07-31

- [x] Calendar canonical package migration complete.
- [x] Calendar domain has no framework imports.
- [x] Calendar public contracts are grouped by API kind.
- [x] Calendar persistence entities are isolated behind application-owned ports.
- [x] Calendar application services do not depend on concrete outbound adapters.
- [x] Calendar service and focused architecture/persistence tests pass.
- [x] Web Calendar/Google error handling preserves stable problem codes.
- [x] Web Calendar/Google messages are localized in supported locales.
- [x] Full cross-repository final commit and remote verification.

## Assistant canonical module migration — 2026-07-31

- [ ] Execute `docs/superpowers/plans/2026-07-31-assistant-module-template-migration.md`.
- [ ] Keep the latest `docs/templates/module-package-structure-template.md` authoritative.
- [ ] Preserve Assistant HTTP, webhook, JSON, database, and feature-flag behavior.
- [ ] Separate pure domain models, persistence entities, ports, adapters, grouped API contracts, and package metadata.
- [ ] Run the complete Assistant and service verification gates before merging.

## Module migration plan registry — 2026-07-31

- [x] Normalize contract-only plans for `customer`, `workforce`, and `booking`.
- [x] Keep Calendar and core Studio plans explicitly marked conformance-complete;
  track Studio `documents` and `subscriptions` separately.
- [x] Create canonical migration plans for `identity`, `tenancy`, `notification`,
  `payment`, `audit`, and `shared`.
- [x] Keep Catalog as the verified implementation baseline and do not treat the
  CDD build-logic plan as a business-module migration.
- [ ] Run service-wide architecture verification after every module plan reaches
  implementation completion.

### Plan update results

- Added the plan registry at `docs/superpowers/plans/README.md`.
- Added canonical plans for Customer, Workforce, Booking, Identity, Tenancy,
  Notification, Payment, Audit, Shared, Catalog baseline verification, Studio
  Documents, and Studio Subscriptions.
- Updated Calendar and Studio plans with current-template conformance notes.
- Corrected the service migration design so Identity and Tenancy are not falsely
  reported as completed baselines.

## Contract-only module implementation slice — 2026-07-31

- [x] Normalized Customer's empty API namespace and retained its root Modulith
  metadata.
- [x] Normalized Workforce's empty API namespace and retained its root Modulith
  metadata.
- [x] Removed Booking's obsolete top-level `events` metadata and stale named
  interface dependencies; retained only actual shared/tenancy dependencies.
- [x] Added source-tree convention tests for all three contract-only modules.
- [x] Verified focused module tests and `applications:studio-api` Modulith tests.
- [ ] Continue with the next dependency-safe migration slice from the registry
  (Identity/Tenancy security and persistence inventory).

## Identity/Tenancy contract boundary slice — 2026-07-31

- [x] Added failing package-boundary tests for grouped public contracts and
  normalized event naming.
- [x] Grouped Identity use-case/results and Tenancy use-case/results under the
  current module template.
- [x] Renamed `TenantCreatedEvent` to `TenantCreated` and updated its consumer.
- [x] Preserved existing Modulith named-interface identifiers and dependency
  semantics while moving package ownership.
- [x] Verified focused tests, full Identity/Tenancy module tests, and Studio
  Modulith verification.
- [ ] Continue with Identity security/domain/persistence separation and Tenancy
  isolation/provisioning separation as separate red-green-refactor slices.

## Identity/Tenancy persistence ownership slice — 2026-07-31

- [x] Added failing tests requiring persistence types to live under outbound
  adapter ownership.
- [x] Moved Identity entities/enums and Spring Data repositories under
  `adapter/out/persistence`.
- [x] Moved Tenancy entities/enums and repositories under
  `adapter/out/persistence`, plus bootstrap registry access under
  `adapter/out/client/database`.
- [x] Updated all production, test, and fixture imports; no legacy entity package
  Java sources remain.
- [x] Verified full Identity/Tenancy tests, Checkstyle, Spotless, compilation,
  and Studio Modulith tests.
- [x] Introduced the Tenancy application-owned repository port, pure Tenant
  aggregate, persistence entity/mapper/adapter, and updated all callers.
- [x] Verified focused domain/mapper/repository tests plus full Tenancy check and
  Studio Modulith verification.
- [x] Moved Tenancy orchestration into `application/service` and renamed the
  scheduled worker to `application/process/TenantProvisioningProcessManager`.
- [x] Verified all Tenancy web/module/repository tests, Checkstyle, Spotless, and
  Studio Modulith verification after Spring proxy wiring was preserved.
- [x] Moved Tenancy controllers, HTTP request/response models, web mapper,
  request-context filters, trusted resolver, rate limiting, and MVC configuration
  into canonical inbound/configuration packages.
- [x] Verified full Tenancy tests, Checkstyle, Spotless, and Studio Modulith
  verification after the inbound adapter migration.
- [ ] Next slice: isolate TenantContextAspect and database-pool/registry ports,
  then continue Identity security/domain boundaries.

## Architecture naming contract — 2026-07-31

- [x] Added the canonical naming catalog at
  `docs/architecture/00-project/naming-conventions.md`.
- [x] Documented naming for packages, files, classes, records, enums, interfaces,
  exceptions, methods, fields, constants, module contracts, adapters,
  repositories, controllers, events, tests, and CDD build-logic types.
- [x] Linked all 33 other architecture Markdown files to the canonical catalog.
- [x] Direct Markdown validation passes with `node scripts/validate-markdown.mjs`.
- [ ] Run the `mise run docs-check` wrapper after the local `mise.toml` trust
  decision is made; the underlying validator already passes.

## Validation conventions — 2026-08-01

- [x] Add the canonical backend validation page for Jakarta Bean Validation,
  records, custom cross-field constraints, domain/application ownership, i18n,
  error mapping, naming, and tests.
- [x] Link validation guidance from the handbook, backend API/controller pages,
  module template, and naming catalog.
- [x] Align Tenancy create/update request records with the persisted slug/name
  bounds using `@Size` in the inbound adapter.
- [x] Add focused validation regression coverage and verify the existing web
  boundary still rejects an oversized slug before persistence.
- [x] Direct Markdown validation passes with `node scripts/validate-markdown.mjs`.
- [x] Run the complete Tenancy check and service Modulith verification before
  committing this slice.
