# Implementation Plan: Task 6 Semantic AI Capability Hardening

## Overview

Close the semantic routing, authorized tool selection, and semantic response-cache
boundaries using the existing pgvector and optional Redis projections. The slice
must remain backend-context scoped, model/dimension compatible, threshold-gated,
safe on fallback, expiring, invalidatable, and observable.

## Task List

### Phase 1: Contract and safety gates

- [x] Add durable semantic-cache invalidation to the existing cache port and JDBC adapter.
- [x] Reject unsafe response payloads before semantic-cache persistence.
- [x] Enforce configured embedding model identity in JDBC semantic reference/cache adapters.

### Phase 2: Observability and verification

- [x] Add bounded Micrometer counters for semantic routing, tool selection, and cache outcomes.
- [x] Add focused regression tests for all new behavior and preserve existing tests.
- [x] Run focused tests, full assistant/database checks, formatting, and compile verification.
- [x] Update `.superpowers/sdd/task-6-report.md` with exact results and limitations.
- [x] Commit and push the scoped changes to `feat/ai-platform-foundation`.

## Verification result

Focused semantic tests: 29 passed. Redis semantic integration test: passed. Database semantic migration contract: passed. Assistant Spotless check: passed. The full assistant test run completed 357 tests with 16 pre-existing failures in unrelated package metadata and tenancy/identity/JPA context setup; those files were not modified by this task.

## Architecture Decisions

- PostgreSQL/pgvector remains authoritative; Redis remains an optional hot projection.
- No new store or provider is introduced.
- Semantic fallback remains explicit: embedding-provider unavailability may fall back to the
  existing model path; invalid vectors, authorization failures, and persistence errors fail closed.
- Cache invalidation is tenant/principal scoped from the bound `AiExecutionContext`; Redis entries
  need not be deleted synchronously because durable hit confirmation prevents stale responses.
