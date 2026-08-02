# Identity Module Migration Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. Security behavior changes require focused regression evidence before each structural move.

**Goal:** Migrate Identity's security, membership, feature-flag, persistence, web,
and Keycloak code to the current module template while preserving authentication,
authorization, tenant membership, rate limiting, and public API behavior.

**Architecture:** Identity is a security-heavy business module. Domain membership,
role, permission, identity, and feature-flag models are framework-free. JPA
representations are adapter-owned. Public results/use cases are grouped under
`api`; HTTP/security entry points are inbound adapters; Keycloak and security
infrastructure are explicit outbound/client or inbound-filter adapters.

## Current inventory

```text
com.emme.identity
├── UserContext/UserContextHolder.java
├── api/{IdentityApi,MembershipInfo,UserInfo}
├── application/{IdentityService,CustomerAuthService,FeatureFlagService,KeycloakAuthService,CustomerMembershipListener}
├── configuration/SecurityConfiguration.java
├── entity/{identity,membership,role,permission,feature-flag entities/repositories}
├── infrastructure/{Keycloak clients, JWT decoder, rate-limit filter, audit logger}
├── service/IdentityApiImpl.java
└── web/{auth,current-user,identity,feature-flag controllers}
```

## Target ownership

```text
com.emme.identity
├── api/{command,query,result,usecase,event,exception,type}
├── application/{service,port/out,mapper}
├── domain/{model,service,event,exception}
├── adapter/in/{web,webhook,messaging/consumer}
│   └── web/filter/ security-owned inbound filters
├── adapter/out/{persistence,client/keycloak,observability}
└── configuration/{SecurityConfiguration,IdentityProperties}
```

`UserContext` and security principals are classified explicitly during inventory:
they may remain in a small inbound security package when they are transport
context, but they must not become business API types accidentally.

## Public contract and naming decisions

- `IdentityApi` becomes a use-case/inbound contract or a named interface grouping
  only the existing cross-module methods; do not expose implementation services.
- `MembershipInfo` and `UserInfo` move to `api/result`.
- Membership/feature-flag mutations use `*Command`; reads use `*Query`; stable
  failures use `*Exception`; emitted membership facts use past-tense `api/event`.
- `KeycloakAuthService` becomes one or more application services implementing
  use-case interfaces; transport token records stay under web response/client
  packages, not domain or public API.

## Tasks

### Task 1: Security and consumer inventory

- [x] Map every import of `com.emme.identity` across modules and applications.
- [x] Record authentication endpoints, current-user endpoints, membership paths,
  feature-flag paths, filters, listeners, and security bean wiring.
- [x] Capture baseline tests for successful login, invalid login, authorization,
  tenant mismatch, rate limiting, JWT decoding, and feature flags.

### Task 2: Architecture guardrails

- [x] Add `IdentityPackageConventionTest` and ArchUnit rules for domain isolation,
  application ports, inbound filters/controllers, persistence containment, and
  named API closure.
- [x] Add package-info to every materialized target package.
- [ ] Keep root `@ApplicationModule` allowed dependencies explicit and unchanged
  until consumer migrations are completed.

### Task 3: Domain and persistence

- [ ] Separate `CustomerIdentity`, `Membership`, `CustomerMembership`, `Role`,
  `Permission`, `RolePermission`, `FeatureFlag`, and related enums into pure domain
  models where they carry business behavior.
- [ ] Create corresponding `*Entity` persistence classes, Spring Data repositories,
  application ports, persistence adapters, and mappers.
- [ ] Preserve schema, tenant filtering, role/permission relationships, and
  managed-entity updates.

### Task 4: Application ports and use cases

- [ ] Split `IdentityService`, `CustomerAuthService`, and `FeatureFlagService`
  responsibilities into named application services and public use-case contracts.
- [ ] Move repository interfaces to `application/port/out`.
- [ ] Move `CustomerMembershipListener` to `adapter/in/messaging/consumer` and
  make it invoke a use case rather than repositories directly.
- [ ] Keep Keycloak operations behind outbound ports.

### Task 5: Security and Keycloak adapters

