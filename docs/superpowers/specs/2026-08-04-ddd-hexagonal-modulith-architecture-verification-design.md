# DDD + Hexagonal + Spring Modulith Architecture Verification Design

| Field | Detail |
|---|---|
| Status | Draft for review |
| Date | 2026-08-04 |
| Scope | `emme-service` business modules and `emme-platform` composition root |
| Position | Point 5, after real full-stack E2E recordings |
| Reference | `migangdelzar/spring-io-conf-25` structure tests and Modulith documentation |

## 1. Goal

Add executable architecture verification for every Emme module so the repository
continuously proves its DDD, Hexagonal, Spring Modulith, naming, persistence,
tenant-isolation, and event-contract rules.

The implementation will adapt the useful ideas from the Spring I/O conference
project while preserving Emme's capability-first module template, grouped public
API, application-owned ports, tenant schemas, JDBC publication registry, and
Kafka externalization.

## 2. Reference patterns to adapt

The reference project contributes these reusable ideas:

- ArchUnit rules for layer placement and dependency direction.
- Bounded-context isolation rules that permit only intentional public contracts.
- Recursive package metadata discovery.
- Rules preventing controllers and application services from returning domain
  aggregates or persistence entities.
- Package-level DDD and interface-layer annotations.
- Spring Modulith `ApplicationModules.verify()` and `Documenter` output.
- MapStruct for mechanical mapping only, without putting business behavior in
  mapper callbacks.

The reference `EntityWithEvents` and `EventsMapper` persistence-event pipeline
is explicitly not copied. Emme already publishes public events through an
application-owned port, Spring Modulith's JDBC publication registry, and the
Kafka externalizer.

## 3. Architecture test placement

Reusable predicates and conditions belong in test fixtures:

```text
libraries/testing/
└── src/testFixtures/java/com/emme/testing/architecture/
    ├── ArchitectureClasses.java
    ├── ArchitectureTestSupport.java
    ├── DddHexagonalRules.java
    ├── EventContractRules.java
    ├── ModulithRules.java
    ├── NamingRules.java
    ├── PackageMetadataRules.java
    ├── PersistenceOwnershipRules.java
    └── TenantIsolationRules.java
```

The platform composition root owns the repository-wide execution suite:

```text
applications/emme-platform/src/test/java/com/emme/
├── DddHexagonalArchitectureTest.java
├── EventContractArchitectureTest.java
├── ModularityTest.java
├── NamingConventionArchitectureTest.java
├── PackageMetadataArchitectureTest.java
├── PersistenceOwnershipTest.java
└── SchemaOwnershipTest.java
```

Module-specific convention tests remain inside each module for rules that need
module-owned names, public contracts, or capability-specific exceptions.

## 4. Rules to enforce

### Domain purity

- Domain code must not depend on Spring, JPA, Hibernate, HTTP, Kafka, Jackson,
  Redis, Liquibase, or adapter packages.
- Aggregates, entities, value objects, specifications, and domain services must
  remain framework-independent.
- Domain events must be immutable and owned by the module that publishes them.

### Application direction

- Application services may depend on domain types, public API types, and
  application-owned outbound ports.
- Application services must not depend on inbound or outbound technical
  adapters, persistence entities, Spring Data repositories, or provider clients.
- Every concrete `*Service` in `application/service` must implement exactly one
  matching `*UseCase` interface.

### Adapter ownership

- HTTP controllers, message consumers, schedulers, and webhook handlers belong
  to `adapter.in`.
- Persistence entities, Spring Data repositories, database adapters, provider
  clients, and external publishers belong to `adapter.out`.
- Controllers may depend on inbound use-case interfaces and web mappers, but
  never on repositories, entities, or domain aggregates.
- Controllers and application services must not return persistence entities or
  domain aggregates.

### Module and API boundaries

- Cross-module dependencies must use explicitly named public API packages or
  public event contracts.
- Cross-module consumers must not import another module's domain, application,
  adapter, configuration, persistence, or provider packages.
- Spring Modulith verification must pass without weakening existing allowed
  dependency declarations.
