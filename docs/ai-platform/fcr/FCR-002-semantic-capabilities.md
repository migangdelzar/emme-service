# FCR-002: Semantic Capabilities

## Change requested

Add vector-search-based semantic classification, proactive tool selection, and
semantic caching with deterministic fallback behavior.

## Affected areas

```text
ai-foundation ports
assistant semantic gateway
PostgreSQL/pgvector migrations
Spring AI EmbeddingModel/VectorStore adapters
catalog/documents search boundaries
Redis exact cache and locks
```

## Acceptance

- Deterministic routes execute before vector and LLM routes.
- Low score/margin or invalid tenant context abstains.
- Tool vectors cannot bypass application authorization.
- Cache hits are scope, freshness, privacy, and version validated.
- The active embedding model/version is identical for indexing and querying.
