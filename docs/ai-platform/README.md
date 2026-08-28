# Emme AI Platform Documentation

| Field | Value |
|---|---|
| Repository | `emme-service` |
| Status | Implementation in progress |
| Runtime | Java 25, Spring Boot/Spring Modulith |
| Deployable boundary | One backend application |
| Durable store | PostgreSQL and pgvector |
| Operational store | Redis |

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

modules/assistant/ai/
  provider-neutral AI contracts
  embedding and vector ports
  semantic decisions and tool policies
  Spring AI/provider adapters (incremental)

modules/assistant/adapter/out/persistence/
  tenant/principal-scoped pgvector adapters

modules/assistant/
  conversation orchestration
  LangGraph4j workflow
  Spring AI clients and advisors
  quote/HITL workflow
  semantic gateway

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
  → AI foundation and provider adapters
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
