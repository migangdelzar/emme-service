# Architecture Decision Records

ADRs record consequential service decisions. They explain why a choice was made,
what alternatives were rejected, and how the decision is verified.

## Required for

- module or repository boundaries;
- authentication, authorization, data ownership, consistency, and event delivery;
- durable technology or infrastructure choices;
- release, migration, rollback, or security trade-offs.

## Lifecycle

```text
Proposed → Accepted → Superseded
                    ↘ Deprecated
```

Do not delete historical ADRs. A changed decision creates a new ADR that links
to the decision it supersedes.

## Existing records

- [ADR-0001: Build-logic convention plugins](0001-build-logic-convention-plugins.md)
- [ADR-0002: Deployment strategy pattern](0002-deployment-strategy-pattern.md)
- [ADR-0003: Trusted proxy boundary for Identity rate limiting](0003-identity-login-rate-limit-client-ip.md)
- [ADR-0004: Shared and Audit ownership](0004-shared-and-audit-ownership.md)

## Index

| ADR | Title | Status | Date |
|---|---|---|---|
| [0001](0001-build-logic-convention-plugins.md) | Precompiled convention plugins for build logic | Accepted | 2026-07-10 |
| [0002](0002-deployment-strategy-pattern.md) | Strategy pattern for deployment | Accepted | 2026-07-10 |
| [0003](0003-identity-login-rate-limit-client-ip.md) | Trust forwarded client IPs only from configured proxies | Accepted | 2026-08-01 |
| [0004](0004-shared-and-audit-ownership.md) | Shared technical capability and reserved Audit ownership | Accepted | 2026-08-01 |

Create a new ADR when a decision changes. Do not rewrite an accepted ADR to
hide its historical rationale.
