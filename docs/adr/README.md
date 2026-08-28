# Architecture Decision Records

ADRs record consequential service decisions. They explain why a choice was made,
what alternatives were rejected, and how the decision is verified.

## Required for

- module or repository boundaries;
- authentication, authorization, data ownership, consistency, and event delivery;
- durable technology or infrastructure choices;
- release, migration, rollback, or security trade-offs.

## Lifecycle

```text
Proposed → Accepted → Superseded
                    ↘ Deprecated
```

Do not delete historical ADRs. A changed decision creates a new ADR that links
to the decision it supersedes.

## Existing records

- [ADR-0001: Build-logic convention plugins](0001-build-logic-convention-plugins.md)
- [ADR-0002: Deployment strategy pattern](0002-deployment-strategy-pattern.md)
- [ADR-0003: Trusted proxy boundary for Identity rate limiting](0003-identity-login-rate-limit-client-ip.md)
- [ADR-0004: Shared and Audit ownership](0004-shared-and-audit-ownership.md)
- [ADR-0005: Spring Modulith event streaming through Kafka](0005-spring-modulith-kafka-event-streaming.md)
- [ADR-0006: Low-cost MVP runtime boundary](0006-mvp-low-cost-runtime-boundary.md)
- [ADR-0007: Selective architecture metadata and mapping generation](0007-selective-architecture-metadata-and-mapping.md)
- [ADR-0009: Java 25 and structured concurrency](0009-ai-java25-structured-concurrency.md)
- [ADR-0010: Spring AI and LangGraph4j boundary](0010-ai-spring-ai-langgraph-boundary.md)
- [ADR-0011: PostgreSQL/pgvector semantic store](0011-ai-vector-store-strategy.md)
- [ADR-0012: Governed online enrichment](0012-ai-governed-online-enrichment.md)

## Index

| ADR | Title | Status | Date |
|---|---|---|---|
| [0001](0001-build-logic-convention-plugins.md) | Precompiled convention plugins for build logic | Accepted | 2026-07-10 |
| [0002](0002-deployment-strategy-pattern.md) | Strategy pattern for deployment | Accepted | 2026-07-10 |
| [0003](0003-identity-login-rate-limit-client-ip.md) | Trust forwarded client IPs only from configured proxies | Accepted | 2026-08-01 |
| [0004](0004-shared-and-audit-ownership.md) | Shared technical capability and reserved Audit ownership | Accepted | 2026-08-01 |
| [0005](0005-spring-modulith-kafka-event-streaming.md) | Spring Modulith event streaming through Kafka | Accepted | 2026-08-02 |
| [0006](0006-mvp-low-cost-runtime-boundary.md) | Low-cost MVP runtime boundary | Accepted with release gates open | 2026-08-03 |
| [0007](0007-selective-architecture-metadata-and-mapping.md) | Selective architecture metadata and mapping generation | Accepted | 2026-08-04 |
| [0009](0009-ai-java25-structured-concurrency.md) | Java 25 and structured concurrency | Proposed | 2026-08-27 |
| [0010](0010-ai-spring-ai-langgraph-boundary.md) | Spring AI and LangGraph4j boundary | Proposed | 2026-08-27 |
| [0011](0011-ai-vector-store-strategy.md) | PostgreSQL/pgvector semantic store | Proposed | 2026-08-27 |
| [0012](0012-ai-governed-online-enrichment.md) | Governed online enrichment | Proposed | 2026-08-27 |

Create a new ADR when a decision changes. Do not rewrite an accepted ADR to
hide its historical rationale.
