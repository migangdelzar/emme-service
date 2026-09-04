# Naming and Vector Ingestion Design

| Field | Detail |
|---|---|
| Date | 2026-09-04 |
| Scope | AI contracts, `ai-platform`, assistant, PostgreSQL/pgvector, Redis, AGE, events, and CDC boundaries |
| Status | Approved baseline for staged implementation; written-spec review pending |
| Related design | [`2026-09-03-repository-framework-first-refactoring-design.md`](2026-09-03-repository-framework-first-refactoring-design.md) |
| Related plan | [`2026-09-04-repository-framework-first-refactoring.md`](../plans/2026-09-04-repository-framework-first-refactoring.md) |

## 1. Decision summary

The repository will use responsibility-first names and one qualified JDBC client
per data source. Application ports and domain models remain provider-neutral.
PostgreSQL-specific adapters may expose `JdbcClient` internally, but their
class names should describe the actual boundary (`Postgres...`) rather than the
mechanism (`Jdbc...`) once each adapter is migrated and verified.

Vector data is a derived projection, not business authority. The authoritative
transaction publishes a semantic projection event. An asynchronous,
tenant-aware projector reads the committed source, canonicalizes and chunks
content, generates embeddings, and idempotently updates PostgreSQL/pgvector.
Redis is an optional disposable hot projection. Reconciliation and backfill
remain mandatory.

PostgreSQL logical decoding and Debezium CDC are reserved for a later scale or
multi-writer boundary. They are not the primary v1 projection mechanism.

## 2. Naming standard

### 2.1 Composition roots and clients

| Current name | Target name | Reason |
|---|---|---|
| `aiTenantJdbcClient` | `tenantJdbcClient` | The bean is identified by the data source it wraps; it is already shared by multiple AI adapters. |
| `SpringAiTenantJdbcConfiguration` | `TenantJdbcClientConfiguration` | Removes framework branding from a data-source composition root. |
| `coreJdbcClient` | `coreJdbcClient` | Already concise and unambiguous because it is qualified to `coreDataSource`. |
| `SpringAiAgeConfiguration` | `AgeGraphConfiguration` | Names the capability rather than the framework that happens to wire it. |

Bean names are technical composition details. They must not appear in
`ai-contracts`, domain packages, application ports, or public API payloads.
Every client injection uses an explicit qualifier when more than one data
source exists.

### 2.2 PostgreSQL-specific adapters

Rename only as each adapter is migrated and its callers/tests are updated in
one atomic slice:

| Current name | Target name |
|---|---|
| `JdbcAgeGraphClient` | `PostgresAgeGraphClient` |
| `JdbcAiJobStatusStore` | `PostgresAiJobStatusStore` |
| `JdbcAiToolIdempotencyStore` | `PostgresAiToolIdempotencyStore` |
| `JdbcLangGraphCheckpointSaver` | `PostgresLangGraphCheckpointSaver` |
| `JdbcSemanticCacheAdapter` | `PostgresSemanticCacheAdapter` |
| `JdbcSemanticReferenceSearchAdapter` | `PostgresSemanticReferenceSearchAdapter` |
| `JdbcQuoteWorkflowRepository` | `PostgresQuoteWorkflowRepository` if it remains a JDBC survivor |
| `JdbcQuoteReviewRepository` | `PostgresQuoteReviewRepository` if it remains a JDBC survivor |
| `JdbcQuoteArtifactRepository` | `PostgresQuoteArtifactRepository` if it remains a JDBC survivor |
| `HybridSearch` | `PostgresHybridSearch` |

Stable CRUD that moves to JPA receives names such as
`SpringDataQuoteWorkflowRepository` only inside the adapter package; the
application port remains `QuoteWorkflowRepository`. `JdbcDesignImageMetadataRepository`
is a JPA candidate and will be evaluated before a name-only migration.

Do not add compatibility classes solely for renaming. If a class is not public
outside its module, rename it directly after caller and configuration searches.

