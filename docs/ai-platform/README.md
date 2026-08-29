# Emme AI Platform Documentation

| Field | Value |
|---|---|
| Repository | `emme-service` |
| Status | Implementation in progress |
| Runtime | Java 25, Spring Boot/Spring Modulith |
| Deployable boundary | One backend application |
| Durable store | PostgreSQL and pgvector |
| Operational store | Redis |
| Optional relationship read model | Apache AGE in PostgreSQL |

This directory decomposes the Emme AI platform into independently reviewable
parts. The platform remains inside `emme-service`; it is not a second AI
service.

## Document map

| Part | Document | Purpose |
|---|---|---|
| Architecture | [Master design](../superpowers/specs/2026-08-27-ai-platform-semantic-architecture-design.md) | Integrated architecture and constraints |
| Product | [PRD](PRD.md) | Product goals, scope, users, and outcomes |
| Requirements | [Functional requirements](requirements.md) | Testable product and platform requirements |
| Requirements | [Non-functional requirements](non-functional-requirements.md) | Reliability, security, performance, and operations |
| Behavior | [Functional specification](functional-specification.md) | Request flows and state transitions |
| Engineering | [Technical specification](technical-specification.md) | Modules, ports, adapters, and contracts |
| Data | [Data model](data-model.md) | PostgreSQL, pgvector, Redis, and audit data |
| Evaluation | [Evaluation specification](evaluation-specification.md) | Datasets, metrics, gates, and promotion |
| Operations | [Operational runbook](operational-runbook.md) | Deployment, monitoring, failures, and rollback |
| Execution | [Implementation plan](implementation-plan.md) | Ordered implementation tasks and verification |
| Dependencies | [Compatibility baseline](dependency-compatibility.md) | Pinned Spring AI and LangGraph4j versions |
| Decisions | [ADR index](adr/README.md) | Architecture decisions and trade-offs |
| Changes | [FCR index](fcr/README.md) | Feature Change Requests |

## Implementation structure

```text
libraries/kernel/
  execution context and low-level context bridges

libraries/ai-contracts/
  framework-neutral model, embedding, extraction, routing, tool, RAG,
  semantic-cache, and workflow contracts

modules/ai-platform/
  reusable model providers and capability adapters
  bounded model admission and provider configuration
  no dependency on Emme assistant use cases

modules/assistant/adapter/out/persistence/
  tenant/principal-scoped pgvector adapters

modules/assistant/
  conversation orchestration
  LangGraph4j workflow definitions
  Spring AI clients, advisors, and Emme-specific composition
  quote/HITL workflow
  semantic gateway
  optional Apache AGE projection and curated graph retrieval adapters

modules/catalog/
  catalog and design matching use cases

modules/documents/
  tenant-scoped knowledge ingestion and retrieval

modules/appointments/
  deterministic availability and appointment use cases

libraries/observability-support/
  correlation, metrics, tracing, and redaction support

applications/emme-platform/
  provider beans, advisor beans, executors, agents, and composition root
```

## Implementation order

```text
Java 25 baseline
  → execution context and concurrency
  → ai-contracts and ai-platform provider adapters
  → pgvector semantic indexes
  → LangGraph4j workflow/checkpoints
  → Spring AI clients/advisors/tools
  → quote extraction and HITL
  → online enrichment/evaluation
  → channels and operational hardening
```

The specification set is approved for incremental implementation. Production
capabilities are claimed only when their code and focused verification are
present; remaining phases are tracked in the implementation plan.

## Optional AGE graph runtime

Apache AGE is a derived relationship read model, not a replacement for
PostgreSQL transactional tables or pgvector. It is disabled by default. The
adapter derives the graph name from the authenticated tenant context and only
supports curated traversal queries; callers cannot supply a graph name or
execute arbitrary Cypher.

To run the optional local database image, build and start the existing JVM
Compose stack with the AGE overlay:

```shell
docker compose -f deployment/compose/compose.yaml \
  -f deployment/compose/compose.runtime-jvm.yaml \
  -f deployment/compose/compose.age.yaml build postgres
docker compose -f deployment/compose/compose.yaml \
  -f deployment/compose/compose.runtime-jvm.yaml \
  -f deployment/compose/compose.age.yaml up -d
```

The overlay combines the official Apache AGE PostgreSQL 17 image with the
official pgvector 0.8.6 extension artifacts. Use `docker-compose` when that is
the executable installed on the host. The AGE flag can also be enabled with
`EMME_AI_AGE_ENABLED=true` while retaining the normal pgvector image; the
application then fails safely closed for graph operations when AGE is absent.
