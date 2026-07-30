# Identity and Tenancy Use Cases

| Use Case | Requirements | Preconditions | Successful Outcome |
|---|---|---|---|
| Authenticate user | FR-001, FR-002, FR-007 | User exists in Keycloak; client and redirect URI are approved. | A validated session is established or terminated; privileged users satisfy MFA. |
| Establish tenant workspace | FR-003, FR-004, FR-005 | User has an active membership or accesses an approved tenant host. | One trusted tenant context and current permission set are established. |
| Authorize an operation | FR-006 | Identity, tenant, and requested permission are known. | The operation proceeds only when the database-backed permission is present. |
| Provision tenant | FR-008, FR-017 | Platform administrator is authorized; slug/domain are unique. | Tenant, owner membership, defaults, and initial configuration are created. |
| Operate tenant lifecycle | FR-009–FR-016 | Tenant exists and requested transition is valid. | Configuration or lifecycle state changes atomically and is audited. |

## Boundary Rules

- External tenant IDs never establish trusted tenant context.
- Keycloak authenticates; PostgreSQL records decide authorization.
- Suspended and staged-deleted tenants cannot process normal business traffic.
- v1 provisions shared-schema RLS tenants only.
