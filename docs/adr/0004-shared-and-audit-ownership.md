# ADR 0004: Shared and Audit ownership during module normalization

| Field | Decision |
|---|---|
| Status | Accepted |
| Date | 2026-08-02 |
| Scope | Shared technical primitives and the currently empty Audit module |

## Context

`shared` is consumed by every persistence adapter and owns technical primitives
such as `PersistedEntity`, `TenantOwnedEntity`, clock/ID helpers, hybrid search, and
global transport advice. These are cross-cutting capabilities rather than
business concepts, but their package locations still need to communicate their
technical ownership clearly.

The `audit` Gradle module contains only Modulith metadata. Existing durable audit
storage is currently owned by Tenancy (`AuditEvent`) and security audit logging
is owned by Identity. No production code imports an Audit API.

## Decision

1. Keep Shared as a technical capability module, not a business module. Place
   high-fan-out primitives in capability-owned packages and expose only the
   required Spring Modulith named interfaces:
   `shared.persistence`, `shared.persistence-jdbc`, `shared.time`,
   `shared.identity`, and `shared.search`. Global advice remains in the
   capability-owned `shared.web.advice` package and has no cross-module API
   consumer.
2. Move global transport advice to `shared.web.advice`; business exception
   advice remains inside each owning module.
3. Keep Audit as a metadata-only reserved Modulith boundary for now. Do not
   create empty `api`, `domain`, `application`, or `adapter` packages and do not
   duplicate Tenancy's audit persistence.
4. A future Audit implementation must first define system of record, retention,
   tenant scope, redaction, availability, and migration ownership in a separate
   approved ADR and plan.

## Consequences

- Shared is capability-first without pretending package movement alone creates a
  business boundary.
- Existing modules consume explicit named interfaces instead of a root-package
  compatibility surface; because the application is unreleased, no legacy
  source shim is retained.
- Audit requirements remain explicit and traceable without a fake implementation.
- Search remains Shared-owned until a module-specific search port is introduced
  with verified tenant predicates and integration coverage.

## Verification

- `SharedOwnershipConventionTest` prevents business-layer scaffolding in Shared.
- Audit remains metadata-only and its module test continues to verify that state.
- The service-wide gate must confirm no accidental Audit imports or duplicate
  audit persistence are introduced.
