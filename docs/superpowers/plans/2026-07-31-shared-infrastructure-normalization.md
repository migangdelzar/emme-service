# Shared Infrastructure Normalization Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. Shared is cross-cutting infrastructure, so apply the module template selectively and never move files without a consumer map.

**Goal:** Normalize Shared's cross-cutting primitives, global web advice, and
hybrid search capability without turning Shared into a business-domain dumping
ground or breaking every module's persistence/test dependency.

**Architecture:** Shared is not a normal business module. It owns technical
primitives that are intentionally reused: JPA base classes, ID/clock helpers,
global error handling, and search infrastructure. Keep these capability-owned
packages rather than forcing empty DDD layers. Business models must remain in
their owning module.

## Current inventory

```text
com.emme.shared
├── BaseEntity.java
├── TenantOwnedEntity.java
├── ClockProvider.java
├── IdGenerator.java
├── search/{HybridSearch,SearchTarget}
└── web/GlobalExceptionHandler.java
```

## Target ownership

```text
com.emme.shared
├── package-info.java
├── persistence/{BaseEntity,TenantOwnedEntity}
├── time/{ClockProvider}
├── identity/{IdGenerator}
├── search/{HybridSearch,SearchTarget}
└── web/advice/GlobalExceptionHandler.java
```

The exact names may remain in their current package when moving them would create
an unnecessary repository-wide compatibility blast radius; that decision requires
an import inventory and an ADR. `shared` exposes technical primitives, not
business API results or domain aggregates.

## Tasks

### Task 1: Consumer and ownership inventory

- [ ] Map every import of `BaseEntity`, `TenantOwnedEntity`, `ClockProvider`,
  `IdGenerator`, `HybridSearch`, `SearchTarget`, and `GlobalExceptionHandler`.
- [ ] Classify each type as persistence primitive, time/test primitive, identity
  primitive, search capability, or web infrastructure.
- [ ] Confirm no business concept is hidden in Shared.

### Task 2: Define capability-owned packages and compatibility strategy

- [ ] Add package-info documentation for each materialized capability package.
- [ ] Choose keep-in-place versus package move per type based on consumer count and
  binary/source compatibility; record moves in an ADR.
- [ ] Keep `BaseEntity` and `TenantOwnedEntity` framework-specific primitives out
  of domain packages; domain models must not extend them.
- [ ] Keep global advice transport infrastructure and ensure it imports only public
  exception contracts and kernel tracing primitives.

### Task 3: Isolate Search capability

- [ ] Keep `HybridSearch` behind a capability-owned port where callers need an
  abstraction; move provider/JDBC details behind an adapter only if that reduces
  coupling without changing query semantics.
- [ ] Preserve `SearchTarget` table/column/predicate behavior and tenant filters.
- [ ] Add integration tests for vector/text search, missing embeddings, limits,
  and tenant predicates.

### Task 4: Verification and dependency hygiene

- [ ] Add architecture rules forbidding Shared business models and enforcing
  approved dependency direction.
- [ ] Run all module compile/tests, shared integration tests, formatting,
  Checkstyle, Modulith, and CI gates after any move.
- [ ] Verify no circular dependency or accidental API exposure is introduced.
- [ ] Document recovery/rollback because Shared changes have repository-wide blast
  radius; merge only after all consumers pass.

## Definition of done

- [ ] Shared has explicit capability ownership and no generic `common`/`utils`
  dumping ground.
- [ ] Technical primitives remain reusable and domain models remain owned locally.
- [ ] Search and global web advice have tests and documented boundaries.

## Completed ownership decision slice — 2026-08-01

- [x] Recorded the consumer/ownership decision in ADR 0004.
- [x] Added capability package metadata for persistence, time, identity, and
  web infrastructure without introducing fake domain/application layers.
- [x] Moved global advice to `shared.web.advice`.
- [x] Added Shared ownership regression coverage.

Remaining work is search integration evidence, full dependency-cycle checks, and
the service-wide verification gate.
