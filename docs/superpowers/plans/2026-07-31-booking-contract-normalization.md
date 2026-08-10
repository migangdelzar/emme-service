# Booking Contract Normalization Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. Keep every step independently verifiable.

**Goal:** Normalize Booking's current contract-only source tree to grouped API
ownership while avoiding invented booking behavior and stale public-interface
dependencies.

**Architecture:** Booking currently contains module metadata, empty `api` and
`events` package metadata, and no production types. The root metadata retains
only the actual shared and tenancy dependencies; references to nonexistent
legacy named interfaces are removed until real Booking contracts exist.

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

- [x] Search production, test, Gradle, and documentation references to
  `booking-api` and `booking-events`.
- [x] Record the exact consumers and whether they need complete API access or an
  event-only interface.
- [x] Verify no Booking Java type currently exists before changing metadata.

### Task 2: Normalize package metadata

- [x] Convert `api/package-info.java` to namespace documentation with no annotation
  until an API-kind child contains a real type.
- [x] Move event metadata to `api/event/package-info.java` only if a real event is
  found during Task 1; otherwise delete the empty `events/package-info.java`.
- [x] Keep root allowed dependencies limited to actual current dependencies;
  restore named module dependencies only when concrete grouped contracts and
  consumer evidence exist.
- [x] Add a source-tree test that forbids top-level `events` and ungrouped API
  classes.

### Task 3: Define the first Booking capability procedure

- [ ] For each future capability, create only the needed commands, queries,
  results, use cases, events, exceptions, and types.
- [ ] Add domain/application/adapters only when the capability owns behavior or
  data.
- [ ] Keep cross-module calls on named public contracts; never import Studio,
  Customer, Workforce, or Catalog implementation packages.

### Task 4: Verify

- [x] Run focused Booking tests and Studio Modulith verification.
- [x] Commit the normalization in the shared contract-boundary commit.

## Definition of done

- [x] No top-level `booking.events` package remains unless an ADR explicitly
  records a temporary compatibility exception.
- [x] No business types were invented.
- [x] The next Booking feature can be added directly under the current template.
