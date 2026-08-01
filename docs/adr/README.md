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