- Every materialized public package must have package metadata and an explicit
  named-interface decision.

### Naming and package structure

- File names, class names, records, enums, services, ports, adapters,
  repositories, controllers, mappers, events, and exceptions must follow the
  normalized architecture naming matrix.
- Empty template packages must not be created only to satisfy the maximum
  module shape.
- Configuration classes must remain in `configuration`.

### Persistence and tenant ownership

- Persistence types must be owned by their module's outbound persistence
  adapter.
- Each entity must declare one explicit table owner.
- `emme_core` mappings may only belong to the platform-owned Identity and
  Tenancy modules.
- Tenant-owned tables must be mapped and migrated in the tenant schema.
- Tenant predicates, routing, and schema selection must remain covered by
  executable tests.

### Event contracts

- Public events must be immutable, past-tense facts grouped under `api.event`.
- Application services publish through application-owned event ports.
- Spring Modulith publication and Kafka externalization remain infrastructure
  concerns.
- Tests must verify topic, key, payload, tenant identity, transaction boundary,
  replay/idempotency behavior, and failure handling.
- Architecture rules must prevent mappers from publishing events or accessing
  event publishers.

## 5. JMolecules adoption policy

JMolecules annotations may be introduced where they clarify an existing
architectural concept:

- `@BoundedContext` for real bounded module roots;
- `@AggregateRoot`, `@Entity`, and `@ValueObject` for domain semantics;
- `@DomainEvent` for domain-owned event types;
- layered annotations on `package-info.java` where they match the canonical
  package structure.

Annotations are supplementary metadata. ArchUnit and Spring Modulith remain
the executable enforcement mechanisms. The implementation must first verify
dependency compatibility with the current Gradle platform before adding any
JMolecules artifact.

## 6. Mapping policy

MapStruct may be adopted selectively for repetitive, deterministic mappings:

- domain ↔ persistence representations;
- application results ↔ web responses;
- external provider payloads ↔ internal models.

Mapper hooks are allowed only for deterministic, side-effect-free target
enrichment. They must not implement business rules, authorization, tenant
resolution, persistence, event publication, or network calls.

The reference `EventsMapper` pattern is not part of this design because Emme
does not use an event-bearing JPA entity model.

## 7. Documentation generation

The existing Modulith documentation test will be strengthened to verify:

- the module graph is valid;
- generated module documentation completes successfully;
- individual PlantUML module diagrams are generated in build output;
- generated documentation is excluded from source control unless explicitly
  promoted as a reviewed artifact;
- event and named-interface decisions remain visible in the generated graph.

## 8. Verification matrix

| Risk | Verification |
|---|---|
| Domain framework leakage | ArchUnit dependency rule |
| Adapter inversion | Layer dependency rules |
| Cross-module internal imports | Spring Modulith + ArchUnit |
| Wrong public API exposure | Named-interface and package rules |
| Multi-use-case service regression | Service declaration rule |
| Entity/table ownership drift | Persistence and schema ownership rules |
| Tenant isolation regression | Tenant schema and predicate tests |
| Event contract drift | Kafka contract and integration tests |
| Documentation drift | Modulith `Documenter` test |
| Naming regression | Repository-wide naming rules |

## 9. Non-goals

- Do not migrate every mapper to MapStruct automatically.
- Do not introduce JMolecules annotations without a verified compatibility
  decision.
- Do not replace the JDBC publication registry with the JPA event store.
- Do not move business logic into architecture-test utilities.
- Do not weaken existing module boundaries to make the suite pass.

## 10. Acceptance criteria

- All current modules are scanned by the architecture suite.
- At least one deliberate red test proves each major rule before its fix.
- The suite passes with no skipped architecture tests.
- Existing module convention tests remain green.
- `ApplicationModules.verify()` and Modulith documentation generation pass.
- Kafka event contract and integration tests remain green.
- No conference-project event persistence infrastructure is introduced.
- The implementation plan and final evidence report record every adapted tool,
  intentional deviation, and remaining environment-dependent gate.