- [x] Move `SecurityConfig` to `configuration/SecurityConfiguration`.
- [x] Classify `KeycloakAdminClient`, `KeycloakRealmProvisioner`, and
  `MultiRealmJwtDecoder` under `adapter/out/client/keycloak` or configuration
  only when their responsibility is bean wiring.
- [x] Move `LoginRateLimitFilter` to `adapter/in/web/filter`.
- [x] Move `SecurityAuditLogger` to the appropriate inbound/outbound observability
  package without changing emitted audit behavior.
- [ ] Keep secrets and realm configuration in typed configuration properties.

### Task 6: HTTP adapters and public API

- [x] Extract request/response records and web mappers from all controllers.
- [x] Move controllers to `adapter/in/web/controller`; Identity-owned exception
  advice remains a follow-up because the current controllers preserve existing
  response behavior without a module-specific handler.
- [x] Update all cross-module imports to named API packages in the same commits as
  contract moves.
- [x] Preserve route paths, status codes, token response shapes, tenant behavior,
  and security annotations.
- [ ] Add module advice only for Identity-owned expected failures.

### Task 7: Verification and hardening

- [ ] Run Identity unit, web, integration, architecture, Modulith, formatting,
  Checkstyle, CI, and boot-JAR checks.
- [ ] Test tenant isolation, privilege escalation resistance, JWT issuer/audience
  validation, login-rate-limit behavior, secret redaction, audit correlation IDs,
  and idempotent membership events.
- [ ] Record migration/recovery and rollback evidence before marking complete.

## Definition of done

- [ ] No legacy mixed Identity package remains; compatibility exceptions are
  not permitted for this unreleased service.
- [ ] Security boundaries are executable and no persistence entity leaks into API
  or web responses.
- [ ] Existing authentication and authorization behavior is preserved.

## Completed incremental slice — 2026-07-31

- [x] Grouped the existing public use-case contract under `api/usecase`.
- [x] Grouped `MembershipInfo` and `UserInfo` under `api/result`.
- [x] Preserved the `identity-api` named-interface identifier while moving its
  ownership to the grouped packages.
- [x] Updated Identity's Tenancy contract imports and added a source-tree
  convention test.
- [x] Verified Identity tests and Studio Modulith verification.

The security/domain/persistence migration remains open; this slice does not
claim the full Identity plan is complete.

## Completed persistence-ownership slice — 2026-07-31

- [x] Moved Identity JPA entities and persistence enums under
  `adapter/out/persistence/entity`.
- [x] Moved Spring Data repository interfaces under
  `adapter/out/persistence/repository`.
- [x] Added package metadata and source-tree ownership tests for the new
  outbound persistence boundary.
- [x] Updated production, test-fixture, and consumer imports without changing
  database mappings or public behavior.
- [x] Verified module tests, Checkstyle, Spotless, compilation, and Studio
  Modulith verification.

Application-owned repository ports, pure domain models, and persistence mappers
remain future slices; this move intentionally preserves behavior while making
the current technical ownership explicit.

## Completed security-boundary slice — 2026-08-01

- [x] Moved `SecurityConfig` to `configuration/SecurityConfiguration`.
- [x] Moved `LoginRateLimitFilter` to `adapter/in/web/filter`.
- [x] Moved the JWT decoder and Keycloak admin client to
  `adapter/out/client/keycloak`.
- [x] Moved `SecurityAuditLogger` to `adapter/out/observability`.
- [x] Split tenant-created realm provisioning into
  `adapter/in/messaging/consumer/TenantCreatedConsumer` and
  `application/process/KeycloakRealmProvisioningProcessManager`.
- [x] Preserved routes, Spring bean wiring, event handling, and external test
  doubles; no security behavior was intentionally changed.
- [x] Verified focused package tests, full Identity check, and Studio Modulith
  verification.

The Identity HTTP contract extraction and application/domain separation remain
open tasks; this slice does not claim the full Identity plan is complete.

## Completed HTTP-boundary slice — 2026-08-01

- [x] Moved Identity controllers and the web-slice test under
  `adapter/in/web/controller`.
