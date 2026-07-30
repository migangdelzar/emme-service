# Conversation and AI Use Cases

| Use Case | Requirements | Preconditions | Successful Outcome |
|---|---|---|---|
| Enter through a channel | FR-042–FR-046 | Channel binding resolves an active tenant; callback authenticity is valid. | One normalized message is accepted exactly once. |
| Normalize multimodal input | FR-047–FR-049 | Media type and size are allowed; content can be fetched securely. | Voice, images, and text become validated normalized input. |
| Detect and route intent | FR-050 | Normalized message and trusted context are available. | Intents select bounded tools without persistence access. |
| Recommend and answer | FR-051–FR-053 | Tenant catalog or knowledge index is available. | Response is grounded in authorized tenant data. |
| Conduct booking dialogue | FR-054–FR-057 | Required facts can be gathered; pending state is durable. | Draft is confirmed, executed once, cancelled, or expires safely. |
| Preserve conversation state | FR-058, FR-059 | Retention policy and tenant context are known. | Durable history and expiring summaries remain tenant isolated. |
| Degrade safely | FR-060 | AI provider is unavailable or returns invalid output. | No consequential action executes; an actionable fallback is returned. |

## Boundary Rules

- Spring AI adapters call module public APIs, never repositories.
- Model output is untrusted and validated before tool execution.
- Prompt content cannot change tenant or actor context.
- Structured catalog and availability tools override generated claims.
