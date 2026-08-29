# Data Model: Emme AI Platform

| Field | Value |
|---|---|
| Status | Draft |
| Source | [Technical specification](technical-specification.md) |
| Authoritative store | PostgreSQL |

## 1. Durable entities

| Entity | Key scope | Purpose |
|---|---|---|
| `conversation` | tenant + conversation | Conversation aggregate |
| `conversation_message` | tenant + conversation + message | Complete message history |
| `ai_workflow_run` | tenant + workflow | Workflow lifecycle and versions |
| `ai_workflow_checkpoint` | tenant + workflow + checkpoint | LangGraph resume state |
| `ai_extraction_result` | tenant + workflow + extraction | Typed model output and validation |
| `quote_draft` | tenant + workflow + quote | Deterministic quote candidate |
| `quote_review_task` | tenant + review task | Staff approval state |
| `quote_review_decision` | tenant + review + version | Reviewer, timestamp, edits, outcome |
| `ai_tool_call` | tenant + workflow + call | Tool request, policy, result, audit |
| `ai_tool_idempotency` | tenant + principal + operation key | Mutation claim, lease, and authoritative replay result |
| `ai_model_execution` | tenant + workflow + execution | Provider, model, tokens, latency, cost |
| `ai_trace` | tenant + workflow + trace | Evaluation and learning evidence |
| `intent_reference` | global/tenant + version | Labeled classifier examples |
| `tool_reference` | global/tenant + version | Approved tool examples |
| `semantic_cache_entry` | global/tenant/user + version | Safe semantic response cache |
| `learning_candidate` | global/tenant + candidate | Governed self-improvement candidate |
| `knowledge_document` | tenant + document | Unstructured source metadata |
| `knowledge_chunk` | tenant + document + chunk | pgvector retrieval content |
| `ai_age_graph_registry` | tenant + graph | Derived AGE graph status and projection version |

## 2. Shared AI metadata

Every AI record that can affect behavior includes:

```text
tenant_id
conversation_id
workflow_id
model_version
prompt_version
embedding_model_version
index_version
created_at
updated_at
```

## 3. Vector records

Intent, tool, cache, document, catalog, and design vectors use separate tables
or clearly separate index namespaces. Each vector record includes:

```text
embedding vector(approved_dimension)
embedding_model
embedding_version
locale
scope
quality_status
```

Queries must filter by tenant/scope and embedding model/version before ordering
by similarity.

## 4. Redis key conventions

```text
session:{userId}
ai:thread:{tenantId}:{conversationId}
ai:lock:{tenantId}:{conversationId}
quote:cache:{tenantId}:{inputHash}:{templateVersion}
rate:{tenantId}:{userId}
review:{tenantId}:{reviewTaskId}
stream:{tenantId}:{conversationId}:events
ai:singleflight:{tenantId}:{cacheFingerprint}
```

Redis keys are temporary and never replace durable PostgreSQL records.

The optional Apache AGE graph is a disposable PostgreSQL read model. Its
registry is tenant-scoped and records the backend-derived graph name,
projection version, and last successful projection. Graph nodes duplicate only
relationship fields needed for curated recommendations and include the tenant
identifier. Transactional services, prices, appointments, approvals, and
audit records remain relational and authoritative.

The optional Redis vector projections use separate index namespaces:

```text
index: emme-ai-semantic-cache
prefix: emme:ai:semantic-cache:

index: emme-ai-semantic-cache-tools
prefix: emme:ai:semantic-cache:tools:
```

The cache index stores tenant, principal, cache kind, context fingerprint,
prompt version, embedding model version, durable cache id, response payload,
and expiry metadata. The six tag values are stored as URL-safe, reversible
encodings because Redis tag syntax has reserved characters; the original
values remain authoritative in PostgreSQL. The durable id and response
payload are indexed metadata fields because Spring AI returns only configured
metadata fields from a vector search. The tool index stores only Spring AI
tool references scoped by the composite backend session key. Both indexes are
rebuildable from PostgreSQL/application callbacks and are disabled by default.

## 5. Integrity rules

- Tenant-scoped foreign keys and indexes are required.
- Approval decisions use optimistic versioning.
- Workflow updates are idempotent by workflow and node execution identity.
- Tool mutations require an idempotency key.
- Mutation tool claims are unique per tenant, authenticated principal, and
  operation key; completed
  replays return the durable result without executing the handler again.
- A failed mutation releases only its in-progress claim; a failed completion
  write remains fail-closed until the claim lease expires. The next authorized
  request may then reclaim the stale `IN_PROGRESS` row. A lease is crash
  recovery, not proof that an external side effect did not complete, so
  application mutation commands must remain idempotent.
- Cache entries require dependency-version fields and expiry.
- Candidate promotion changes an index pointer, not individual active rows.
- Audit records retain actor identity and outcome.
- AGE graph traversal is limited to allowlisted relationship paths and is
  tenant-bound by the authenticated AI execution context.
- AGE unavailability or stale derived data must degrade to PostgreSQL/pgvector
  or no recommendation; it must not block authoritative transactions.
