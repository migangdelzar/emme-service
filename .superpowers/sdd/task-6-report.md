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
- Added injected semantic metrics for routing, tool selection, cache lookup, and cache write
  outcomes with bounded labels and no tenant/principal cardinality.
- Added authenticated-principal-scoped durable cache invalidation through the existing cache port
  and JDBC adapter.
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
| Focused assistant semantic tests (`SemanticChatCacheTest`, `JdbcSemanticAdapterTest`, `SemanticRoutingServiceTest`, `MicrometerSemanticMetricsTest`, `SpringAiSemanticConfigurationTest`, `SpringAiSemanticCacheConfigurationTest`) | **PASS — 29 tests** |
| `:modules:assistant:integrationTest --tests '*RedisSemanticIntegrationTest'` | **PASS** |
| `:database:test --tests '*AiSemanticSearchMigrationContractTest'` | **PASS** |
| `:modules:assistant:spotlessCheck` | **PASS** |
| `git diff --check` | **PASS** |
| Full `:modules:assistant:test` run | **LIMITED — 357 completed, 16 failed** |

The 16 full-suite failures are the known pre-existing failures in unrelated package metadata and
tenancy/identity/JPA application-context setup. The unrelated dirty files were preserved and not
staged.

## Limitations

- The invalidation contract currently exposes current authenticated tenant/principal and cache
  kind scope only; no new admin/event invalidation trigger was present in the existing assistant
  boundary, so none was invented.
- Redis hot entries are not synchronously deleted. Durable PostgreSQL invalidation plus durable
  hit confirmation prevents an invalidated response from being served; Redis still expires by its
  configured TTL. This is a deliberate use of the existing two-tier design.
- Unsafe-payload detection is conservative pattern filtering, not a complete DLP system; callers
  requiring stronger guarantees need a dedicated policy service behind the existing port.
- The checked-in Task 6 brief and the explicitly requested semantic scope do not match; the live
  workflow/SSE requirements remain outstanding.

## Files changed by this scoped slice

- Semantic application ports and services under `modules/assistant/src/main/java/com/emme/assistant/ai/application`.
- JDBC semantic adapters under `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence`.
- Micrometer adapter and Spring configuration under `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/observability` and `.../configuration`.
- Focused assistant tests under `modules/assistant/src/test/java/com/emme/assistant/ai`.
- `tasks/plan.md` and the Task 6 section in `tasks/todo.md`.
