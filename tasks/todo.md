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
