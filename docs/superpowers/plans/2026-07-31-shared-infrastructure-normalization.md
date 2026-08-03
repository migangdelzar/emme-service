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

## Historical pre-normalization inventory

```text
com.emme.shared
├── persistence/{BaseEntity,TenantOwnedEntity}
├── time/ClockProvider.java
├── identity/IdGenerator.java
├── search/{HybridSearch,SearchTarget}
└── web/advice/GlobalExceptionHandler.java
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

The capability-owned locations are intentional and all root-package primitive
locations have been removed. `shared` exposes technical primitives, not business
API results or domain aggregates.

## Tasks

### Task 1: Consumer and ownership inventory

- [x] Map every import of `BaseEntity`, `TenantOwnedEntity`, `ClockProvider`,
  `IdGenerator`, `HybridSearch`, `SearchTarget`, and `GlobalExceptionHandler`.
- [x] Classify each type as persistence primitive, time/test primitive, identity
  primitive, search capability, or web infrastructure.
- [x] Confirm no business concept is hidden in Shared.

### Task 2: Define capability-owned packages and compatibility strategy

- [x] Add package-info documentation for each materialized capability package.
- [x] Choose keep-in-place versus package move per type based on consumer count and
  binary/source compatibility; record moves in an ADR.
- [x] Keep `BaseEntity` and `TenantOwnedEntity` framework-specific primitives out
  of domain packages; domain models must not extend them.
- [x] Keep global advice transport infrastructure and ensure it imports only public
  exception contracts and kernel tracing primitives.

### Task 3: Isolate Search capability

- [x] Keep `HybridSearch` behind a capability-owned port where callers need an
  abstraction; Catalog callers use `CatalogSearchPort` and keep Shared details
  inside the Catalog adapter.
- [x] Preserve `SearchTarget` table/column/predicate behavior and tenant filters.
- [x] Add integration tests for vector/text search, missing embeddings, limits,
  and tenant predicates.

### Task 4: Verification and dependency hygiene

- [x] Add architecture rules forbidding Shared business models and enforcing
  approved dependency direction.
- [x] Run the repository test gate, Shared integration tests, formatting,
  Checkstyle, and application Modulith verification after the moves.
- [x] Verify no circular dependency or accidental API exposure is introduced.
- [ ] Document recovery/rollback because Shared changes have repository-wide blast
  radius; merge only after all consumers pass.

## Definition of done

- [x] Shared has explicit capability ownership and no generic `common`/`utils`
  dumping ground.
- [x] Technical primitives remain reusable and domain models remain owned locally.
- [x] Search and global web advice have tests and documented boundaries.

## Completed ownership decision slice — 2026-08-01

- [x] Recorded the consumer/ownership decision in ADR 0004.
- [x] Added capability package metadata for persistence, time, identity, and
  web infrastructure without introducing fake domain/application layers.
- [x] Moved global advice to `shared.web.advice`.
- [x] Added Shared ownership regression coverage.

Search integration and tenant-predicate evidence is recorded in
`docs/superpowers/reviews/2026-08-02-shared-search-verification.md`. Remaining
work is full dependency-cycle checks and the service-wide verification gate.

## Test-profile shutdown lifecycle — 2026-08-03

- [x] Add an application-level regression assertion for every shared ephemeral
  test profile.
- [x] Change H2 and test-only database profiles from `ddl-auto: create-drop` to
  `ddl-auto: create`, preserving startup isolation without dropping
  `event_publication` before Spring Modulith shutdown callbacks run.
- [x] Verify the application parity test and Studio module check.
- [ ] Retain PostgreSQL/Testcontainers shutdown output as environment-specific
  evidence until the container lifecycle can be isolated without weakening
  resource ownership.

## Completed tenant-scoped search maintenance slice — 2026-08-01

- [x] Added a failing source-boundary test requiring tenant predicates on
  embedding updates and missing-embedding maintenance queries.
- [x] Updated `HybridSearch.updateEmbedding`, `idsMissingEmbedding`, and
  `countMissingEmbedding` to require a tenant identifier and bind it as a
  parameter.
- [x] Preserved the existing enum-bound table allow-list and hybrid search
  query semantics.
- [x] Verified the Shared ownership test and compilation.

Remaining Shared work is dependency-cycle verification and the final
service-wide gate.

## Completed capability package normalization — 2026-08-02

- [x] Moved `BaseEntity` and `TenantOwnedEntity` to `shared.persistence`.
- [x] Moved `ClockProvider` to `shared.time` and `IdGenerator` to
  `shared.identity`.
- [x] Updated all module imports and shared tests to use the capability-owned
  packages; no legacy root-package primitive remains.
- [x] Verified the source-boundary regression test fails before the move and
  passes after the move.

Remaining Shared work is dependency-cycle verification and the final
service-wide gate.

## Completed managed JDBC connection template slice — 2026-08-02

- [x] Promoted managed JDBC connection execution from Tenancy into Shared's
  capability-owned `persistence.jdbc` package.
- [x] Added generic `ThrowingSqlConnectionFunction<R, E extends Throwable>`
  and `ThrowingSqlConnectionConsumer<E extends Throwable>` contracts.
- [x] Added `JdbcConnectionExecutor` backed by Spring `JdbcTemplate`, with
  `withConnection` for result-producing callbacks and
  `consumeWithConnection` for side-effecting callbacks.
- [x] Added typed `JdbcConnectionExecutionException` preserving the original
  callback cause and rethrowing fatal `Error` instances.
- [x] Migrated the Tenancy Liquibase adapter away from manual connection
  acquisition and close handling.
- [x] Added focused unit coverage for generic callback results, consumer
  execution, and checked-failure translation.
- [x] Added Shared integration coverage for tenant-scoped embedding maintenance
  and bounded missing-embedding selection.

The template deliberately does not expose a `Supplier` overload: a supplier
cannot make the managed `Connection` dependency explicit. Additional
connection-backed adapters should reuse this capability instead of creating a
module-local connection service.