## 3. Vector projection model

### 3.1 Ownership

```text
Business/domain tables       authoritative tenant data
Projection event/publication durable trigger for derived work
Projection worker            chunking, embedding, policy, retries
PostgreSQL/pgvector          durable semantic projection and source for RAG
Redis                        disposable hot cache/projection
AGE                          optional graph projection
Kafka                        external/high-scale transport only
```

The vector projection records must carry at least:

- tenant ID and source aggregate/document ID;
- source version or content fingerprint;
- chunk identity and chunk position;
- embedding model name, model version, and dimension;
- projection/index version and readiness state;
- safe metadata required for tenant, authorization, and retrieval filtering.

The active index is selected by version. A reindex writes a new version,
validates it, and switches the active pointer; it does not delete the active
projection first.

### 3.2 Population timing

| Timing | Use | Trade-off |
|---|---|---|
| Synchronous in the request transaction | Only when immediate searchability is a hard requirement and embedding is local/reliable | Adds provider latency and couples business commits to model outages; not the default |
| After commit, asynchronous projector | Default for documents, catalog, services, prices, templates, and knowledge | Seconds of eventual consistency, but protects OLTP and supports retry/replay |
| Lazy query-time embedding | Cache warming or explicitly disposable data | First-query latency and unpredictable availability; never the authoritative ingestion path |
| Batch backfill/reindex | Initial population, model changes, repair, and migration | Higher operational complexity; requires checkpoints, throttling, and version cutover |

The business transaction publishes only a compact, versioned event. The
projector re-reads canonical data after commit instead of trusting a large
event payload. This avoids stale payloads, reduces PII in messages, and keeps
the source of truth in PostgreSQL.

## 4. Event and projector flow

1. A module commits an authoritative change under its normal transaction.
2. It publishes an event containing event ID, tenant ID, aggregate ID, source
   version, change kind, and schema version.
3. Spring Modulith records the event publication with the transaction.
4. An asynchronous projector claims/retries the publication in a new
   transaction.
5. The projector validates tenant scope and source version, reads the current
   canonical record, applies redaction and chunking, and calls the configured
   embedding port.
6. It upserts the pgvector projection using the model/version/content identity.
7. It marks the projection ready and invalidates or refreshes the Redis hot
   projection.
8. A reconciliation job finds missing, stale, failed, or model-incompatible
   projections and resubmits bounded work.

Projectors are at-least-once and must be idempotent. A failed embedding or
vector write leaves the authoritative record committed and the projection
retryable; it must never expose a partially indexed document as ready.

## 5. CDC decision

PostgreSQL logical decoding can stream WAL changes, and Debezium’s PostgreSQL
connector consumes the standard `pgoutput` stream and publishes table changes
to Kafka. This is a valid future integration, but row-level CDC is not a
semantic domain event.

### 5.1 CDC is appropriate when

- external or legacy writers bypass application events;
- several independent applications write the same PostgreSQL database;
- projection throughput must scale independently through Kafka;
- downstream teams need a complete row-change stream;
- an initial snapshot plus continuous change stream is operationally justified.

### 5.2 CDC is not the default here because

- replication slots and WAL retention need active operational monitoring;
- updates/deletes require correct replica identity configuration;
- table schemas and database internals become integration contracts;
- row changes do not express authorization, redaction, chunking, or business
  meaning;
- raw change payloads can leak sensitive content into Kafka;
- duplicate, replay, snapshot, and read-after-write behavior still require
  idempotent projectors and reconciliation.

If CDC is introduced, the safe shape is:

```text
PostgreSQL logical decoding
        ↓
Debezium PostgreSQL connector (pgoutput)
        ↓
Kafka topic per bounded source contract
        ↓
sanitized semantic projection consumer
        ↓
PostgreSQL/pgvector + Redis invalidation
```

