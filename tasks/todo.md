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

## Remaining execution backlog — priority/type order

This is the authoritative order for unfinished work. Detailed checklists remain
inside each linked migration plan; completed historical slices below are not
reopened by this backlog.

### P0 — Architecture baseline, security, and tenant isolation

- [x] Complete Catalog baseline verification and commit its verification report.
- [ ] Finish the remaining Identity security/domain/application separation in
  `docs/superpowers/plans/2026-07-31-identity-module-migration.md`.
- [ ] Complete the Tenancy boundary migration in
  `docs/superpowers/plans/2026-07-31-tenancy-module-migration.md`.

### P1 — Cross-cutting ownership and infrastructure

- [ ] Decide and record whether Audit is a real owned capability or should be
  retired; update the registry and dependencies.
- [ ] Normalize Shared infrastructure only after the Audit ownership decision,
  preserving rollback and repository-wide dependency evidence.

### P2 — Domain capabilities

- [ ] Migrate Studio Documents using its approved public contracts and the
  current module template.
- [ ] Migrate Studio Subscriptions using its approved public contracts and the
  current module template.
- [ ] Migrate Assistant after Identity, Tenancy, and Shared contracts are
  stable.

### P3 — Provider integrations

- [ ] Migrate Notification with explicit provider ports, idempotency, and retry
  evidence.
- [ ] Migrate Payment after Subscription contracts are stable, preserving
  webhook signature/replay and transaction behavior.

## Studio Documents canonical boundary slice — 2026-08-01

- [x] Add framework-free document and chunk domain models with lifecycle tests.
- [x] Move persistence types behind application-owned ports and adapters.
- [x] Extract document HTTP DTOs/mapper and move the controller to inbound
  adapter ownership.
- [x] Add package metadata and dependency-direction regression coverage.
- [x] Split application operations into one service per use case and complete
  grouped public contracts.
- [x] Expose only the Documents API through a Spring Modulith named interface.
- [ ] Complete Studio integration, Modulith, and service-wide verification.

## Studio Subscriptions canonical migration — 2026-08-01

- [x] Extract framework-free subscription domain and entitlement policy.
- [x] Move persistence behind application-owned ports, entities, mappers, and adapters.
- [x] Group subscription API contracts and replace the multi-operation service.
- [x] Move HTTP DTOs/controller to canonical inbound adapter packages.
- [x] Update Identity, Studio, and test fixtures to canonical subscription contracts.
- [x] Expose only `subscriptions-api` through a Spring Modulith named interface.
- [ ] Complete service-wide integration, Modulith, schema, security, and recovery evidence.

### P4 — Final governance verification

- [ ] Run the final service-wide architecture, Modulith, CI, boot-artifact,
  documentation, security, and rollback evidence gate.

Execution rules and dependencies are maintained in
`docs/superpowers/plans/README.md#remaining-execution-order-priority-and-type`.

## Documentation reconciliation checkpoint — 2026-08-01

- [x] Reconcile Calendar's historical TDD checklist with its completed status
  table, definition of done, source tree, and verification evidence.
- [x] Reconcile Tenancy's completed package/domain/application/web slices and
  leave only genuine port, typed-configuration, operational-evidence, and final
  verification gaps open.
- [x] Verify Tenancy unit tests, integration tests, and Studio Modulith tests.
- [x] Complete Catalog baseline verification before Identity's next
  implementation slice.

## One-service-per-use-case application boundary — 2026-08-01

- [x] Add failing Identity and Catalog architecture tests that reject
  multi-use-case application-service facades.
- [x] Split Identity membership operations into
  `AssignMembershipService`, `GetCurrentUserMembershipsService`, and
  `RevokeMembershipService`.
- [x] Split Identity feature-flag operations into one service per public use
  case while preserving the `featureFlagService` SpEL bean for evaluation.
- [x] Split Catalog item operations into one service per public use case.
- [x] Extract shared mapping/evaluation behavior into named collaborators
  instead of retaining a multi-use-case facade.
