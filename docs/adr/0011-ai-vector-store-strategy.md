# ADR-0011: PostgreSQL/pgvector as Initial Semantic Store

## Status

Accepted

## Date

2026-08-27

## Context

The repository already uses PostgreSQL/pgvector for tenant-filtered catalog and
document search. The shared local and integration-test Redis runtime is pinned
to `redis:8.10.1-alpine3.23`, which includes the Redis Query Engine required by
Spring AI's Redis vector-store integration. Semantic Redis remains opt-in at
the application level.

## Decision

- Use PostgreSQL/pgvector for durable intent, tool-reference, cache, document,
  catalog, and design vectors.
- Keep classification, tool references, and cache in separate indexes/tables.
- Use Redis for locks, temporary workflow state, live events, rate limits, and
  exact hot-cache acceleration.
- Use the pinned Redis 8 runtime for the opt-in Redis vector adapter and keep
  PostgreSQL authoritative for durable semantic records.
- Use the same embedding model/version/dimension for indexing and querying.

## Consequences

- Fewer infrastructure systems and strong existing tenant-query patterns.
- Semantic cache latency must be measured against the target workload.
- Redis semantic acceleration can be enabled without changing application
  ports; if it is disabled or unavailable, PostgreSQL/pgvector remains the
  fallback path.
