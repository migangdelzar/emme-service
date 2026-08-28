# Evaluation Specification: Emme AI Platform

| Field | Value |
|---|---|
| Status | Draft |
| Trace source | PostgreSQL, anonymized before evaluation |
| Execution | CI or asynchronous worker |

## 1. Dataset groups

```text
intent-routing
  English and Spanish salon messages
  multi-intent requests
  ambiguous and abstention examples

tool-selection
  read-only, confirmation, and mutation scenarios
  role and tenant capability variants

design-extraction
  shape, length, color, finish, effects, decorations, repairs, removals

business-safety
  wrong tenant, wrong price, duplicate booking, unauthorized tool

retrieval
  tenant FAQs, aftercare, service descriptions, and policy documents

workflow
  HITL pause/resume, retry, timeout, and restart recovery
```

## 2. Metrics

### Router

- Intent accuracy.
- Top-k recall.
- Top-1/top-2 margin distribution.
- Abstention precision and recall.
- False-route rate.
- Tool-selection accuracy.

### Extraction

- Slot accuracy.
- Date/time accuracy.
- Design attribute accuracy.
- Invalid-schema rate.
- Human-correction rate.

### Retrieval/cache

- Hit@k.
- Context precision and recall.
- Cache hit rate.
- False-cache-hit rate.
- Stale-cache rate.
- Cache correction/invalidation rate.

### Business safety

- Wrong-tenant retrieval rate: must be zero.
- Wrong-price rate: must be zero in deterministic quote tests.
- Duplicate-booking rate: must be zero in idempotency tests.
- Unauthorized-tool rate: must be zero.
- Unnecessary-HITL rate.

### Operations

- End-to-end and component latency.
- Model tokens and cost.
- Retry and timeout rate.
- Provider fallback frequency.
- Checkpoint resume success.

## 3. Promotion gate

An index, threshold set, prompt version, or provider configuration cannot be
promoted when a required safety metric regresses or when the evaluation dataset
is incomplete.

Promotion sequence:

```text
candidate
→ redaction validation
→ deterministic safety tests
→ regression evaluation
→ shadow comparison
→ canary tenant/index
→ monitored rollout
→ promote or rollback
```

Ragas may measure retrieval and generation quality. It cannot replace
deterministic tests for pricing, authorization, tenant isolation, or booking
correctness.
