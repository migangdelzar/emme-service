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

- [ ] Extract request/response records and web mappers from all controllers.
- [ ] Move controllers to `adapter/in/web/controller` and add module advice only
  for Identity-owned expected failures.
- [ ] Update all cross-module imports to named API packages in the same commits as
  contract moves.
- [ ] Preserve route paths, status codes, token response shapes, tenant behavior,
  and security annotations.

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
