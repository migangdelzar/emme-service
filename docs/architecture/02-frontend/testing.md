# Frontend Testing

## Test levels

| Level | Purpose | Boundary |
|---|---|---|
| Unit | Pure functions, reducers, validators, formatters | No browser or network |
| Component | Rendering, interaction, accessibility semantics | DOM with mocked API boundary |
| Integration | Feature flow with routing and data client | Browser-like test environment |
| E2E | Critical user journey across real frontend/backend | Running application and infrastructure |

## Rules

- Test user-visible behavior rather than implementation details.
- Mock the API boundary, not internal React modules.
- Cover loading, empty, validation, server error, authorization, and success states.
- Use stable semantic queries and explicit test IDs only when semantics are insufficient.
- Keep tests deterministic; control time, randomness, and network responses.
- Reserve E2E for critical journeys and contract confidence, not every component state.

## Minimum feature coverage

```text
feature test suite
├── renders initial/loading state
├── shows empty state
├── submits valid input
├── displays validation failure
├── displays server failure
└── completes the primary user outcome
```

## Quality gates

- Unit and component tests run on every change.
- Integration tests exercise routing, providers, API clients, and error states.
- Contract tests detect API schema drift before E2E.
- Critical E2E flows run against a production-like backend and deterministic identities/tenants.
- Accessibility checks run for shared components and critical pages.
- Visual regression is used only for stable, high-value surfaces; avoid brittle snapshots.
- Test data is isolated, classified, and deleted after execution.
- Flaky tests are quarantined with an owner and expiry date, never silently retried forever.

## Frontend testing checklist

- [ ] Tests assert user-visible behavior rather than component internals.
- [ ] API responses include validation, conflict, unauthorized, unavailable, and success cases.
- [ ] Session expiry, tenant changes, and permission changes are covered.
- [ ] Keyboard/focus and accessible-name behavior is covered for critical interactions.
- [ ] Critical E2E flows produce artifacts, traces, and failure diagnostics.
- [ ] Tests are deterministic and safe to run in parallel.
