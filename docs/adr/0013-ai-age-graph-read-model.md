# ADR-0013: Optional Apache AGE Relationship Read Model

## Status

Accepted

## Date

2026-08-29

## Context

Emme needs relationship-based design recommendations, but transactional salon
data must remain in PostgreSQL and semantic retrieval already has pgvector.
Apache AGE provides graph traversal inside PostgreSQL, but its graph namespace
and Cypher execution model introduce isolation and operational risks if they
are exposed directly to a model or frontend.

## Decision

- Use Apache AGE only as an optional, disposable relationship read model.
- Keep PostgreSQL relational tables authoritative for services, prices, staff,
  appointments, policies, clients, approvals, and audit records.
- Derive the AGE graph name exclusively from the authenticated backend tenant
  context. Never accept a graph name or tenant ID from the LLM or frontend.
- Project only allowlisted node and relationship types through typed contracts.
- Expose curated traversal queries such as design-to-service; do not accept
  unrestricted LLM-generated Cypher.
- Run AGE graph operations through the existing tenant-aware JDBC boundary and
  persist projection status in `ai_age_graph_registry`.
- Keep the AGE extension and application graph feature disabled by default.
  The normal PostgreSQL runtime remains the official pgvector image.
- Provide an explicit local AGE+pgvector image overlay by combining official
  PostgreSQL 17/trixie extension artifacts. Production images require the
  normal image review, digest pinning, and release evidence.

## Consequences

Positive:

- Relationship retrieval can be added without moving transactional data out of
  PostgreSQL.
- A missing or stale graph degrades recommendations only; it cannot change
  price, availability, permissions, or appointment outcomes.
- Tenant isolation is enforced by backend context, graph naming, node tenant
  properties, and the relational registry.

Trade-offs:

- AGE graph projection must be rebuilt when relational source data changes.
- The current slice provides the projection/retrieval boundary and curated
  adapter; durable post-commit catalog event wiring remains a later increment
  because the current catalog APIs do not expose those projection events.
- AGE and pgvector require a reviewed combined database image when both are
  enabled in local or production environments.

## Verification

- Migration contract verifies the optional extension guard, tenant registry,
  and RLS policy.
- Adapter unit tests verify context binding and unavailable behavior.
- AGE Testcontainers tests verify idempotent projection, curated traversal,
  and two-tenant graph-name isolation.
- Compose contract and image smoke checks verify the opt-in runtime contains
  both `age` and `vector`.
