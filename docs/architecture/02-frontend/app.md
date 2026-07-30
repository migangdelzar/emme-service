# Frontend Application

## Purpose

A frontend application owns the browser composition root: routing, providers, global layout, authentication/session bootstrapping, API clients, and feature composition.

## Reference layout

```text
apps/<application>/
├── src/
│   ├── app/                 # composition root, routes, providers
│   ├── features/            # user-facing capabilities
│   ├── modules/              # optional larger capability boundaries
│   ├── shared/               # deliberately reusable UI/platform code
│   ├── lib/                  # thin technical adapters
│   └── main.tsx
├── public/
├── vite.config.ts
└── package.json
```

```mermaid
flowchart TB
    MAIN[main.tsx] --> CONFIG[Runtime config]
    MAIN --> SESSION[Session/auth provider]
    MAIN --> DATA[Query/data provider]
    MAIN --> ROUTER[Router]
    ROUTER --> LAYOUT[Application layout]
    LAYOUT --> FEATURE[Feature route]
    FEATURE --> API[Typed API client]
```

## Rules

- The app shell owns routing and global providers.
- Features own user flows and feature-specific state.
- Shared code earns its place through reuse and stable semantics.
- Browser code calls typed clients, not ad hoc `fetch` calls scattered through components.
- Environment configuration is validated at startup and contains no secrets that must remain server-side.
- The frontend must not rely on internal backend packages; it consumes HTTP or generated contracts.

## App-shell guardrails

### Composition root

The app shell owns only cross-feature concerns:

```text
main.tsx
  → runtime configuration
  → error boundary
  → authentication/session provider
  → query/data provider
  → router
  → application layout
  → feature route
```

Feature business behavior remains in the feature/module boundary. Do not place API calls, tenant decisions, or feature-specific state in the global shell.

### Security and runtime configuration

- Treat all browser code and `VITE_*` variables as public.
- Keep tokens and sensitive state in the repository-approved secure session mechanism; never persist secrets in arbitrary local storage.
- Enforce authorization on the backend; frontend guards improve UX but are not security controls.
- Validate runtime configuration before rendering protected routes.
- Define safe behavior for expired sessions, tenant changes, unauthorized routes, and backend unavailability.

### Application checklist

- [ ] Routes have authenticated/anonymous and tenant-scope behavior defined.
- [ ] Global error, loading, offline, and session-expiry states are handled.
- [ ] API clients are typed and centralized.
- [ ] Browser-exposed configuration contains no secrets.
- [ ] Accessibility and performance budgets are part of CI.
- [ ] Production source maps, logging, and telemetry follow the security policy.
