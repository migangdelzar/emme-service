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

- [ ] Inventory all Catalog production packages and compare them to the current
  template's materialization rule.
- [ ] Verify every materialized package has `package-info.java` and every API kind
  joins the intended `api` named interface.
- [ ] Verify domain imports no Spring/JPA/HTTP/JSON/provider code.
- [ ] Verify application code imports no concrete outbound adapter and no API
  result exposes a persistence entity.
- [ ] Verify persistence mapper managed-entity behavior and tenant predicates.
- [ ] Verify hybrid-search ports/adapters remain Catalog-owned and do not leak
  Shared implementation details.
- [ ] Run `./gradlew :modules:catalog:test :modules:catalog:integrationTest :applications:studio-api:test --tests '*ModularityTest*' --no-daemon --no-configuration-cache`.
- [ ] Run service formatting, Checkstyle, CI, and boot-JAR gates.
- [ ] Create a verification report; if no gaps remain, mark Catalog as the
  verified baseline in `docs/superpowers/plans/README.md` and `tasks/todo.md`.

## Definition of done

- [ ] Catalog conformance is evidenced by tests and source-tree checks.
- [ ] Any deliberate naming compatibility exception has an ADR and executable
  guardrail.
- [ ] No unrelated Catalog behavior is changed.
