# ADR-0011: PostgreSQL/pgvector as Initial Semantic Store

## Status

Proposed

## Date

2026-08-27

## Context

The repository already uses PostgreSQL/pgvector for tenant-filtered catalog and
document search. Redis is currently deployed as plain `redis:7-alpine` without a
verified Redis Vector Search module.

## Decision

- Use PostgreSQL/pgvector for durable intent, tool-reference, cache, document,
  catalog, and design vectors.
- Keep classification, tool references, and cache in separate indexes/tables.
- Use Redis for locks, temporary workflow state, live events, rate limits, and
  exact hot-cache acceleration.
- Add a Redis Vector adapter only after a Redis Stack/RediSearch compatibility
  and operational spike.
- Use the same embedding model/version/dimension for indexing and querying.

## Consequences

- Fewer infrastructure systems and strong existing tenant-query patterns.
- Semantic cache latency must be measured against the target workload.
- Redis can be introduced as an optimization without changing application
  ports.