- [x] Extracted HTTP request records under `adapter/in/web/request` with
  resource-specific names such as `CreateFeatureFlagRequest` and
  `OverrideFeatureFlagRequest`.
- [x] Extracted HTTP response records under `adapter/in/web/response`.
- [x] Added `IdentityWebMapper` and `FeatureFlagWebMapper` for transport mapping.
- [x] Preserved existing route paths, response fields, status codes, validation
  annotations, authorization annotations, and customer-login behavior.
- [x] Added source-tree guardrails for controller/request/response/mapper
  ownership and removed Java sources from the legacy `web` package.
- [x] Verified the full Identity test suite and formatting; application Modulith
  verification remains part of the final cross-module gate.

Application service/domain separation, controller-to-controller orchestration,
and Identity-specific failure advice remain future slices.

## Completed Membership domain/application slice — 2026-08-01

- [x] Introduced the framework-free `domain/model/Membership` aggregate and
  `MembershipStatus` lifecycle vocabulary.
- [x] Moved membership persistence representation to `MembershipEntity` and
  technical Spring Data types to `SpringDataMembershipRepository` and
  `SpringDataRoleRepository` to distinguish adapters from application ports.
- [x] Added application-owned `MembershipRepository` and `RoleRepository`
  outbound ports with `MembershipService` orchestration.
- [x] Added `MembershipPersistenceMapper`, `MembershipPersistenceAdapter`,
  and `RolePersistenceAdapter`; managed entities are updated in place when an
  existing identifier is saved.
- [x] Rewired Identity membership HTTP and public API flows to consume domain
  models/application services while preserving routes, response fields, and
  database schema.
- [x] Added domain lifecycle, package ownership, and persistence mapper tests.
- [x] Verified the full Identity test suite and test compilation.

The remaining Identity work is the permission/identity/authentication
application split, feature-flag ownership, typed security configuration, and
Identity-specific failure advice.

## Completed permission application slice — 2026-08-01

- [x] Added `PermissionPort` as the application-owned authorization boundary.
- [x] Added `GetUserPermissionsUseCase` and
  `GetUserPermissionsService` under the grouped API/application packages.
- [x] Added `PermissionPersistenceAdapter`, which keeps JPA membership,
  role-permission, and permission traversal outside application services.
- [x] Rewired the Identity and current-user controllers to the use-case
  contract; no controller imports the legacy `IdentityService`.
- [x] Removed the legacy mixed-responsibility `IdentityService`.
- [x] Preserved active-membership and tenant filtering semantics and the
  existing `Set<String>` HTTP response shape.
- [x] Added unit, source-tree, and integration coverage for permission
  resolution.

Customer authentication, typed security configuration, and Identity-specific
failure advice remain open slices.

## Completed feature-flag application slice — 2026-08-01

- [x] Introduced the framework-free `domain/model/FeatureFlag` model with
  explicit enabled-state behavior.
- [x] Added application-owned `FeatureFlagRepository` and
  `SubscriptionPlanPort` contracts.
- [x] Moved feature-flag orchestration to `application/service/FeatureFlagService`
  while preserving the `featureFlagService` bean name used by SpEL guards.
- [x] Renamed technical persistence types to `FeatureFlagEntity` and
  `SpringDataFeatureFlagRepository`.
- [x] Added `FeatureFlagPersistenceMapper`, `FeatureFlagPersistenceAdapter`,
  and `SubscriptionPlanAdapter`.
- [x] Rewired feature-flag controllers, shared test fixtures, and Assistant
  tests without changing routes, payloads, plan gating, override precedence,
  or schema names.
- [x] Added domain, application, mapper, and package-boundary coverage.
- [x] Verified the full Identity test suite and compilation of Assistant test
  sources.

## Completed customer-membership event slice — 2026-08-01

- [x] Introduced the framework-free `domain/model/CustomerMembership` model.
- [x] Added the application-owned `CustomerMembershipRepository` port and
  idempotent `EnsureCustomerMembershipService`.
- [x] Renamed the technical composite-key types to
  `CustomerMembershipEntity`, `CustomerMembershipId`, and
  `SpringDataCustomerMembershipRepository`.
