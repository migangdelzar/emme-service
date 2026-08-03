# Audit Module Normalization Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. Do not scaffold architectural layers without a real audit responsibility.

**Goal:** Resolve the Audit module's current empty state through an explicit
ownership decision and leave the repository with no misleading pseudo-architecture.

**Architecture:** Audit currently contains only `@ApplicationModule` metadata. The
latest module template says optional branches are materialized only when a real
responsibility exists. This plan therefore has two valid outcomes: keep a
metadata-only reserved module with an approved capability record, or retire the
empty Gradle module if no owner/consumer exists. It must not create empty
`api/application/domain/adapter` directories.

## Current inventory

```text
modules/audit/src/main/java/com/emme/audit/package-info.java
```

## Tasks

### Task 1: Ownership and consumer decision

- [x] Search all production, test, migration, build, and documentation references
  to `com.emme.audit` and audit tables/events.
- [x] Identify the system of record for audit data currently emitted by Identity,
  Tenancy, Studio, Notification, Payment, and shared infrastructure.
- [x] Record owner, data classification, retention, tenant scope, availability,
  RTO/RPO, and whether an independent module is justified.

### Task 2: Apply the selected outcome

- [x] If no current capability exists, keep only root metadata and document the
  module as reserved; do not add empty package branches.
- [x] Not applicable: no independently owned Audit capability exists; any future
  capability requires a separate approved design before materialization.
- [x] Not applicable: the metadata-only reserved boundary remains part of the
  current Modulith build; deletion would require a separate explicit plan.

### Task 3: Verify

- [x] Run `./gradlew :modules:audit:test :applications:emme-platform:test --tests '*ModularityTest*' --no-daemon --no-configuration-cache`.
- [x] Record the decision in ADR 0004 and update the service module registry.

## Definition of done

- [x] Audit has an explicit owner and status.
- [x] No empty architectural tree is presented as implemented architecture.
- [x] Any future audit implementation has a separate approved module plan.

## Decision recorded — 2026-08-01

- [x] Confirmed durable `AuditEvent` persistence is currently owned by Tenancy
  and security audit logging is owned by Identity.
- [x] Recorded ADR 0004 for Shared and Audit ownership.
- [x] Kept Audit as a metadata-only reserved Modulith boundary; no empty
  architectural layers were scaffolded.
- [x] Preserved a separate future decision before extracting Audit.

## Reserved-boundary hardening — 2026-08-02

- [x] Removed unnecessary Shared and Tenancy implementation dependencies from
  the metadata-only Audit Gradle project.
- [x] Set Audit's Modulith metadata to `allowedDependencies = {}`.
- [x] Replaced the placeholder test assertion with a source invariant proving
  that only `package-info.java` exists and no empty DDD/Hexagonal layer tree was
  scaffolded.
- [x] Verified Audit and the application Modulith test after the cleanup.

## Repository-local closure — 2026-08-03

- [x] Verified the metadata-only Audit boundary in the complete platform
  Modulith, CI, boot JAR, and Markdown gates.
- [x] Confirmed that durable audit ownership remains with the existing Tenancy
  and Identity capabilities; no duplicate Audit business module was created.

Any future Audit capability still requires a separately approved retention,
ownership, and data-classification decision.
