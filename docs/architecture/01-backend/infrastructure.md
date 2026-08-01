# Outbound Adapters and Configuration

> **Naming contract:** Follow the [canonical architecture naming catalog](../00-project/naming-conventions.md) for package names, filenames, Java/Kotlin types, methods, and tests. Local examples on this page must not introduce a conflicting convention.

## Purpose

Infrastructure is a concern, not a top-level package in the canonical module layout. Technical implementations live under `adapter.out`; Spring composition lives under `configuration`. Together they contain persistence, messaging, provider, framework, and operational integrations while keeping technology outside the application/domain core.

## Typical ownership

```text
adapter/out/
├── persistence/
│   ├── entity/        # JPA/database representations
│   ├── repository/    # Spring Data/JDBC mechanics
│   ├── adapter/       # implements application repository port
│   ├── mapper/        # domain ↔ persistence
│   └── projection/    # bounded read models
├── messaging/
│   ├── publisher/
│   └── mapper/
├── client/
│   └── <provider>/    # client, provider DTOs, mapper, port adapter
└── observability/     # module-specific metrics/tracing adapters

configuration/         # Spring wiring and typed configuration
```

```mermaid
flowchart LR
    CORE[Application core] --> PORT[application.port.out]
    DB[Persistence adapter] -.implements.-> PORT
    EXT[Provider/client adapter] -.implements.-> PORT
    MSG[Messaging adapter] -.implements.-> PORT
    DB --> POSTGRES[(PostgreSQL)]
    EXT --> VENDOR[External system]
    MSG --> BROKER[(Broker)]
```

## Adapter rules

- Implement stable ports defined in `application.port.out`; domain code does not own infrastructure ports.
- Translate vendor errors into application-specific failures.
- Keep credentials and endpoint configuration outside code.
- Make retries, timeouts, idempotency, and circuit behavior explicit.
- Do not leak JPA entities, SDK models, or raw provider responses across the module boundary.
- Use testcontainers or contract tests for infrastructure behavior that an in-memory fake cannot prove.

## Composition

The module's `configuration` package is the composition root that wires concrete adapters. Dependency injection is the default; services depend on application-owned ports rather than instantiate providers internally. `@ApplicationModule` remains in the module-root `package-info.java`, not in `configuration`.

## Adapter guardrails

### Persistence

- Keep persistence models and repositories inside the outbound boundary.
- Enforce tenant scope and transaction semantics at the adapter/database boundary.
- Define indexes, query limits, pagination, locking, and migration compatibility for production paths.
- Verify database-specific behavior with real PostgreSQL integration tests.

### External providers

- Configure connect/read timeouts, bounded retries, backoff, circuit behavior, and rate limits.
- Validate provider responses and map provider error codes into stable application failures.
- Redact request/response data according to classification.
- Use idempotency keys for provider mutations and persist correlation/provider IDs.
- Provide a fake for unit tests and a contract/sandbox test where the provider is business-critical.

### Configuration and security

- Bind typed configuration and validate it at startup.
- Keep credentials in managed secret stores and rotate without source changes.
- Use least-privilege database and provider identities.
- Do not make network calls or read mutable environment state in static initializers.

### Naming

- Application ports use capability names such as `PricingPort` or `QuoteRepository`.
- Concrete adapters state the technology or strategy: `PricingClientAdapter`, `QuotePersistenceAdapter`, `KafkaQuoteEventPublisher`.
- Framework repositories are visibly technical: `SpringDataQuoteRepository`.
- Provider request/response types remain inside that provider's package.
- Do not use `InfrastructureService`, `RepositoryImpl`, or `DefaultClient`; these names hide the actual adapter role.

### Infrastructure checklist

- [ ] Every adapter implements a port owned by an inner layer.
- [ ] Persistence and provider failure modes have explicit mappings.
- [ ] Timeouts, retries, circuit behavior, and idempotency are tested.
- [ ] Tenant and data-classification controls are enforced and redacted in logs.
- [ ] Real-boundary integration tests cover production-specific behavior.

For module-wide security, tenancy, migration, resilience, and operational approval, use the [module template](../../templates/module-package-structure-template.md).
