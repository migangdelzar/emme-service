# Workforce Contract Normalization Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. Keep every step independently verifiable.

**Goal:** Normalize the currently empty Workforce module contract boundary to the
latest module template without inventing workforce business behavior.

**Architecture:** Workforce currently contains only module metadata and an empty
`api` package. The module remains a valid Modulith boundary, but no optional
architectural layer is materialized until a real workforce capability exists.

## Current inventory

```text
modules/workforce/src/main/java/com/emme/workforce/
├── package-info.java
└── api/package-info.java
```

## Target and naming rules

```text
com.emme.workforce/
├── package-info.java
└── api/package-info.java
```

Future types must use the normalized matrix: `*Command`, `*Query`, descriptive
`*Info`/`*Summary`/`*Page` results, `*UseCase`, past-tense event names,
`*Exception`, and semantic API types. Spring/JPA/provider classes must never be
placed in `api`.

## Tasks

### Task 1: Normalize the empty boundary

- [ ] Verify no production or test class imports `com.emme.workforce.api`.
- [ ] Replace `@NamedInterface("workforce-api")` with namespace documentation
  until a concrete API-kind child exists.
- [ ] Preserve root allowed dependencies `shared` and `tenancy`.
- [ ] Add a source-tree convention test for root ownership and grouped API types.
- [ ] Run `./gradlew :modules:workforce:test :applications:studio-api:test --tests '*ModularityTest*' --no-daemon --no-configuration-cache`.
- [ ] Commit `chore(workforce): normalize empty module contract boundary`.

### Task 2: Define the first capability gate

- [ ] Require an approved capability design before creating domain/application or
  adapter packages.
- [ ] Materialize only the API kind needed by the first capability.
- [ ] Add package-info and API closure tests with the first real type.

## Definition of done

- [ ] Workforce has no fake business architecture.
- [ ] The legacy `workforce-api` named interface is removed.
- [ ] Modulith verification and the source-tree convention test pass.