- [x] Added `CustomerMembershipPersistenceMapper` and
  `CustomerMembershipPersistenceAdapter` so application code does not depend
  on JPA types.
- [x] Moved appointment handling to the inbound
  `adapter/in/messaging/consumer/AppointmentCreatedConsumer` and preserved the
  existing JWT customer-role filtering behavior.
- [x] Added service, mapper, consumer, and source-tree boundary tests.

## Completed customer authentication slice — 2026-08-01

- [x] Introduced the framework-free `CustomerIdentity` aggregate and
  `SocialProvider` vocabulary.
- [x] Added grouped customer authentication/profile commands, results, and
  public use-case interfaces.
- [x] Added `CustomerIdentityRepository`, `CustomerTokenDecoder`, and
  transport-neutral `CustomerTokenClaims` application ports.
- [x] Moved customer authentication and phone updates into
  `AuthenticateCustomerService` and `UpdateCustomerProfileService`.
- [x] Renamed technical persistence types to `CustomerIdentityEntity` and
  `SpringDataCustomerIdentityRepository`, with mapper and persistence adapter.
- [x] Added `CustomerTokenDecoderAdapter` to keep Keycloak JWT types outside the
  application service.
- [x] Rewired `AuthController` through public use cases and web response mapper
  types without exposing persistence entities.
- [x] Preserved customer-realm issuer validation, provider fallback, profile
  update behavior, route paths, and response fields.
- [x] Added domain, application, persistence-mapper, package-boundary, and
  invalid-realm regression coverage.

Typed security configuration, Identity-specific failure advice, and Keycloak
boundary slices are recorded below; the broader security hardening gate remains.

## Completed typed security configuration slice — 2026-08-01

- [x] Added `IdentitySecurityProperties` under `configuration` with typed CORS,
  CSP, logout, and cache settings.
- [x] Preserved the existing local-development defaults and security behavior.
- [x] Rewired `SecurityConfiguration` to consume typed settings instead of
  embedding environment-specific values in the security bean wiring.
- [x] Added property-default and package-ownership tests.
- [x] Verified Identity checks, Studio Modulith verification, Markdown
  validation, and diff whitespace checks.

Identity-specific failure advice and the Keycloak boundary slices are recorded
below; the broader security hardening gate remains.

## Completed Identity failure advice slice — 2026-08-01

- [x] Added public `InvalidCustomerTokenException` and
  `CustomerNotFoundException` types under `api/exception`.
- [x] Added scoped `IdentityExceptionHandler` under the inbound web advice
  adapter using RFC 9457 `ProblemDetail` responses and stable error codes.
- [x] Rewired customer authentication/profile services to raise the typed
  Identity failures while preserving existing controller translations.
- [x] Added exception mapping and package-ownership coverage.
- [x] Verified Identity checks, Studio Modulith verification, Markdown
  validation, and whitespace checks.

The following Keycloak boundary and typed-configuration slices preserve the
existing authentication behavior while completing the remaining structural
separation.

## Completed Keycloak application boundary slice — 2026-08-01

- [x] Added grouped `AuthenticateUserCommand`, `GetUserInfoQuery`,
  `UserTokenResult`, `UserInfoResult`, and `AuthenticateUserUseCase` contracts.
- [x] Added the application-owned `UserAuthenticationPort` and
  `AuthenticateUserService`; tenant-realm selection remains application-owned.
- [x] Moved password-grant and user-info HTTP calls into
  `KeycloakUserAuthenticationAdapter`.
- [x] Moved `OkHttpClient` construction to `IdentityClientConfiguration` and
  removed direct Keycloak transport orchestration from the application layer.
- [x] Rewired `AuthController` through `AuthenticateUserUseCase` and removed
  the legacy `KeycloakAuthService`.
- [x] Preserved the distinction between invalid credentials (`401`) and an
  unavailable authentication provider (`500`).
- [x] Added application-service, package-boundary, and login regression
  coverage.
- [x] Verified Identity checks, Studio Modulith verification, Markdown
  validation, and whitespace checks.

- [x] Typed user, admin, issuer, realm, and client settings under
  `IdentityKeycloakProperties` and updated application/test profiles.
