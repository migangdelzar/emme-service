# Operational Runbook: Emme AI Platform

| Field | Value |
|---|---|
| Status | Draft |
| Scope | JVM service, local worker, PostgreSQL, Redis, model providers |

## 1. Startup checks

1. Run `mise run toolchain:jvm`; it verifies the selected Java executable and
   reports the Gradle launcher and daemon JVM versions.
2. Use `mise run toolchain:native` only on a host with the approved GraalVM
   Native Image toolchain.
3. Verify preview flags for JVM processes using `StructuredTaskScope`.
4. Verify PostgreSQL and pgvector migrations.
5. Verify Redis health and keyspace policy.
6. Verify configured local providers and cloud opt-in policy.
7. Verify OpenTelemetry agent on JVM deployments.
8. Verify active embedding model/version matches vector indexes.
9. Verify workflow checkpoint schema and outbox publication.

## 2. Dashboards

Required panels:

- Requests by tenant and channel.
- Semantic cache hit/miss and false-hit correction.
- Intent score and margin distributions.
- Abstention and LLM fallback rate.
- Tool candidate selection and policy rejections.
- Model/provider latency, timeout, tokens, and cost.
- Workflow state counts and checkpoint resume failures.
- HITL queue depth and waiting time.
- Candidate index and promotion status.
- Cross-tenant access violations.

## 3. Alerts

- Any confirmed cross-tenant retrieval or unauthorized tool execution.
- Deterministic quote mismatch.
- Duplicate booking detection.
- Provider timeout or fallback spike.
- Queue lag or dead-letter growth.
- Checkpoint persistence failure.
- Embedding model/index mismatch.
- Redis lock error rate.
- Tenant AI budget exhaustion.

## 4. Incident procedures

### Model or provider outage

1. Confirm provider health and timeout metrics.
2. Disable the affected provider through configuration.
3. Allow local or approved fallback only for permitted tenants.
4. Keep deterministic routes and cached safe answers available.
5. Do not retry mutations automatically.

### Vector-store outage

1. Stop semantic shortcuts.
2. Continue deterministic commands and safe structured fallback.
3. Disable candidate ingestion if the index cannot be validated.
4. Restore or fail over PostgreSQL according to the database runbook.

### Suspected poisoned candidate/index

1. Move the candidate or index version to quarantine.
2. Switch the active pointer to the previous version.
3. Review provenance and affected tenants.
4. Re-run regression and safety evaluation.
5. Record the incident and retain the failed artifact.

### Workflow stuck in HITL

1. Inspect workflow and review task records in PostgreSQL.
2. Confirm reviewer authorization and optimistic version.
3. Resume through the approval endpoint; never edit checkpoints manually in
   production without an approved recovery procedure.

## 5. Rollback

Rollback must be possible for:

- Application release.
- Provider configuration.
- Prompt version.
- Embedding/index version.
- Threshold configuration.
- Candidate promotion.

Redis may be flushed for temporary acceleration only after confirming that all
durable records exist in PostgreSQL.
