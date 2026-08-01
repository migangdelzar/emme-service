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

- [ ] Search all production, test, migration, build, and documentation references
  to `com.emme.audit` and audit tables/events.
- [ ] Identify the system of record for audit data currently emitted by Identity,
  Tenancy, Studio, Notification, Payment, and shared infrastructure.
- [ ] Record owner, data classification, retention, tenant scope, availability,
  RTO/RPO, and whether an independent module is justified.

### Task 2: Apply the selected outcome

- [ ] If no current capability exists, keep only root metadata and document the
  module as reserved; do not add empty package branches.
- [ ] If a capability exists, create a separate approved audit design first, then
  materialize only its real API/domain/application/adapter packages.
- [ ] If the module is not needed, remove it from settings/build dependencies using
  a separate explicit deletion plan; do not silently delete it here.

### Task 3: Verify

- [ ] Run `./gradlew :modules:audit:test :applications:studio-api:test --tests '*ModularityTest*' --no-daemon --no-configuration-cache`.
- [ ] Record the decision in an ADR and update the service module registry.

## Definition of done

- [ ] Audit has an explicit owner and status.
- [ ] No empty architectural tree is presented as implemented architecture.
- [ ] Any future audit implementation has a separate approved module plan.
