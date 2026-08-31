# Task 5 Final Review Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the remaining Task 5 review findings without changing canonical reload, RLS, retry/DLQ, Redis deferral, or deferred handler behavior.

**Architecture:** Keep `ai_job_state` as a single core-owned durable table applied once by the core Liquibase runner. Keep tenant iteration and durable claims in the existing reconciliation path, but add an explicit atomic claimed-to-retryable transition when bounded executor admission rejects work. Wire job JDBC and transactions to named core beans, and expose queue lag/claim duration through the existing injected metrics boundary.

**Tech Stack:** Java 25, Spring Boot, Spring JDBC, Liquibase formatted SQL, PostgreSQL/Testcontainers, JUnit 5, AssertJ/Mockito, Micrometer, Gradle, Spotless.

## Global Constraints

- Preserve canonical durable request reload, PostgreSQL RLS, retry/DLQ transitions, Redis/live-event deferral, and intentionally deferred concrete handlers.
- Follow strict Red-Green-Refactor TDD for production behavior changes.
- Do not stage or modify unrelated existing worktree changes.
- Use explicit core datasource qualifiers and focused architecture/integration tests.

## Task List

### Task 1: Move durable job schema to the core migration path

**Acceptance criteria:** formatted SQL metadata is valid; the core changelog includes exactly one core-owned job changeset; the studio changelog does not include or execute it; the RLS policy remains present and resolves the tenant function from the core path.

**Verification:** focused database contract test, assistant integration compilation/test, and Spotless.

### Task 2: Make scheduling obey the central property gate

**Acceptance criteria:** AI job configuration does not bypass `spring.task.scheduling.enabled`; disabled scheduling has no scheduled-task post processor even when AI job configuration is loaded; enabled scheduling still exposes it.

**Verification:** focused application scheduling test and assistant compilation.

### Task 3: Release rejected claims immediately and preserve tenant alternation

**Acceptance criteria:** rejected claimed submissions invoke one atomic durable defer transition with a positive next-availability delay; the row is retryable immediately and retains tenant scope; reconciliation visits active tenants in deterministic alternating order and rejection of one tenant does not leave its row claimed.

**Verification:** focused unit tests plus live PostgreSQL rejection/tenant-alternation integration coverage.

### Task 4: Add queue-lag and claim-duration telemetry

**Acceptance criteria:** every claim operation records claim duration; every claimed row records durable queue lag; Micrometer uses bounded, tenant-free timer names; existing lifecycle metrics remain intact.

**Verification:** focused Micrometer test and live claim integration assertions.

### Task 5: Qualify core JDBC wiring and verify DI

**Acceptance criteria:** `coreJdbcTemplate` and `coreDataSource` are explicitly named; the store cannot accidentally receive bootstrap/tenant JDBC; a context containing competing datasource/template beans starts and resolves the store to the core template.

**Verification:** focused context/DI test, compilation, and Spotless.

### Final checkpoint

- Run all focused Java 25 unit, architecture, database, and PostgreSQL/Testcontainers tests.
- Run assistant/application/database Spotless checks and compile tasks.
- Update `.superpowers/sdd/task-5-report.md` with evidence and remaining unrelated blockers only.
- Commit logical slices, push `feat/ai-platform-foundation`, and verify the remote tip.