- [x] Update architecture documentation, naming conventions, and migration
  evidence to make the rule explicit.
- [ ] Run the final service-wide verification gate after the remaining module
  migrations are complete.

#### Results

The initial red architecture tests identified three multi-use-case facades:
Identity membership, Identity feature flags, and Catalog item operations. Each
was replaced with focused application services. The refactor was not required
to break a circular dependency; it enforces interface segregation, keeps
transactions and authorization boundaries explicit, and makes each use case
independently testable. Shared behavior now belongs to named mappers or
collaborators rather than a compatibility facade.

## Typed external configuration normalization — 2026-08-01

- [x] Add failing property and source-boundary tests for Calendar settings.
- [x] Bind `app.calendar.calendar-id` through `CalendarProperties` and inject
  it into Calendar application and Google outbound components.
- [x] Add failing property and source-boundary tests for Catalog image storage.
- [x] Bind `app.catalog.image-dir` through
  `CatalogImageStorageProperties` and inject it into the storage adapter.
- [x] Add failing property and source-boundary tests for WhatsApp settings.
- [x] Bind `app.whatsapp.*` through `WhatsAppProperties`, remove direct
  environment access from the WhatsApp application path, and expose the full
  property set in both deployable application configurations.
- [x] Bind Google Calendar service-account credentials and endpoints through
  `GoogleCalendarProperties`; no Calendar production adapter reads the process
  environment directly.
- [x] Normalize the Assistant Groq API key through `AiProperties`; no
  Assistant production AI provider reads the process environment directly.
- [x] Normalize Notification provider credentials and options through
  `NotificationProperties`; no Notification production provider reads the
  process environment directly.
- [x] Normalize Payment provider credentials and the Mercado Pago webhook
  secret through `PaymentProperties`; no Payment production provider or
  webhook reads the process environment directly.
- [ ] Run the final service-wide verification gate after those provider slices
  are complete.

#### Results

The first three property slices are green at unit, module, and integration
levels. Source checks now prevent new direct `@Value` injection in the
normalized components. Remaining provider environment reads are tracked
separately because each provider has a different existing property model and
secret-redaction contract.

## Identity role/permission domain boundary slice — 2026-08-01

- [x] Add failing domain, mapper, and package-ownership tests for `Role` and
  `Permission`.
- [x] Introduce framework-free `Role`, `Permission`, and `RoleScope` models.
- [x] Add persistence mappers and rewire role/permission adapters through the
  domain models without changing schema or permission results.
- [x] Remove the obsolete `RoleReference` port model after all consumers use
  the domain `Role`.
- [x] Verify Identity tests, Checkstyle, Spotless, integration tests, Modulith,
  CI, boot JARs, Markdown, and whitespace.

#### Results

- Red phase: the new source-tree and domain/mapper tests failed to compile
  because the canonical models and mappers did not yet exist.
- Green/refactor phase: focused domain and mapper tests passed after the
  framework-free models and persistence boundary were introduced.
- Full verification passed for Identity check/integration tests, Studio
  Modulith verification, service CI, both boot JARs, Markdown validation, and
  whitespace checks.
- Known non-blocking dependency-analysis warnings remain in the application
  projects because Spring Boot projects currently apply both `java-library` and
  `org.springframework.boot`.

## Tenancy typed database configuration slice — 2026-08-01

- [x] Add failing source and configuration tests for typed database credentials.
- [x] Introduce `TenantDatabaseConnectionProperties` under the canonical
  `configuration` package, bound to the existing `spring.datasource` keys.
- [x] Replace field-level `@Value` injection in `TenantDatabasePoolProvider`
  with constructor injection of the typed properties.
- [x] Verify Tenancy tests, Checkstyle, Spotless, integration tests, Modulith,
  Markdown, whitespace, CI, and boot JARs.

#### Results

- Red phase: the new source-boundary and properties tests failed to compile
  because `TenantDatabaseConnectionProperties` did not yet exist.
