# Catalog Canonical Baseline Verification Plan

> **For agentic workers:** This is a verification plan, not permission to redesign Catalog behavior. Use the current module template and preserve the already-migrated contracts.

**Goal:** Re-verify Catalog against the latest module template and record the
remaining naming, package-info, dependency, persistence, search, and operational
controls before treating it as the service migration baseline.

## Baseline scope

Catalog already has grouped API contracts, framework-free domain models,
application ports, persistence adapters, inbound adapters, and hybrid-search
integration. This plan checks conformance and fixes only documented gaps.

## Tasks

- [x] Inventory all Catalog production packages and compare them to the current
  template's materialization rule.
- [x] Verify every materialized package has `package-info.java` and every API kind
  joins the intended `api` named interface.
- [x] Verify domain imports no Spring/JPA/HTTP/JSON/provider code.
- [x] Verify application code imports no concrete outbound adapter and no API
  result exposes a persistence entity.
- [x] Verify persistence mapper managed-entity behavior and tenant predicates.
- [x] Verify hybrid-search ports/adapters remain Catalog-owned and do not leak
  Shared implementation details.
- [x] Run `./gradlew :modules:catalog:test :modules:catalog:integrationTest :applications:studio-api:test --tests '*ModularityTest*' --no-daemon --no-configuration-cache`.
- [x] Run service formatting, Checkstyle, CI, and boot-JAR gates.
- [x] Create a verification report; if no gaps remain, mark Catalog as the
  verified baseline in `docs/superpowers/plans/README.md` and `tasks/todo.md`.

## Definition of done

- [x] Catalog conformance is evidenced by tests and source-tree checks.
- [x] Any deliberate naming compatibility exception has an ADR and executable
  guardrail.
- [x] No unrelated Catalog behavior is changed.

## Verification report — 2026-08-01

### Catalog boundary changes

- Added package-level metadata for every materialized Catalog package while
  retaining `@ApplicationModule` on the module root and
  `@NamedInterface("catalog-api")` on `catalog.api`.
- Added the Catalog-owned `CatalogSearchPort` and `CatalogSearchHit` contracts.
- Added `HybridCatalogSearchAdapter` to translate Shared's hybrid-search engine
  into Catalog semantics; `CatalogMatchService` no longer imports Shared's
  `HybridSearch` or `SearchTarget` directly.
- Added convention and adapter mapping tests for package metadata and search
  ownership.

### Infrastructure corrections discovered by the full gate

- Updated `DatabaseRegistryAdapter` to consume Spring Boot's
  `JdbcConnectionDetails`, which supports both configured JDBC properties and
  Testcontainers `@ServiceConnection` details.
- Removed `final` from Identity persistence adapters that must be proxied by the
  configured Spring AOP infrastructure.
- Corrected stale Studio `Tenant.getId()` test references to `Tenant.id()`.
- Applied scoped Spotless fixes to existing Booking, Customer, and Workforce
  package metadata/convention tests.

### Evidence

- Focused Catalog tests: passed.
- Catalog integration test with PostgreSQL Testcontainers: passed.
- Studio Modulith test: passed.
- `./gradlew ci -x test -x integrationTest -x e2eTest`: passed.
- `:applications:studio-api:bootJar` and `:applications:emme-platform:bootJar`:
  passed.
- `node scripts/validate-markdown.mjs`, source-boundary checks, and
  `git diff --check`: passed.
