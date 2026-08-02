# Module API and HTTP Boundary

> **Naming contract:** Follow the [canonical architecture naming catalog](../00-project/naming-conventions.md) for package names, filenames, Java/Kotlin types, methods, and tests. Local examples on this page must not introduce a conflicting convention.

## Purpose

The word API describes two different contracts that must remain separate:

1. The `<module>.api.*` Java packages form the public module contract and are exported through the `<module> :: api` named interface.
2. `adapter.in.web` is the external HTTP contract presented to clients.

The module API contains commands, queries, results, use-case interfaces, events, expected exceptions, and stable public types. The web adapter translates HTTP requests into that contract and module results into stable HTTP responses. Neither boundary owns business decisions.

```mermaid
flowchart LR
    CLIENT[External client] --> HTTP[adapter.in.web\nrequest · response · controller]
    HTTP --> MODULEAPI[Java API packages\nnamed interface: module :: api]
    OTHER[Another module] --> MODULEAPI
    APP[application.service] -.implements use cases.-> MODULEAPI
    APP --> DOMAIN[domain]

    OTHER -.must not import.-> HTTP
    OTHER -.must not import.-> DOMAIN
```

## Module API structure

```text
api/
├── command/      # please do this
├── query/        # please tell me this
├── result/       # here is the module answer
├── usecase/      # this capability is available
├── event/        # this already happened
├── exception/    # this expected operation failed
└── type/         # stable public vocabulary
```

Every materialized child package has its own responsibility-focused `package-info.java` and joins the logical `module :: api` named interface. Whenever `api.event` exists, it also joins `module :: events`; every `api.type` used in an event signature joins that same interface. The complete comments and filename rules are copy-ready in the [module source catalog](../../templates/module-package-structure-template.md#appendix-a-copy-ready-module-source-catalog).

## HTTP adapter flow

```text
HTTP request
  ↓ authentication / tenant context / validation
web controller
  ↓ command or query
application use case
  ↓ domain behavior + ports
result / error
  ↓ response mapping
HTTP response
```

```mermaid
sequenceDiagram
    participant C as Client
    participant W as Web adapter
    participant A as Application use case
    participant D as Domain
    participant R as Repository port

    C->>W: Request + credentials
    W->>W: Authenticate, tenant, validate
    W->>A: Command / query
    A->>D: Execute invariant-preserving behavior
    A->>R: Load or persist through port
    R-->>A: Result
    A-->>W: Application result / failure
    W-->>C: Versioned response / problem
```

## Module API rules

- Expose use-case interfaces, not application service implementations.
- Keep commands, queries, results, events, exceptions, and types immutable and implementation-neutral.
- Do not expose aggregates, JPA entities, provider DTOs, controllers, or raw infrastructure failures.
- Name commands imperatively, events in past tense, and result shapes consistently.
- Treat a named-interface change as a compatibility change.

## HTTP API rules

- Version public routes deliberately, for example `/api/v1/...`.
- Validate syntax and boundary constraints at the edge.
- Apply the [backend validation conventions](validation.md): Jakarta constraints belong on inbound request/message models, while domain and cross-aggregate rules remain in their owning layers.
- Resolve authentication and tenant context before entering application behavior.
- Use request and response DTOs; do not expose persistence entities.
- Return consistent error envelopes with a correlation identifier.
- Keep pagination, filtering, idempotency, and concurrency semantics explicit.
- Document public endpoints through OpenAPI or equivalent generated API documentation.

### Spring MVC endpoint versioning

Spring Framework 7 provides first-class endpoint version conditions through the
`version` attribute on `@RequestMapping` and its composed mappings such as
`@GetMapping` and `@PostMapping`. The application must configure one explicit
`ApiVersionStrategy` and one resolver before controllers use that attribute.

Use the repository's two-level policy:

| Boundary | Policy | Example |
|---|---|---|
| Public URI and OpenAPI major contract | Keep the stable major path visible to clients | `/api/v1/identity/memberships` |
| Spring controller compatibility mapping | Use Spring's `version` attribute when two representations of the same route must coexist | `@GetMapping(path = "/memberships", version = "1.1")` |

```java
@RestController
@RequestMapping("/api/v1/identity")
final class IdentityController {

  @GetMapping(path = "/memberships", version = "1.0")
  MembershipResponse membershipsV1() {
    // Stable v1 representation.
  }

  @GetMapping(path = "/memberships", version = "1.1+")
  MembershipResponse membershipsV1_1() {
    // Compatible baseline for later 1.x requests.
  }
}
```

The version source may be a path segment, request header, query parameter, or
media-type parameter, but a service chooses one canonical resolver for its
public API. Do not silently combine multiple version sources. A missing or
unsupported version must produce the configured client error, and version
selection must be covered by MockMvc contract tests.

For the current service, `/api/v1` remains the externally visible major
contract. Spring mapping versions are introduced only when an endpoint needs
parallel compatibility behavior; they do not justify changing a stable route
or duplicating controllers prematurely. See the official [Spring MVC API
versioning reference](https://docs.spring.io/spring-framework/reference/web/webmvc-versioning.html).

## Error mapping

| Condition | HTTP result |
|---|---:|
| Invalid request shape | `400` |
| Missing or invalid authentication | `401` |
| Authenticated but not allowed | `403` |
| Resource not found within authorized scope | `404` |
| Business rule conflict | `409` |
| Domain validation failure | `422` |
| Unexpected failure | `500` with no internal detail |

The exact status may be specialized by the API contract, but the mapping must be stable and tested.

## API evolution and security

### Versioning and compatibility

- Version externally consumed routes and schemas deliberately.
- Prefer the visible `/api/v<major>` route for the public major contract and
  Spring MVC `@RequestMapping(version = "...")` for parallel handler
  compatibility within that contract.
- Prefer additive changes; deprecate before removal.
- Do not expose JPA entities, domain aggregates, provider responses, or internal exceptions.
- Define field nullability, default behavior, enum evolution, pagination, sorting, and maximum page size.
- Use consumer-driven or schema contract tests for APIs with independent consumers.

### Security and tenancy

- Authenticate before application use-case execution.
- Authorize the requested action at the application boundary, not only at the controller annotation.
- Resolve tenant context from trusted identity/session context; do not trust a body field alone.
- Enforce tenant scope in repositories and integration queries as defense in depth.
- Apply rate limits, request-size limits, and abuse controls to public or expensive endpoints.

### Mutation safety

For state-changing endpoints, document:

```text
authorization → validation → idempotency key → transaction → response
```

Define duplicate-submit behavior, optimistic-concurrency behavior, and whether the response represents committed state or an accepted asynchronous command.

### API checklist

- [ ] Every public Java type lives in the correct `api.*` kind and follows the canonical filename pattern.
- [ ] Every materialized API-kind package declares the intended Spring Modulith named interface.
- [ ] Other modules import only named module API contracts, never web or implementation packages.
- [ ] OpenAPI/schema documentation matches implementation.
- [ ] Authentication, authorization, and tenant tests cover positive and negative paths.
- [ ] Error responses use a stable problem/error code and correlation ID.
- [ ] Pagination and request limits prevent unbounded work.
- [ ] Mutation idempotency and concurrency semantics are documented.
- [ ] Compatibility tests protect existing consumers.

For project-level release gates, use the [application template](../../templates/modulith-application-template.md). For module-level approval, use the [module template](../../templates/module-package-structure-template.md).
