# Task 6 — Semantic routing, tool selection, and caching

**Date:** 2026-08-31
**Branch:** `feat/ai-platform-foundation`
**Scope:** semantic AI capability hardening requested by the implementation brief

## Scope note

The checked-in `.superpowers/sdd/task-6-brief.md` describes a different live-workflow/SSE
slice. This implementation follows the explicit requested scope: semantic routing/classification,
semantic tool selection, and semantic caching. Live workflow SSE/WhatsApp graph integration is
not included.

## Implemented

- Preserved the existing tenant-scoped pgvector semantic references, deterministic threshold and
  top-1/top-2 margin policy, and explicit embedding-provider-only fallback to the legacy LLM.
- Centralized the default embedding model name, model version, and vector dimension across the
  assistant, AI-platform, Redis, and semantic contract boundaries; Redis composition now fails
  closed when its vector-store or hot-store settings disagree with the active contract.
- Added fail-closed validation before durable or Redis cache hit accounting, plus safe empty-result
  cache fallback when embedding, durable DB, or Redis operations fail. Durable invalidation errors
  remain visible; Redis projection errors do not erase a successful durable invalidation.
- Added injected semantic metrics for routing, tool selection, cache lookup, and cache write
  outcomes with bounded labels and no tenant/principal cardinality, including top-1/top-2 scores,
  margins, latency, failures, fallbacks, and invalidation dependency/scope outcomes.
- Added durable application-event publication/listening for tenant policy, service, price, and
  quote-template dependency changes; invalidation coordinates JDBC authority and the Redis hot
  projection with principal/tenant-safe key deletion.
- Rejected unsafe semantic-cache response payloads before embedding or persistence (payment-card,
  email, phone-like, and bearer-token patterns); existing transactional/personalized bypasses
  remain in force.
- Enforced configured embedding model identity, in addition to dimension checks, for JDBC
  semantic reference search and cache operations.
- Kept PostgreSQL/pgvector authoritative and Redis as the existing optional hot projection; no
  competing store or provider was added.

## Verification

| Check | Result |
|---|---|
| Focused AI-platform and assistant semantic tests (embedding contract/defaults, routing, cache policy/safety/fallback, JDBC/Redis adapters, invalidation, publisher wiring, metrics, Chat/RAG fallback) | **PASS — 52 selected tests** |
| `:modules:assistant:integrationTest --tests '*RedisSemanticIntegrationTest'` | **PASS** |
| `:database:test --tests '*AiSemanticSearchMigrationContractTest'` | **PASS** |
| `:modules:assistant:spotlessCheck :modules:ai-platform:spotlessCheck` | **PASS** |
| `git diff --check` | **PASS** |
| Full `:modules:assistant:test` run | **LIMITED — 357 completed, 16 failed** |
| `:applications:emme-platform:test` | **LIMITED — 62 completed, 8 known unrelated architecture-baseline failures** |

The 16 full-suite failures are the known pre-existing failures in unrelated package metadata and
tenancy/identity/JPA application-context setup. The unrelated dirty files were preserved and not
staged.

## Limitations

- Redis hot entries are removed through the indexed principal/tenant projection when the Redis
  client is available. Durable PostgreSQL invalidation plus durable hit confirmation remains the
  correctness authority when Redis is unavailable.
- Unsafe-payload detection is conservative pattern filtering, not a complete DLP system; callers
  requiring stronger guarantees need a dedicated policy service behind the existing port.
- The checked-in Task 6 brief and the explicitly requested semantic scope do not match; the live
  workflow/SSE requirements remain outstanding.

## Files changed by this scoped slice

- Semantic application ports and services under `modules/assistant/src/main/java/com/emme/assistant/ai/application`.
- JDBC semantic adapters under `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence`.
- Micrometer adapter and Spring configuration under `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/observability` and `.../configuration`.
- Focused assistant tests under `modules/assistant/src/test/java/com/emme/assistant/ai`.
- Shared semantic contracts under `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/semantic`.
- Task 6 implementation/tests under `modules/assistant/src/main/java/com/emme/assistant/ai` and
  `modules/assistant/src/test/java/com/emme/assistant/ai`.
- Canonical AI-platform embedding defaults under
  `modules/ai-platform/src/main/java/com/emme/ai/platform/configuration`.