- Green/refactor phase: typed configuration tests and the source-boundary test
  passed after constructor injection replaced field-level `@Value` usage.
- Full verification passed for Tenancy tests/check/integration tests, Studio
  Modulith verification, service CI, both boot JARs, Markdown validation, and
  whitespace checks.
- Integration teardown continues to emit known H2/PostgreSQL and event-
  publication shutdown warnings after successful completion.

## Tenancy provisioning port boundary slice — 2026-08-01

- [x] Add failing process-manager, port-ownership, and adapter tests.
- [x] Introduce application-owned provisioning request and schema-migration
  ports.
- [x] Move JDBC registry lifecycle updates into an outbound persistence adapter.
- [x] Move schema creation and Liquibase execution into an outbound database
  adapter.
- [x] Keep scheduling, correlation, provisioning success/failure transitions,
  and error bounding in `TenantProvisioningProcessManager`.
- [x] Verify Tenancy tests, Checkstyle, Spotless, integration tests, Modulith,
  CI, boot JARs, Markdown, and whitespace.

#### Results

- Red phase: the new process-manager test failed to compile because the
  provisioning ports did not yet exist.
- Green/refactor phase: the process manager now depends only on application
  ports; JDBC and Liquibase implementations are outbound adapters.
- Full verification passed for Tenancy tests/check/integration tests, Studio
  Modulith verification, service CI, both boot JARs, Markdown validation, and
  whitespace checks.
- Integration teardown continues to emit known H2/PostgreSQL and event-
  publication shutdown warnings after successful completion.

## Tenancy provisioning service JDBC boundary slice — 2026-08-01

- [x] Add failing service and source-boundary tests for request/status access.
- [x] Extend `TenantProvisioningRepository` with request creation and status
  lookup capabilities.
- [x] Move request/status SQL into `JdbcTenantProvisioningRepository`.
- [x] Remove `JdbcTemplate` from `TenantProvisioningApplicationService`.
- [x] Verify Tenancy tests, Checkstyle, Spotless, integration tests, Modulith,
  CI, boot JARs, Markdown, and whitespace.

#### Results

- Red phase: the new service test failed to compile because request/status
  capabilities were not yet part of the provisioning repository port.
- Green/refactor phase: request creation and status mapping now use the
  application-owned port; JDBC remains only in the outbound adapter.
- The application implementation was normalized from
  `JdbcTenantProvisioningService` to `TenantProvisioningApplicationService`.
- Focused service/source tests and the full Tenancy/service verification gates
  passed.

## Tenancy pool failure baseline slice — 2026-08-01

- [x] Add deterministic tests for unresolved database routing and empty pool
  lifecycle shutdown.
- [x] Record the failure behavior as part of the Tenancy operational evidence.
- [x] Verify Tenancy tests, Checkstyle, Spotless, Markdown, and whitespace.

#### Results

- An unresolved registry entry fails with the database identifier in the
  exception before a pool is created.
- Empty pool state reports zero active pools and remains safe after shutdown.
- The remaining live routing, eviction, and recovery scenarios remain open for
  integration-level evidence.

## Tenancy event-after-commit boundary slice — 2026-08-01

- [x] Add failing consumer annotation and delegation tests.
- [x] Handle `TenantCreated` through Spring Modulith's application-module
  listener boundary.
- [x] Preserve Identity realm provisioning delegation and public event schema.
- [x] Verify Identity/Tenancy tests, Modulith, CI, boot JARs, Markdown, and
  whitespace.

### Results

- The source-boundary test first failed because the consumer used plain
  `@EventListener` instead of the Modulith listener boundary.
- The consumer now delegates through `@ApplicationModuleListener`, preserving
  the existing event payload and process-manager behavior.
- Identity/Tenancy tests and integration tests, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL teardown warnings
  remain non-blocking and occurred after successful task completion.

## Tenancy default-pool recovery slice — 2026-08-01