- [x] Injected the configured Identity HTTP client into both Keycloak adapters;
  neither adapter constructs transport clients or reads `@Value` settings.
- [x] Added typed-property, package-boundary, and shared-test-fixture coverage.
- [x] Verified Identity checks, AuthWebTest login regressions, Studio Modulith
  verification, Markdown validation, and whitespace checks.

Remaining follow-up: complete the broader Identity security hardening review.

## Completed realm provisioning port slice — 2026-08-01

- [x] Added `IdentityProviderAdministrationPort` under
  `application/port/out` with only the realm, client, role, and user operations
  required by provisioning.
- [x] Rewired `KeycloakRealmProvisioningProcessManager` to depend on the
  application port instead of `KeycloakAdminClient`.
- [x] Kept Keycloak HTTP administration inside the outbound adapter, which now
  implements the port.
- [x] Added process behavior and source-tree boundary coverage.
- [x] Verified Identity checks, Studio Modulith verification, Markdown
  validation, and whitespace checks.

Remaining follow-up: review provisioning secrets, retry policy, and the wider
Identity security hardening surface.

## Completed realm provisioning hardening slice — 2026-08-01

- [x] Added typed `IdentityRealmProvisioningProperties` for realm client,
  redirect URI, role, admin-user, and retry settings.
- [x] Removed the production hardcoded tenant-user password and fail fast when
  the provisioning password is not configured.
- [x] Moved retry timing behind `RetryDelayPort`; production wiring uses the
  configured delay while tests use a no-op delay.
- [x] Removed the default master Keycloak admin password and exposed runtime
  environment variables for sensitive credentials.
- [x] Added configuration, retry, secret, and source-boundary regression tests.
- [x] Verified Identity checks, Studio Modulith verification, Markdown
  validation, whitespace checks, and the security source scan.

Remaining follow-up: complete the wider Identity security hardening surface.

## Completed trusted-proxy rate-limit slice — 2026-08-01

- [x] Replaced field-level `@Value` login rate-limit settings with typed
  `IdentityRateLimitProperties` under `configuration`.
- [x] Made the secure direct-deployment default explicit: the limiter uses the
  socket peer address and ignores `X-Forwarded-For` unless the peer matches a
  configured trusted proxy network.
- [x] Preserved the existing login route, default limit, response status, and
  in-memory limiter behavior.
- [x] Added focused configuration and filter regression tests for spoofed and
  trusted forwarded headers.
- [x] Recorded the security decision in
  `docs/adr/0003-identity-login-rate-limit-client-ip.md`.

The remaining Identity work is distributed rate-limit state, broader
authorization review, and the final production-readiness evidence gate.

## Current migration status — 2026-08-01

The current Identity baseline includes typed security and provisioning
configuration, separated provider ports and adapters, redacted audit output,
and a trusted-proxy boundary for login rate limiting. The remaining scope is
distributed rate-limit state, broader authorization domain/application
separation, and the final production-readiness evidence gate.

## Completed security audit hardening slice — 2026-08-01

- [x] Added regression coverage for audit failure redaction,
  control-character sanitization, bounded log values, and forwarded-header
  spoofing.
- [x] Changed authentication-failure audit output to record the exception type,
  never the exception message.
- [x] Sanitized and bounded principals, authorities, decisions, URIs, and
  client addresses before logging them.
- [x] Changed audit IP attribution to use the socket peer address rather than
  trusting an unverified `X-Forwarded-For` header.
- [x] Verified Identity checks, Studio Modulith verification, Markdown
  validation, whitespace checks, and the source-level secret scan.

Remaining follow-up: decide trusted proxy handling for the login rate-limit
filter, then continue authorization domain/application separation.

## Completed persistence entity naming slice — 2026-08-01

- [x] Renamed the role, permission, and role-permission JPA representations to
  `RoleEntity`, `PermissionEntity`, and `RolePermissionEntity`.
- [x] Updated Spring Data repositories, persistence adapters, membership
  mapping, repository tests, and module integration fixtures.
- [x] Added a source-tree regression test that rejects the ambiguous legacy
  entity names.
