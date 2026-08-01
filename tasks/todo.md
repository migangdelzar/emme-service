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

### Identity Membership domain/application slice — 2026-08-01

- [x] Add failing package guardrails for the Membership domain and persistence
  boundary.
- [x] Introduce framework-free Membership lifecycle behavior.
- [x] Add application-owned membership/role ports and MembershipService.
- [x] Add persistence mapper and adapters while preserving managed JPA identity.
- [x] Rewire Identity membership/current-user/public API flows.
- [x] Verify Identity tests and mapper round-trip coverage.
- [ ] Continue with permission/identity service separation and typed security
  configuration.

### Identity permission application slice — 2026-08-01

- [x] Add failing tests for the permission use case and package boundary.
- [x] Introduce `PermissionPort` and `GetUserPermissionsUseCase`.
- [x] Move permission traversal into `PermissionPersistenceAdapter`.
- [x] Rewire permission consumers and remove legacy `IdentityService`.
- [x] Verify unit, integration, architecture, formatting, and Modulith gates.
- [ ] Continue with customer authentication and customer-membership event
  application separation.

### Identity Feature Flag application slice — 2026-08-01

- [x] Add failing domain, application, and package-boundary tests.
- [x] Introduce the Feature Flag domain model and application repository ports.
- [x] Isolate JPA persistence behind entity, mapper, and adapter types.
- [x] Isolate subscription plan lookup behind `SubscriptionPlanPort`.
- [x] Preserve the SpEL bean name and feature-flag HTTP behavior.
- [x] Verify Identity tests and Assistant test compilation.
- [x] Continue with customer-membership event application separation.

### Identity customer-membership event slice — 2026-08-01

- [x] Add failing tests for the membership event application boundary.
- [x] Introduce the framework-free CustomerMembership model and repository port.
- [x] Move idempotent membership creation into EnsureCustomerMembershipService.
- [x] Rename and isolate the composite-key JPA entity and Spring Data repository.
- [x] Move appointment event handling to the inbound messaging consumer package.
- [x] Verify Identity tests, architecture checks, and affected application tests.
- [x] Continue with customer authentication application separation.

### Identity customer authentication slice — 2026-08-01

- [x] Add failing tests for customer identity domain and use-case boundaries.
- [x] Introduce public customer authentication/profile commands, results, and
  use cases.
- [x] Move provider-token decoding and customer identity persistence behind
  application-owned ports.
- [x] Rename CustomerIdentity technical persistence types and add mapper/adapter
  implementations.
- [x] Rewire AuthController without exposing JPA entities.
- [x] Verify Identity checks and login/profile regression coverage.
- [x] Continue with typed security configuration and Identity failure advice.

### Identity typed security configuration slice — 2026-08-01

- [x] Add failing tests for typed security defaults and package ownership.
- [x] Introduce IdentitySecurityProperties with safe local defaults.
- [x] Rewire SecurityConfiguration to consume typed properties.
- [x] Verify Identity checks and security configuration regression coverage.
- [x] Continue with Identity-specific failure advice.

### Identity failure advice slice — 2026-08-01

- [x] Add failing tests for Identity-owned expected exceptions and ProblemDetail
  mapping.
- [x] Introduce public customer authentication/profile exception types.
- [x] Add scoped Identity web advice without replacing shared global handling.
- [x] Rewire application services to raise stable Identity failures.
- [x] Verify Identity checks and HTTP error regression coverage.
- [x] Continue with final Identity security hardening and Keycloak application
  boundary separation.

### Identity Keycloak application boundary slice — 2026-08-01

- [x] Add failing tests for password-grant orchestration behind an application
  use case and outbound port.
- [x] Introduce typed user-authentication commands, queries, and results.
- [x] Move OkHttp/Jackson/Keycloak user authentication into an outbound adapter.
- [x] Rewire AuthController through the application use case.
- [x] Remove the legacy application KeycloakAuthService.
- [x] Verify Identity checks, login regression coverage, and Modulith boundaries.

