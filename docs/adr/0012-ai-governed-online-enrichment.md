# ADR-0012: Governed Online Enrichment

## Status

Proposed

## Date

2026-08-27

## Context

Emme should improve from successful interactions, but a single misroute,
prompt-injected document, or privacy mistake must not modify production
behavior.

## Decision

- Persist traces and strong feedback online.
- Allow safe scoped cache enrichment with TTL and dependency-version checks.
- Store intent/tool learning as candidate records first.
- Redact PII, screen content, deduplicate, embed asynchronously, and evaluate.
- Promote through shadow, canary, versioned active-pointer, and rollback steps.
- Never create tools, permissions, prices, or appointments from learning data.

## Consequences

- The system adapts quickly without uncontrolled production mutation.
- Candidate/index lifecycle and evaluation infrastructure are required.
- Cache policies must distinguish global, tenant, user, and transactional data.