- [x] Preserved the existing table names, columns, relationships, and runtime
  behavior.

The remaining Identity work is distributed rate-limit state, broader
authorization domain/application separation, and the final production-readiness
evidence gate.

## Completed one-service-per-use-case normalization — 2026-08-01

- [x] Added a source-level convention test rejecting application services that
  implement multiple use-case interfaces.
- [x] Replaced the membership facade with
  `AssignMembershipService`, `GetCurrentUserMembershipsService`, and
  `RevokeMembershipService`.
- [x] Replaced the feature-flag facade with focused services for evaluation,
  effective-flag queries, platform updates, and tenant overrides.
- [x] Extracted membership and feature-flag mapping into application-owned
  mappers while preserving public results and the `featureFlagService` SpEL
  bean name.
- [x] Verified Identity formatting and tests, including the new convention and
  focused feature-flag service tests.

This is a cohesion and interface-segregation rule, not a circular-dependency
workaround. Circular dependencies remain prohibited and must be resolved with
ports, events, or focused collaborators.

## Completed membership web-boundary slice — 2026-08-01

- [x] Added grouped membership commands, query, use-case contracts, and
  application result mapping.
- [x] Refactored Identity and current-user controllers and the web mapper away
  from `MembershipService` and `domain.model.Membership`.
- [x] Preserved membership routes, response fields, status codes, and tenant
  selection behavior.
- [x] Added source-boundary regression coverage and normalized the query use-case
  operation name to `getMemberships`.
- [x] Verified Identity tests/check/integration, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and whitespace checks.

Remaining Identity web normalization includes FeatureFlag adapter result mapping,
then architecture dependency rules and final authorization, tenant-isolation,
and migration/recovery evidence.

## Completed feature-flag web-boundary slice — 2026-08-01

- [x] Added grouped feature-flag commands, query, use-case contracts, and
  public result models.
- [x] Refactored platform and tenant feature-flag controllers and the web
  mapper away from `FeatureFlagService` and `domain.model.FeatureFlag`.
- [x] Preserved routes, JSON response fields, effective override behavior, and
  platform-admin authorization.
- [x] Added source-boundary and application-result regression coverage.
- [x] Verified Identity tests/check/integration, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and whitespace checks.

Remaining Identity work is architecture dependency evidence, broader
authorization hardening, explicit provisioning transaction/event ports, and
final tenant-isolation and migration/recovery evidence.

## Completed provisioning-configuration port slice — 2026-08-01

- [x] Added the application-owned
  `IdentityRealmProvisioningConfigurationPort` and immutable
  `IdentityRealmProvisioningSettings` model.
- [x] Mapped typed Spring provisioning properties in
  `IdentityProvisioningConfiguration`.
- [x] Refactored `KeycloakRealmProvisioningProcessManager` away from Spring
  configuration-property types.
- [x] Preserved retry, validation, realm, client, role, and admin-user
  provisioning behavior.
- [x] Added source-boundary and process-manager regression coverage.
- [x] Verified Identity tests/check/integration, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and whitespace checks.

Remaining Identity work is architecture dependency evidence, broader
authorization hardening, explicit provisioning transaction/event ports, and
final tenant-isolation and migration/recovery evidence.

## Completed provisioning command boundary slice — 2026-08-01

- [x] Added `ProvisionTenantIdentityCommand` to the grouped Identity API.
- [x] Mapped the cross-module `TenantCreated` event in the inbound consumer
  instead of exposing it through the Identity use-case contract.
- [x] Refactored `ProvisionTenantIdentityUseCase` and
  `KeycloakRealmProvisioningProcessManager` to consume the Identity-owned
  command.
- [x] Preserved after-commit listener behavior, realm naming, and provisioning
  payload values.
- [x] Added source-boundary and consumer/process regression coverage.
- [x] Verified focused Identity tests and Spotless formatting.

Remaining Identity work is architecture dependency evidence, broader
authorization hardening, explicit provisioning transaction/event ports, and
final tenant-isolation and migration/recovery evidence.

## Completed unreleased API cleanup — 2026-08-01

