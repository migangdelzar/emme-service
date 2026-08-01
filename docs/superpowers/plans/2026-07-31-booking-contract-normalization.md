# Booking Contract Normalization Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. Keep every step independently verifiable.

**Goal:** Normalize Booking's current contract-only source tree to grouped API
ownership while preserving every declared cross-module dependency and avoiding
invented booking behavior.

**Architecture:** Booking currently contains module metadata, empty `api` and
`events` package metadata, and no production types. Its declared dependencies on
Studio, Customer, Workforce, and Catalog are retained until real Booking
contracts replace the legacy named-interface declarations.

## Current inventory

```text
modules/booking/src/main/java/com/emme/booking/
├── package-info.java
├── api/package-info.java
└── events/package-info.java
```

## Target ownership

```text
com.emme.booking/
├── package-info.java
└── api/package-info.java
```

`api/event` is created only when a real Booking event exists. The legacy top-level
`events` package is not a valid target package.

## Tasks

### Task 1: Inventory declarations and consumers

- [ ] Search production, test, Gradle, and documentation references to
  `booking-api` and `booking-events`.
- [ ] Record the exact consumers and whether they need complete API access or an
  event-only interface.
- [ ] Verify no Booking Java type currently exists before changing metadata.

### Task 2: Normalize package metadata

- [ ] Convert `api/package-info.java` to namespace documentation with no annotation
  until an API-kind child contains a real type.
- [ ] Move event metadata to `api/event/package-info.java` only if a real event is
  found during Task 1; otherwise delete the empty `events/package-info.java`.
- [ ] Preserve root allowed dependencies exactly; do not weaken Modulith rules.
- [ ] Add a source-tree test that forbids top-level `events` and ungrouped API
  classes.

### Task 3: Define the first Booking capability procedure

- [ ] For each future capability, create only the needed commands, queries,
  results, use cases, events, exceptions, and types.
- [ ] Add domain/application/adapters only when the capability owns behavior or
  data.
- [ ] Keep cross-module calls on named public contracts; never import Studio,
  Customer, Workforce, or Catalog implementation packages.

### Task 4: Verify

- [ ] Run `./gradlew :modules:booking:test :applications:studio-api:test --tests '*ModularityTest*' --no-daemon --no-configuration-cache`.
- [ ] Commit `chore(booking): normalize grouped contract boundary`.

## Definition of done

- [ ] No top-level `booking.events` package remains unless an ADR explicitly
  records a temporary compatibility exception.
- [ ] No business types were invented.
- [ ] The next Booking feature can be added directly under the current template.
