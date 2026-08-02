# Frontend–Backend Integration

> **Naming contract:** Follow the [canonical architecture naming catalog](../00-project/naming-conventions.md) for package names, filenames, Java/Kotlin types, methods, and tests. Local examples on this page must not introduce a conflicting convention.

## Purpose

The frontend and backend are independently structured applications joined by explicit HTTP contracts. The frontend owns presentation and interaction; the backend owns authorization, tenancy, validation, and business truth.

## Request flow

```text
React feature
   ↓ typed API client
HTTP /api
   ↓ auth + tenant context
controller
   ↓ application use case
domain + infrastructure
```

```mermaid
sequenceDiagram
    participant UI as React feature
    participant CLIENT as Typed API client
    participant API as Backend API
    participant APP as Application use case
    participant DB as Module data

    UI->>CLIENT: Command / query
    CLIENT->>API: Authenticated request
    API->>API: Validate tenant + authorization
    API->>APP: Execute use case
    APP->>DB: Read/write through port
    DB-->>APP: Result
    APP-->>API: Contract result/error
    API-->>CLIENT: Versioned response
    CLIENT-->>UI: View state
```

## Contract rules

- Use versioned API routes.
- Generate or centrally maintain request/response types where practical.
- Treat backend validation and authorization as authoritative.
- Define loading, empty, validation, conflict, unauthorized, and unavailable states in the frontend.
- Propagate correlation IDs for support and tracing.
- Keep backend error codes stable enough for frontend behavior; keep human messages replaceable.
- Add contract tests for high-value endpoints and E2E tests for critical journeys.

## Local development

- Vite proxies API requests to the local backend.
- The backend exposes a documented health endpoint.
- Authentication and tenant fixtures are deterministic for tests.
- CORS, cookie, and token behavior is tested in the same shape used by local development.

## Integration guardrails

### API contract

- Keep OpenAPI/schema definitions versioned with the backend contract owner.
- Generate client types where practical, then adapt them to feature view models.
- Detect breaking schema changes in CI before deployment.
- Define maximum request/response sizes, pagination, timeout, and rate-limit behavior.

### Authentication and tenancy

- Use the repository-approved OAuth/session/token flow; never invent a parallel browser credential store.
- Propagate tenant identity from trusted claims/session context, not arbitrary client fields.
- Define behavior for expired tokens, revoked sessions, tenant switching, and insufficient permissions.
- Keep CORS, CSRF, cookie, and header policy consistent across local, CI, stage, and production.

### Failure and consistency

```text
frontend request
  → timeout / cancellation
  → typed success or stable error code
  → retry only safe/idempotent operations
  → user-visible recovery or support correlation
```

Document whether a successful mutation means committed state, accepted asynchronous work, or an intermediate status. The frontend must not display success when the backend has only accepted a request for later processing.

### Integration checklist

- [ ] Schema/client compatibility is checked before release.
- [ ] Auth, tenant, CORS/CSRF, and session expiry are tested end to end.
- [ ] Error codes map to explicit UI states.
- [ ] Retry/cancellation behavior does not duplicate mutations.
- [ ] Correlation IDs and safe diagnostics cross the boundary.
- [ ] Critical journeys run against production-like infrastructure.