### Identity typed Keycloak configuration slice — 2026-08-01

- [x] Add failing tests for typed user/admin client settings and adapter
  ownership.
- [x] Extend `IdentityKeycloakProperties` across user authentication, admin
  provisioning, issuer, realm, and client settings.
- [x] Inject the configured Identity HTTP client into both Keycloak adapters.
- [x] Update application, local, platform, integration-test, and shared-test
  fixture configuration.
- [x] Verify focused Identity regressions, full Identity checks, Studio
  Modulith verification, Markdown validation, and whitespace checks.

Remaining Identity follow-up: complete the broader security hardening review.

### Identity realm provisioning hardening slice — 2026-08-01

- [x] Add failing tests for missing provisioning credentials and configurable
  retry behavior.
- [x] Move realm client, redirect URI, role, admin-user, and retry settings into
  `IdentityRealmProvisioningProperties`.
- [x] Remove the production hardcoded tenant-user password and validate the
  provisioning password before contacting the provider.
- [x] Inject retry delay behavior through `RetryDelayPort` so tests do not block
  on real sleeps.
- [x] Remove the master admin password default and expose environment-backed
  configuration for runtime profiles.
- [x] Verify Identity checks, Studio Modulith verification, Markdown validation,
  whitespace checks, and the source-level secret guard.

Remaining Identity follow-up: complete the broader security hardening review.

### Identity realm provisioning port slice — 2026-08-01

- [x] Add failing process and package-boundary tests for the outbound
  administration capability.
- [x] Introduce `IdentityProviderAdministrationPort` under
  `application/port/out`.
- [x] Rewire `KeycloakRealmProvisioningProcessManager` to depend only on the
  application port.
- [x] Keep Keycloak HTTP administration inside the outbound adapter.
- [x] Verify full Identity checks, Studio Modulith verification, Markdown
  validation, and whitespace checks.

Remaining Identity follow-up: complete the broader security hardening review.

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
- [x] Moved `TenantContextAspect` under the outbound persistence aspect package
  and normalized `DataSourceConfiguration`/`TenantPoolingProperties` under
  `configuration`.
- [x] Introduced `DatabaseRegistryPort` and immutable `DatabaseRegistryEntry`,
  renamed the JDBC implementation to `DatabaseRegistryAdapter`, and moved the
  pool/routing datasource under the database client adapter with
  `TenantDatabasePoolProvider`.
- [x] Moved Identity security configuration to `configuration/SecurityConfiguration`.
- [x] Moved the login filter, Keycloak clients/JWT decoder, and security audit
  observer into canonical inbound/outbound adapter packages.
- [x] Split tenant-created realm provisioning into an inbound event consumer and
  an application process manager.
- [x] Verified Identity tests/checks and Studio Modulith verification after the
  security boundary migration.
- [x] Moved Identity controllers and the web test into
  `adapter/in/web/controller`.
- [x] Extracted named request/response records and web mappers under the inbound
  web adapter, preserving existing HTTP contracts.
- [x] Verified the full Identity test suite after the HTTP boundary migration.
- [ ] Next slice: continue Identity application/domain separation and failure
  advice.

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

### Identity security audit hardening slice — 2026-08-01

- [x] Add failing regression tests for exception-message redaction,
  control-character sanitization, bounded audit values, and forwarded-header
  spoofing.
- [x] Log authentication failure types instead of exception messages so secrets
  and provider details cannot enter audit output.
- [x] Sanitize and bound user-controlled audit values before structured fields
  are written to logs.
- [x] Use the socket peer address for audit IP attribution; retain the separate
  forwarded-header/rate-limit policy as an explicit follow-up decision.
- [x] Verify focused audit tests, full Identity checks, Studio Modulith,
  Markdown validation, whitespace, and source-level secret scanning.

Remaining Identity follow-up: decide trusted proxy handling for login rate
limiting, then continue authorization domain/application separation.
