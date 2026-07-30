# Knowledge and RAG Use Cases

| Use Case | Requirements | Preconditions | Successful Outcome |
|---|---|---|---|
| Ingest tenant document | FR-061–FR-064 | Manager is authorized; file type and size are accepted. | Document is converted, chunked, embedded, keyword-indexed, and reaches a visible terminal status. |
| Retrieve tenant knowledge | FR-065 | Trusted tenant context and query are present. | Retrieval filters tenant rows before ranking and returns authorized chunks only. |
| Project semantic change | FR-076 | An authoritative transaction commits a projection-relevant domain event. | Idempotent workers update the tenant's vector and graph projections after commit. |
| Reconcile projection | FR-077 | Operator selects a tenant or scheduled reconciliation detects drift. | Missing, stale, or corrupt projections are rebuilt from authoritative relational records. |

## Boundary Rules

- PostgreSQL and pgvector own metadata and embeddings.
- Object storage owns originals; signed URLs are not durable domain state.
- Failed ingestion never exposes partially indexed content as ready.
- RAG cannot answer authoritative price or availability questions without structured tools.
- Apache AGE is a disposable read model and never accepts authoritative business writes.
