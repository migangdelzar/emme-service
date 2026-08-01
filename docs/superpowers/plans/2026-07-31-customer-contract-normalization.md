# Customer Contract Normalization Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. Keep every step independently verifiable.

**Goal:** Normalize the currently empty Customer module contract boundary to the
latest module template without inventing customer business behavior.

**Architecture:** Customer currently contains only the module root and an empty
`api` package. The plan makes that absence explicit, keeps the root Modulith
metadata, and materializes grouped API packages only when a real contract is
introduced. No domain, application, adapter, or persistence package is created
for a module with no production type.

**Source of truth:** `docs/templates/module-package-structure-template.md`.

## Current inventory

```text
modules/customer/src/main/java/com/emme/customer/
├── package-info.java
└── api/package-info.java
```

There are no Customer commands, queries, results, use cases, events, entities,
controllers, services, or repositories to migrate.

## Target and naming rules

```text
com.emme.customer/
├── package-info.java                 @ApplicationModule only
└── api/package-info.java             namespace documentation only
```

When the first real contract is approved, use:

```text
api/command/<Verb><Subject>Command.java
api/query/<ReadVerb><Subject>Query.java
api/result/<Subject><Shape>.java
api/usecase/<Verb><Subject>UseCase.java
api/event/<Subject><PastParticiple>.java
api/exception/<Subject><Failure>Exception.java
api/type/<Concept><Qualifier>.java
```

Annotate each materialized API-kind child with `@NamedInterface("api")`; add
`@NamedInterface({"api", "events"})` only to a real public event package.

## Tasks

### Task 1: Remove the legacy empty named-interface shape

- [ ] Confirm no Java type or external consumer imports `com.emme.customer.api`.
- [ ] Replace `@NamedInterface("customer-api")` in `api/package-info.java` with
  responsibility-only Javadoc; do not expose an empty named interface.
- [ ] Preserve `@ApplicationModule(displayName = "Customer", allowedDependencies = {"shared", "tenancy"})`.
- [ ] Add a source-tree test that rejects business classes directly in the module
  root and rejects API types outside grouped child packages.
- [ ] Run `./gradlew :modules:customer:test :applications:studio-api:test --tests '*ModularityTest*' --no-daemon --no-configuration-cache`.
- [ ] Commit `chore(customer): normalize empty module contract boundary`.

### Task 2: Establish the first-contract procedure

- [ ] Add a module README/design note only if the first Customer capability is
  approved; document its aggregate, data classification, availability, RTO/RPO,
  owner, and dependency review.
- [ ] Create only the API kind required by that capability.
- [ ] Add package-info, API closure, contract tests, and a consumer update in the
  same commit as the first real type.

## Definition of done

- [ ] Customer contains no invented empty `domain`, `application`, `adapter`, or
  `configuration` tree.
- [ ] No legacy `customer-api` named interface remains.
- [ ] The module passes Modulith verification and has an auditable first-contract
  procedure.
- [ ] No behavior or public type was invented by this normalization.
