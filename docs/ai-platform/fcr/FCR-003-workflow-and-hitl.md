# FCR-003: LangGraph4j Workflow and HITL

## Change requested

Add a durable LangGraph4j workflow for conversation, quote extraction,
deterministic calculation, approval, resume, and final response.

## Affected areas

```text
assistant workflow
LangGraph4j checkpoint adapter
PostgreSQL workflow migrations
quote review endpoints and persistence
notifications and live workflow events
```

## Acceptance

- Workflow states and checkpoints survive restart.
- Staff review is a persisted state, not an email fallback.
- Review decisions use optimistic locking and audit actor identity.
- Resume re-establishes tenant and principal context.
- LangGraph4j and Spring AI do not run competing tool loops.