- [x] Removed the legacy `IdentityApi` use-case contract and
  `IdentityApiService` implementation.
- [x] Removed the temporary all-memberships query/use-case created only to
  preserve that legacy implementation.
- [x] Added a source-tree regression rule rejecting the removed legacy files.
- [x] Applied the unreleased-system no-compatibility rule to the architecture
  handbook, module/application templates, naming catalog, and lessons.
- [x] Verified Identity compilation/tests, Modulith, service CI, both boot
  JARs, formatting, Markdown, and whitespace.

The remaining Identity work is authorization hardening, explicit provisioning
transaction/event ports, and final tenant-isolation and migration/recovery
evidence.

## Completed tenant-realm outbound port slice — 2026-08-01

- [x] Added the application-owned `TenantIdentityRealmPort`.
- [x] Added `TenantIdentityRealmAdapter` under the outbound module adapter;
  only that adapter imports the Tenancy public contract.
- [x] Refactored `KeycloakRealmProvisioningProcessManager` and its tests to
  consume the outbound port.
- [x] Added source-boundary and adapter delegation regression coverage.
- [x] Preserved realm provisioning behavior and after-commit listener ownership.
- [x] Verified focused Identity tests and Spotless formatting.

The remaining Identity work is authorization hardening, explicit provisioning
transaction/event ports, and final tenant-isolation and migration/recovery
evidence.

## Completed authentication-configuration port slice — 2026-08-01

- [x] Added the application-owned `IdentityRealmConfigurationPort`.
- [x] Adapted `IdentityKeycloakProperties` to that port in the composition
  root.
- [x] Refactored `AuthenticateUserService` away from configuration-property
  implementation types.
- [x] Preserved platform and tenant realm selection behavior.
- [x] Added source-boundary and authentication-service regression coverage.
- [x] Verified Identity tests/check/integration, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and whitespace checks.

Remaining Identity work is architecture dependency evidence, broader
authorization hardening, explicit provisioning transaction/event ports, and
final tenant-isolation and migration/recovery evidence.

## Completed appointment-consumer inbound-port slice — 2026-08-01

- [x] Added `EnsureCustomerMembershipCommand` and
  `EnsureCustomerMembershipUseCase` under the grouped Identity API.
- [x] Made `EnsureCustomerMembershipService` implement the use case.
- [x] Changed `AppointmentCreatedConsumer` to depend on the use-case contract
  rather than the concrete application service.
- [x] Preserved CUSTOMER-role filtering, JWT subject parsing, idempotency, and
  appointment event handling.
- [x] Added source-boundary and consumer delegation regression coverage.
- [x] Verified Identity tests/check/integration, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and whitespace checks.

Remaining Identity work is architecture dependency evidence, broader
authorization hardening, explicit provisioning transaction/event ports, and
final tenant-isolation and migration/recovery evidence.

## Completed provisioning inbound-port slice — 2026-08-01

- [x] Added the public `ProvisionTenantIdentityUseCase` contract under the
  grouped Identity API.
- [x] Made `KeycloakRealmProvisioningProcessManager` implement the use case.
- [x] Changed the Modulith `TenantCreated` consumer to depend on the use-case
  abstraction rather than the concrete process manager.
- [x] Preserved after-commit listener behavior and realm-provisioning semantics.
- [x] Added delegation and source-boundary regression coverage.
- [x] Verified Identity tests/check/integration, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and whitespace checks.

## Completed distributed login-rate-limit slice — 2026-08-01

- [x] Added the application-owned `LoginAttemptRateLimiter` outbound port.
- [x] Removed attempt storage from `LoginRateLimitFilter` and preserved trusted
  proxy client-key resolution and HTTP 429 behavior.
- [x] Added an atomic Redis Lua-script adapter with a process-local fallback
  when Redis is not configured.
- [x] Declared Identity's Spring Data Redis production dependency explicitly.
- [x] Added filter-boundary, Redis adapter, and source-tree regression coverage.
- [x] Verified Identity tests/check/integration, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and whitespace checks.

Remaining Identity work is architecture dependency evidence, provisioning
transaction/event ports, broader authorization hardening, and final
tenant-isolation and migration/recovery evidence.