The CDC consumer must publish or transform into the same internal
`SemanticProjectionRequested` contract used by application events. It must not
create a second vector-ingestion implementation.

## 6. Alternatives and trade-offs

| Approach | Freshness | Reliability | Operational cost | Decision |
|---|---:|---:|---:|---|
| Direct synchronous embedding | Immediate | Low when providers fail | Low initially, high coupling | Reject as default |
| Database trigger to embedding queue | Near-immediate | Difficult to reason about | High database coupling | Reject |
| Modulith publication + projector | Seconds | Durable retry and replay | Low/medium | Adopt now |
| Polling only | Seconds/minutes | Recoverable but less precise | Medium query load | Use for reconciliation, not primary trigger |
| Debezium + Kafka CDC | Seconds | Strong replay capability | High | Reserve for multi-writer/scale boundary |

## 7. Security and failure gates

- Every event and projection query is tenant-scoped.
- CDC replication users are read-only and restricted to approved publications.
- Sensitive source content is not placed in Kafka unless explicitly redacted and
  authorized.
- Provider failures do not roll back the authoritative business transaction.
- Projection failures are visible through status, metrics, retry count, and
  reconciliation lag.
- Redis outage falls back to PostgreSQL/pgvector or durable cache behavior.
- Model/version/dimension mismatch fails closed and schedules reindex work.
- Deletes produce explicit projection removal/tombstone work.

## 8. Staged implementation plan

### Wave 1 — naming and composition

- Rename `aiTenantJdbcClient` to `tenantJdbcClient` and the configuration class.
- Add architecture tests for one qualified client per data source.
- Rename only the AGE and already-migrated atomic survivors.
- Update the migration ledger and all affected tests.

### Wave 2 — projection contract

- Define provider-neutral semantic projection event and projector ports.
- Add versioned event identity, idempotency, readiness, retry, and reconciliation
  contracts.
- Reuse Spring AI `EmbeddingModel`/`VectorStore` where the standard metadata
  contract fits; keep specialized PostgreSQL operations behind adapters.

### Wave 3 — authoritative event population

- Emit document/catalog semantic-change events after authoritative commits.
- Implement asynchronous projectors and bounded backfill.
- Add PostgreSQL/RLS, Redis outage, duplicate delivery, delete, and model
  migration integration tests.

### Wave 4 — optional CDC

- Introduce Debezium only after proving an application-event coverage gap or a
  measured throughput/multi-writer requirement.
- Start with one allow-listed publication and sanitized event transform.
- Feed the existing projector contract; do not duplicate vector logic.

### Wave 5 — remaining repository work

- Complete JPA-first quote/trace persistence classification and migrations.
- Complete LangGraph security/composition verification.
- Finish provider HTTP, domain persistence, Modulith/Kafka, build, deployment,
  performance, and Docker-enabled integration gates.

## 9. Acceptance criteria

- No duplicate JDBC client bean wraps the same data source.
- AI application ports contain no Spring, JDBC, JPA, Redis, Kafka, or provider
  SDK types.
- All feature-level SQL uses named-parameter `JdbcClient` or a documented
  lower-level bootstrap exception.
- Stable entity CRUD uses JPA only when it reduces code without losing tenant,
  locking, JSONB, or atomicity guarantees.
- Vector projections are asynchronous, idempotent, tenant-scoped, retryable,
  observable, and rebuildable.
- Redis is never required for durability or correctness.
- CDC, if enabled later, feeds the same projection contract and has monitored
  replication slots, sanitized payloads, and replay/reconciliation tests.

## 10. Sources

- [Spring Modulith application events and publication registry](https://docs.spring.io/spring-modulith/reference/events.html)
- [Spring AI PGVector store](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html)
- [Spring AI vector store overview](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- [PostgreSQL logical replication](https://www.postgresql.org/docs/current/logical-replication.html)
- [Debezium PostgreSQL connector](https://debezium.io/documentation/reference/stable/connectors/postgresql.html)
