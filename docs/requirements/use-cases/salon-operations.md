# Salon Operations Use Cases

| Use Case | Requirements | Preconditions | Successful Outcome |
|---|---|---|---|
| Review business dashboard | FR-018 | User has dashboard permission and an active tenant. | Tenant-scoped operational and revenue summaries are displayed. |
| Maintain service catalog | FR-019–FR-023 | User has catalog permission. | Services and artist capabilities change without damaging history. |
| Maintain customer records | FR-024–FR-028 | User has customer permission. | Tenant-scoped customer data and visit history follow retention rules. |
| Review finances | FR-038, FR-039 | User has financial-report permission. | Tenant-scoped period metrics and approved exports are produced. |
| Configure business | FR-040, FR-041 | Owner is authorized; external consent succeeds where required. | Business hours, policies, notifications, and approved calendar connections are updated. |

## Boundary Rules

- Catalog values and finance totals come from structured SQL data.
- Retired records remain referentially valid for history.
- Provider SDKs remain outbound adapters.
