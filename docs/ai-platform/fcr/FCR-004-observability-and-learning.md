# FCR-004: Observability, Evaluation, and Learning

## Change requested

Persist AI traces, instrument model/vector/tool/workflow behavior, and add an
asynchronous candidate-learning and evaluation pipeline.

## Affected areas

```text
observability-support
application metrics and tracing
PostgreSQL AI trace/candidate migrations
OpenTelemetry JVM agent configuration
evaluation worker and Ragas scaffold
dashboards, alerts, and rollback runbook
```

## Acceptance

- Model, vector, tool, workflow, queue, HITL, tenant, and cost metrics exist.
- PII is redacted before evaluation and learning.
- Ragas is never called synchronously during a customer request.
- Candidate promotion is versioned, canaried, monitored, and reversible.
- Safety regressions block promotion.
