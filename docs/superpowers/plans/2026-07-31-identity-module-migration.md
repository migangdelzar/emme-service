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

- [ ] No legacy mixed Identity package remains except a documented temporary
  exception with owner, removal task, tests, and ADR.
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

Typed security configuration, Identity-specific failure advice, and the final
security hardening gate remain open slices.

## Completed typed security configuration slice — 2026-08-01

- [x] Added `IdentitySecurityProperties` under `configuration` with typed CORS,
  CSP, logout, and cache settings.
- [x] Preserved the existing local-development defaults and security behavior.
- [x] Rewired `SecurityConfiguration` to consume typed settings instead of
  embedding environment-specific values in the security bean wiring.
- [x] Added property-default and package-ownership tests.
- [x] Verified Identity checks, Studio Modulith verification, Markdown
  validation, and diff whitespace checks.

Identity-specific failure advice and the final security hardening gate remain
open slices.

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

The next Identity hardening slice is to move the password-grant Keycloak
orchestration out of the legacy application class, type the remaining Keycloak
client configuration, and complete security regression coverage.

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

Remaining follow-up: type the remaining issuer/client Keycloak configuration
and complete the broader Identity security hardening review.