## Completed JWT issuer and audience hardening slice — 2026-08-02

- [x] Added an Identity-owned JWT trust policy based on the existing typed
  Keycloak issuer and client-id settings.
- [x] Rejected untrusted issuers before dynamic JWKS URL construction, preventing
  issuer-controlled outbound key resolution.
- [x] Accepted only the configured platform realm and non-empty tenant realms
  following the `emme-<slug>` naming boundary.
- [x] Applied Spring Security issuer, timestamp, and audience validation to each
  dynamically resolved decoder.
- [x] Added regression coverage for trusted platform/tenant issuers, attacker
  issuers, malformed realm boundaries, and audience rejection.
- [x] Verified the focused Identity policy test with Gradle.

Remaining Identity work is broader authorization hardening, explicit provisioning
transaction/event ports, tenant-isolation tests, and migration/recovery evidence.

## Completed exception-advice boundary slice — 2026-08-01

- [x] Added a source-boundary regression test preventing advice from importing
  a concrete Identity controller.
- [x] Scoped `IdentityExceptionHandler` by the inbound web controller package
  instead of `basePackageClasses`.
- [x] Preserved Identity problem-detail status and error-code behavior.
- [x] Verified Identity tests/check/integration, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and whitespace checks.

## Completed TenantCreated after-commit consumer slice — 2026-08-01

- [x] Added consumer delegation and source-boundary regression coverage.
- [x] Replaced plain `@EventListener` and method-level `@Transactional` with
  `@ApplicationModuleListener`.
- [x] Preserved the `TenantCreated` payload and realm-provisioning delegation.
- [x] Verified Identity tests/check/integration, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and whitespace checks.

## Completed role and permission domain boundary slice — 2026-08-01

- [x] Added framework-free `Role`, `Permission`, and domain-owned `RoleScope`.
- [x] Added `RolePersistenceMapper` and `PermissionPersistenceMapper`.
- [x] Kept JPA representations as `RoleEntity`, `PermissionEntity`, and
  `RolePermissionEntity`; preserved existing table, column, and enum mappings.
- [x] Rewired `RolePersistenceAdapter`, `MembershipService`, and
  `PermissionPersistenceAdapter` to consume domain models or mapper boundaries.
- [x] Removed obsolete `RoleReference` and persistence-owned `RoleScope`.
- [x] Added domain, mapper, and source-tree regression coverage.
- [x] Verified Identity tests/check/integration, Studio Modulith, service CI,
  boot JARs, Markdown validation, and whitespace checks.

Remaining Identity work includes distributed rate-limit state, architecture
dependency rules, explicit provisioning transaction/event ports, broader
authorization hardening, and final tenant-isolation and migration/recovery
evidence.

## Completed inbound security-context ownership slice — 2026-08-01

- [x] Moved `UserContext` and `UserContextHolder` out of the Identity root
  package and into `adapter/in/web/security`.
- [x] Preserved JWT/OIDC parsing, missing-authentication failures, and the
  existing static access methods used by Identity and Calendar.
- [x] Declared the package as the explicit `identity-security` named interface
  and narrowed Calendar's Modulith dependency to that interface.
- [x] Added source-tree, security-context, Calendar, integration, and Modulith
  regression coverage.
- [x] Verified service CI, boot JARs, Markdown, and whitespace checks.

The remaining Identity work is distributed rate-limit state, broader
authorization domain/application separation, and the final production-readiness
evidence gate.

## Completed authorization wiring separation slice — 2026-08-01

- [x] Moved role hierarchy and method-security bean wiring into
  `IdentityAuthorizationConfiguration`.
- [x] Moved JWT and OAuth2/OIDC realm-role conversion into dedicated inbound
  security components.
- [x] Preserved role names, `ROLE_` prefixing, OIDC `userinfo` support, and
  existing security chains.
- [x] Added unit and source-tree regression coverage.
- [x] Verified Identity checks and integration tests, Modulith verification,
  CI, boot JARs, Markdown, and whitespace.

The remaining Identity work is distributed rate-limit state, broader
authorization domain/application separation, and the final production-readiness
evidence gate.