- [x] Add a failing regression test for recovery after the default pool closes.
- [x] Replace a closed default pool with a fresh pool on the next lookup.
- [x] Preserve the non-evictable default-pool behavior and safe shutdown.
- [x] Verify Tenancy tests, integration tests, Modulith, CI, boot JARs,
  Markdown, and whitespace.

### Results

- The regression test first failed because the provider returned the same closed
  Hikari pool after external shutdown.
- The provider now replaces the exact stale default-pool reference with a fresh
  pool, including the concurrent compare-and-set path.
- Focused Tenancy tests, Tenancy check/integration tests, Studio Modulith
  verification, service CI, both boot JARs, Markdown validation, and
  `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL teardown warnings
  remain non-blocking and occurred after successful task completion.

## Identity exception-advice boundary slice — 2026-08-01

- [x] Add a failing source-boundary test for controller-independent exception
  advice.
- [x] Scope Identity exception advice by the inbound web controller package,
  not by importing a concrete controller.
- [x] Preserve existing Identity problem-detail status and error-code behavior.
- [x] Verify Identity tests, Modulith, CI, boot JARs, Markdown, and whitespace.

### Results

- The source-boundary test first failed because `IdentityExceptionHandler`
  imported `IdentityController` through `basePackageClasses`.
- The advice now uses the controller package name, removing concrete
  controller coupling while preserving its RFC 9457 problem details.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL teardown warnings
  remain non-blocking and occurred after successful task completion.

## Identity distributed login-rate-limit slice — 2026-08-01

- [x] Add failing tests for an application-owned login rate-limit port.
- [x] Move attempt state out of `LoginRateLimitFilter`.
- [x] Add an atomic Redis-backed adapter and a local fallback for environments
  without Redis.
- [x] Preserve trusted-proxy client-key resolution and HTTP 429 behavior.
- [x] Verify Identity security tests, Modulith, CI, boot JARs, Markdown, and
  whitespace.

### Results

- Red phase: tests failed to compile because the application-owned limiter port
  and injected filter boundary did not yet exist.
- Green/refactor phase: the filter now delegates to `LoginAttemptRateLimiter`,
  Redis uses an atomic increment-plus-expiry Lua script, and environments
  without Redis use an explicit local fallback.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL teardown warnings
  remain non-blocking and occurred after successful task completion.

## Identity provisioning inbound-port slice — 2026-08-01

- [x] Add failing tests for the tenant-created consumer's inbound use-case
  dependency.
- [x] Expose `ProvisionTenantIdentityUseCase` under the grouped Identity API.
- [x] Make the provisioning process manager implement the use case.
- [x] Make the Modulith consumer depend on the use-case abstraction, preserving
  after-commit behavior and provisioning semantics.
- [x] Verify Identity security tests, Modulith, CI, boot JARs, Markdown, and
  whitespace.

### Results

- Red phase: the consumer test failed to compile because the inbound use-case
  contract did not yet exist.
- Green/refactor phase: the consumer now depends on
  `ProvisionTenantIdentityUseCase`; the process manager remains the concrete
  application implementation.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL teardown warnings
  remain non-blocking and occurred after successful task completion.

## Identity membership web-boundary slice — 2026-08-01

- [x] Add failing tests preventing membership web adapters from importing
  application implementations or domain models.
- [x] Add grouped membership commands, query, use-case contracts, and result
  mapping.
- [x] Refactor Identity and current-user controllers to consume public use cases.
- [x] Preserve membership routes, status codes, response fields, and tenant
  selection behavior.
- [x] Verify Identity web/security tests, Modulith, CI, boot JARs, Markdown, and
  whitespace.

### Results

- Red phase: the source-boundary test failed because the membership controllers
  and web mapper imported `MembershipService` and the domain `Membership` type.
- Green/refactor phase: grouped membership commands, query, use cases, and
  `MembershipInfo` mapping now form the web-facing boundary. Controllers depend
  only on public use-case contracts and the web mapper depends only on the
  public result model.
- Membership routes, response status codes, response fields, and tenant
  selection behavior remain unchanged.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL shutdown warnings
  remain non-blocking and occurred after successful task completion.

## Identity feature-flag web-boundary slice — 2026-08-01

- [x] Add failing tests preventing feature-flag web adapters from importing
  application implementations or domain models.
- [x] Add grouped feature-flag commands, query, use-case contracts, and result
  mapping.
- [x] Refactor platform and tenant feature-flag controllers to consume public
  use cases.
- [x] Preserve feature-flag routes, response fields, effective-value behavior,
  and authorization behavior.
- [x] Verify Identity tests/check/integration, Modulith, CI, boot JARs, Markdown,
  and whitespace.

### Results

- Red phase: the source-boundary test failed because both feature-flag
  controllers imported `FeatureFlagService`, and the web mapper imported the
  domain `FeatureFlag` model.
- Green/refactor phase: feature-flag commands, query, use cases, and public
  results now isolate the web adapters from application implementations and
  domain models. Effective values are returned through an immutable result.
- Feature-flag routes, JSON fields, effective override behavior, and platform
  admin authorization remain unchanged.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL shutdown warnings
  remain non-blocking and occurred after successful task completion.

## Identity appointment-consumer inbound-port slice — 2026-08-01

- [x] Add a failing source-boundary test for the appointment consumer.
- [x] Add a grouped customer-membership command and use-case contract.
- [x] Make the application service implement the contract and decouple the
  consumer from the concrete service.
- [x] Preserve CUSTOMER-role filtering, JWT subject parsing, idempotency, and
  appointment event behavior.
- [x] Verify Identity tests/check/integration, Modulith, CI, boot JARs, Markdown,
  and whitespace.

### Results

- Red phase: the architecture test failed because the appointment consumer
  imported `EnsureCustomerMembershipService` directly.
- Green/refactor phase: the consumer now depends on the grouped
  `EnsureCustomerMembershipUseCase` and sends an
  `EnsureCustomerMembershipCommand`; the application service remains the
  idempotent implementation.
- CUSTOMER-role filtering, JWT subject parsing, and membership persistence
  behavior remain unchanged.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL shutdown warnings
  remain non-blocking and occurred after successful task completion.

## Identity authentication-configuration port slice — 2026-08-01

- [x] Add a failing architecture test preventing application authentication
  services from importing Spring configuration properties.
- [x] Add an application-owned realm-configuration port and composition-root
  adapter.
- [x] Refactor `AuthenticateUserService` and its tests to consume the port.
- [x] Preserve platform and tenant realm selection behavior.
- [x] Verify Identity tests/check/integration, Modulith, CI, boot JARs, Markdown,
  and whitespace.

### Results

- Red phase: the architecture test failed because `AuthenticateUserService`
  imported `IdentityKeycloakProperties` directly.
- Green/refactor phase: the service now depends on the application-owned
  `IdentityRealmConfigurationPort`; `IdentityClientConfiguration` adapts the
  typed Spring properties at the composition root.
- Platform and tenant realm selection behavior remains unchanged.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL shutdown warnings
  remain non-blocking and occurred after successful task completion.

## Identity provisioning-configuration port slice — 2026-08-01

- [x] Add a failing architecture test preventing provisioning orchestration
  from importing Spring configuration properties.
- [x] Add an application-owned provisioning configuration port and immutable
  settings model.
- [x] Adapt typed provisioning properties in the configuration root.
- [x] Refactor `KeycloakRealmProvisioningProcessManager` and its tests to consume
  the application boundary.
- [x] Preserve retry, validation, realm, client, role, and admin-user behavior.
- [x] Verify Identity tests/check/integration, Modulith, CI, boot JARs, Markdown,
  and whitespace.

### Results

- Red phase: the architecture test failed because
  `KeycloakRealmProvisioningProcessManager` imported
  `IdentityRealmProvisioningProperties` directly.
- Green/refactor phase: provisioning now consumes
  `IdentityRealmProvisioningConfigurationPort` and immutable
  `IdentityRealmProvisioningSettings`; Spring properties are mapped only in
  `IdentityProvisioningConfiguration`.
- Retry, validation, realm, client, role, and admin-user behavior remain
  unchanged.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL shutdown warnings
  remain non-blocking and occurred after successful task completion.

## Identity provisioning command boundary slice — 2026-08-01

- [x] Add a failing architecture test preventing the Identity use case from
  importing the Tenancy event transport type.
- [x] Add `ProvisionTenantIdentityCommand` to the grouped Identity API.
- [x] Map `TenantCreated` to the Identity command in the inbound consumer.
- [x] Refactor the provisioning use case and process manager to consume the
  Identity-owned command.
- [x] Preserve after-commit provisioning, realm naming, and all provisioning
  payload values.
- [x] Verify focused Identity tests, formatting, and source-boundary checks.

### Results

- Red phase: the architecture test failed because
  `ProvisionTenantIdentityUseCase` imported `TenantCreated` directly.
- Green/refactor phase: the inbound consumer now translates the cross-module
  event into `ProvisionTenantIdentityCommand`; Identity application code no
  longer depends on the Tenancy event transport type.
- The `@ApplicationModuleListener` after-commit entry point and provisioning
  behavior remain unchanged.
- Focused Identity tests and Spotless formatting passed.

## Identity unreleased API cleanup — 2026-08-01

- [x] Remove the legacy `IdentityApi` use-case contract and
  `IdentityApiService` implementation.
- [x] Remove the temporary all-memberships compatibility query/use-case that
  existed only to preserve the legacy implementation.
- [x] Add an executable source-tree rule rejecting the removed legacy API
  files.
- [x] Record the unreleased-system rule in the architecture handbook,
  templates, naming catalog, and engineering lessons.
- [x] Verify Identity compilation/tests, Modulith, service CI, both boot JARs,
  formatting, Markdown, and whitespace.

### Results

- The unreleased service now uses only canonical grouped API contracts; no
  compatibility alias or legacy Identity API implementation remains.
- The architecture rule is explicit: compatibility layers require an external
  released consumer, persisted/serialized contract, or approved migration
  window with documented evidence.

## Identity tenant-realm outbound port slice — 2026-08-01

- [x] Add a failing architecture test preventing provisioning orchestration
  from importing the Tenancy application service contract directly.
- [x] Add the application-owned `TenantIdentityRealmPort`.
- [x] Add `TenantIdentityRealmAdapter` under the outbound module adapter and
  delegate to the Tenancy public contract there.
- [x] Refactor `KeycloakRealmProvisioningProcessManager` and tests to consume
  the outbound port.
- [x] Add adapter delegation regression coverage and package metadata.
- [x] Verify focused Identity tests and Spotless formatting.

### Results

- Red phase: the source-boundary test and adapter test failed because the
  provisioning process had no application port or adapter implementation.
- Green/refactor phase: Tenancy realm updates now cross the explicit
  `TenantIdentityRealmPort`; only the outbound adapter imports `TenantApi`.
- Realm provisioning behavior and after-commit listener ownership remain
  unchanged.

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
- [x] Normalize Assistant persistence names, inbound adapter paths, and initial
  domain/API package boundaries.
- [ ] Replace temporary entity-backed orchestration with focused use-case
  services and application-owned ports.
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

## Catalog canonical baseline verification slice — 2026-08-01

- [x] Added package-level metadata and a convention test for every materialized
  Catalog package while preserving the Modulith root and named API annotations.
- [x] Hid Shared hybrid search behind Catalog-owned `CatalogSearchPort` and
  `HybridCatalogSearchAdapter`, with mapping coverage.
- [x] Corrected the shared JDBC connection-details boundary so Testcontainers
  `@ServiceConnection` integration tests receive the bootstrap URL.
- [x] Removed proxy-blocking `final` declarations from Identity persistence
  adapters and corrected stale Studio tenant accessor tests discovered by CI.
- [x] Verified Catalog tests/integration, Studio Modulith, service CI, boot JARs,
  Markdown validation, source-boundary checks, and whitespace.
- [x] Updated the plan registry and Catalog verification report.

## Identity trusted-proxy rate-limit slice — 2026-08-01

- [x] Add failing tests for typed rate-limit settings and forwarded-header
  spoofing resistance.
- [x] Replace `@Value` rate-limit fields with `IdentityRateLimitProperties`.
- [x] Accept `X-Forwarded-For` only when the immediate peer matches configured
  trusted proxy networks; preserve the remote address as the secure default.
- [x] Document the decision in an ADR and the Identity migration plan.
- [x] Verify focused Identity tests, full Identity checks, Identity integration,
  Modulith, CI, Markdown, whitespace, and boot-JAR gates.

### Results

- Focused typed-properties and forwarded-header filter tests passed.
- `:modules:identity:check` passed.
- `:modules:identity:integrationTest` passed; teardown emitted existing
  PostgreSQL/Testcontainers shutdown I/O warnings after test completion.
- Studio Modulith verification, service CI, both application boot JARs,
  Markdown validation, and `git diff --check` passed.

## Identity persistence entity naming slice — 2026-08-01

- [x] Add and run the failing source-tree test for normalized `*Entity` names.
- [x] Rename Identity role/permission JPA types and update all repository,
  adapter, mapper, fixture, and integration-test references.
- [x] Verify Identity tests, integration tests, Modulith, CI, boot JARs,
  Markdown, and whitespace.

### Results

- The convention test first failed on the missing normalized entity files.
- Identity unit tests, Checkstyle, Spotless, and integration tests passed.
- Studio Modulith verification, service CI, both application boot JARs,
  Markdown validation, and `git diff --check` passed.
- Integration teardown emitted existing Testcontainers/PostgreSQL shutdown
  warnings after successful test completion.

## Identity inbound security-context ownership slice — 2026-08-01

- [x] Add and run the failing source-tree test for moving `UserContext` and
  `UserContextHolder` out of the Identity root package.
- [x] Move the security-context types under `adapter/in/web/security` and
  update Identity and Calendar consumers without changing behavior.
- [x] Verify Identity tests, Calendar tests, Modulith, CI, boot JARs,
  Markdown, and whitespace.

### Results

- The source-tree test first failed because the canonical security package was
  absent.
- Moved `UserContext` and `UserContextHolder` under inbound web security and
  exposed only the `identity-security` named interface for Calendar.
- Identity and Calendar unit/integration checks, Modulith verification, service
  CI, both boot JARs, Markdown validation, and whitespace checks passed.
- Testcontainers teardown emitted existing database/prune warnings after
  successful completion.

## Identity authorization wiring separation slice — 2026-08-01

- [x] Add and run failing tests for extracted role-authority mapping and
  configuration ownership.
- [x] Move JWT/OIDC authority mapping and role hierarchy wiring into dedicated
  Identity authorization components.
- [x] Keep `SecurityConfiguration` focused on filter-chain and transport
  security wiring without changing role names or access behavior.
- [x] Verify Identity security tests, integration tests, Modulith, CI, boot
  JARs, Markdown, and whitespace.

### Results

- The source-tree convention test first failed because the canonical
  authorization components were absent.
- Extracted role claim parsing, JWT conversion, OAuth2/OIDC authority mapping,
  role hierarchy, and method-security wiring from `SecurityConfiguration`.
- Preserved `ROLE_` prefixing, the existing role hierarchy, OIDC `userinfo`
  support, and all existing filter-chain behavior.
- Identity unit/check/integration tests, Studio Modulith verification, service
  CI, both application boot JARs, Markdown validation, and `git diff --check`
  passed.
- Integration teardown emitted existing PostgreSQL/Testcontainers shutdown
  warnings after successful completion.
