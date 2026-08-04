# ADR 0007: Selective architecture metadata and mapping generation

| Field | Decision |
|---|---|
| Status | Accepted |
| Date | 2026-08-04 |
| Scope | DDD/Hexagonal metadata and Java object mapping |

## Context

The service already enforces module boundaries with Spring Modulith and
dependency direction with ArchUnit. The module template also describes
framework-free domain models, application-owned ports, and explicit mapper
boundaries. Adding JMolecules or converting every mapper to MapStruct would add
annotations, processors, and generated code across every module before there is
a concrete rule that requires either dependency.

## Decision

1. Keep ArchUnit and Spring Modulith as the executable architecture authorities.
   They already verify dependency direction, named interfaces, public event
   shape, package metadata, entity ownership, and generated module
   documentation.
2. Do not add JMolecules as a mandatory service dependency. It may be adopted
   later for a specific domain package when its annotations provide meaningful
   documentation or tooling value without replacing the existing executable
   rules.
3. Keep handwritten Java mappers as the default when they are short, explicit,
   or contain domain-specific decisions. Adopt MapStruct selectively only for
   repetitive, deterministic mappings between domain, persistence, application,
   web, or provider representations.
4. MapStruct lifecycle hooks and decorators may only enrich a mapping with
   deterministic, side-effect-free data. They must not perform authorization,
   tenant resolution, persistence, event publication, network calls, or business
   decisions.
5. Any future adoption must be isolated to one capability, include a compile
   and generated-source review, use explicit unmapped-target failures, and add a
   focused architecture/test fixture before expanding to other modules.

## Consequences

- The current build stays smaller and keeps domain code framework-free.
- Architecture rules remain visible and executable rather than depending on
  annotations that do not enforce the full dependency graph by themselves.
- Some repetitive mapping code remains handwritten until the cost of repetition
  justifies a capability-local MapStruct adoption.
- A future mapper migration is reversible and does not require a repository-wide
  generated-source change.

## Verification

- `DddHexagonalArchitectureTest` and `CrossModuleDependencyArchitectureTest`
  enforce the current dependency boundaries.
- `SchemaOwnershipTest`, `EventContractArchitectureTest`, and
  `NamingConventionArchitectureTest` enforce the capability contracts.
- `ModularityTest` verifies Spring Modulith structure and generated AsciiDoc/
  PlantUML documentation.
- MapStruct's official reference describes compile-time generated mappings and
  lifecycle callbacks; those callbacks are intentionally constrained here:
  <https://mapstruct.org/documentation/stable/reference/html/>.
- Spring Modulith remains the module verification and documentation authority:
  <https://docs.spring.io/spring-modulith/reference/>.
