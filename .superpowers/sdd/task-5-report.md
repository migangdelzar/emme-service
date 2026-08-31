## 2026-08-31 — Task 5: Durable AI jobs and backpressure remediation

### Task: Tenant Activation Listener — final provisioning step

- [x] Step 1: Create `TenantActivated` event
  - **Actions Applied**
    - Created `modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantActivated.java` — record with `@Externalized("emme.tenancy.tenant-activated::#{#this.tenantId()}")`
  - **Verification**
    - Compiles cleanly

- [x] Step 2: Create `TenantActivationListener`
  - **Actions Applied**
    - Created `modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/messaging/consumer/TenantActivationListener.java` — consumes `TenantRealmReady`, marks tenant active, publishes `TenantActivated`
    - Used `@Transactional(propagation = Propagation.REQUIRES_NEW)` to avoid Spring Modulith conflict with `@TransactionalEventListener` (same pattern as `TenantSchemaProvisioningListener`)
  - **Verification**
    - `./gradlew :modules:tenancy:compileJava` — PASS
    - `./gradlew :modules:identity:compileJava` — PASS

- [x] Step 3: Add `findSchemaName()` to repository
  - **Actions Applied**
    - Added `String findSchemaName(UUID tenantId)` to `TenantProvisioningRepository.java`
    - Implemented in `TenantProvisioningPersistenceAdapter.java` — queries `emme_core.tenant_registry` by `tenant_id`
  - **Verification**
    - Compiles cleanly; adapter method matches existing JDBC patterns

- [x] Step 4: Resolve circular dependency (identity ↔ tenancy)
  - **Actions Applied**
    - Moved `TenantRealmReady` from `com.emme.identity.api.event` to `com.emme.tenancy.api.event` (identity depends on tenancy, so this respects the dependency direction)
    - Updated imports in `TenantRealmProvisioningListener.java`, `TenantRealmProvisioningListenerTest.java`, and `TenantActivationListener.java`
    - Applied spotless formatting to identity and tenancy modules
  - **Verification**
    - `./gradlew :modules:tenancy:compileJava :modules:identity:compileJava` — PASS (no circular dependency)
    - `./gradlew :modules:identity:test --tests "*TenantRealmProvisioningListenerTest"` — PASS
    - `./gradlew :modules:identity:test --tests "*IdentityPackageConventionTest"` — PASS

- [x] Step 5: Write unit test
  - **Actions Applied**
    - Created `TenantActivationListenerTest.java` — validates `markActive` is called and `TenantActivated` is published with correct `keycloakRealm`
  - **Verification**
    - `./gradlew :modules:tenancy:test --tests "*TenantActivationListenerTest"` — PASS

- [x] Step 6: Commit
  - **Actions Applied**
    - `git commit -m "feat: TenantActivationListener — final provisioning step, externalizes TenantActivated to Kafka"`
    - Commit: `a2d4ecb`
  - **Verification**
    - 18 files changed, 5 new files created

### Summary — Current Status

The prior report content was unrelated tenant provisioning and has been retained
below as historical material; it must not be used as evidence for AI jobs.

## 2026-08-31 — Durable AI jobs and backpressure slice

Added the smallest job boundary over existing Spring Modulith JDBC publication and PostgreSQL; no new queue dependency or Kafka admission-control path was introduced. The slice adds job contracts, idempotent claim/complete/fail worker orchestration, bounded platform-thread executor configuration, and durable tenant-scoped job state migration.

Tests:

- Red: `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*AiJobWorkerServiceTest'` failed at expected missing-contract compilation.
- Green: `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*AiJobWorkerServiceTest' --tests '*AiJobExecutorConfigurationTest' :database:test --tests '*AiJobMigrationContractTest'` — `BUILD SUCCESSFUL`.

Implemented: durable-before-event enqueue, tenant-scoped atomic JDBC claim/complete/failure transitions, constraints/RLS/indexes, Spring event listener and bounded executor wiring. Retry state uses durable `RETRYING`/`DEAD_LETTER` transitions and exponential availability timestamps. Redis remains an operational projection boundary already supplied by `AiLiveEventPublisher`; no second queue was introduced.

Genuinely future work: production job handlers per job type, reconciliation for events missed before restart, configurable retry ceiling in the SQL transition (currently aligned to the default three attempts), and integration tests requiring the project PostgreSQL/Testcontainers profile. The worker also requires each concrete handler to call `BoundedModelExecutionScheduler`; the generic boundary cannot safely invent model capability for all job types.

### Next Up

Kafka event contract tests (step 6 — validate `TenantActivated` event serialization/message structure)

## 2026-08-31 — Final Task 5 remediation

Remediated the reviewer findings on `feat/ai-platform-foundation`:

- `JdbcAiJobStatusStore.findAvailable(limit)` now recovers claims older than five minutes and selects due `QUEUED`/`RETRYING` rows with `FOR UPDATE SKIP LOCKED`, deterministic ordering, and an explicit `current_tenant_id()` predicate. Durable context columns are persisted so reconciliation can reconstruct `AiExecutionContext`.
- Executor rejection now leaves the durable row for reconciliation; it never invokes the worker inline.
- `AiJobProperties.maxAttempts` is wired into both the JDBC store and worker. Retry availability uses PostgreSQL `power(2, attempts - 1)` progression and exhausted attempts become `DEAD_LETTER`.
- Added regression coverage for rejection behavior and strengthened migration contract assertions for schema, constraints, indexes, RLS, and PostgreSQL-only statements.

The migration tests are static contract tests only. They do not execute the SQL against live PostgreSQL because this repository’s existing database test infrastructure does not provision a PostgreSQL/Testcontainers runtime in this task. Live validation remains required before production rollout. Production job handlers remain intentionally disabled/deferred; the durable scheduling, claiming, retry, and reconciliation boundary is implemented, but no concrete job-type business handler is claimed complete.

## 2026-08-31 — Final review remediation: durable claiming and tenant-safe reconciliation

The final-review gaps are remediated on `feat/ai-platform-foundation`:

- Reconciliation now calls `AiJobStatusStore.claimAvailable(limit)`, whose JDBC implementation runs one transaction containing stale-claim recovery and a PostgreSQL `WITH candidates ... FOR UPDATE SKIP LOCKED` followed by `UPDATE ... RETURNING`. The durable status transition to `CLAIMED` occurs before the rows are returned; there is no select-only reconciliation lock.
- The scheduled poller obtains active tenant IDs from the authoritative `TenantRepository`. For each registry tenant it binds a synthetic backend `AiExecutionContext` and `TenantContextHolder`, and the JDBC transaction establishes `app.current_tenant_id` with `set_config(..., true)`. Every recovery/claim/update predicate also carries the explicit tenant ID and `current_tenant_id()` check. Frontend, event, and LLM tenant IDs are not used to enumerate scheduled work.
- Jobs returned by reconciliation use a dedicated already-claimed worker path, so dispatch does not attempt a second claim. Rejected executor submissions remain durable and retryable.
- New unit coverage verifies authoritative tenant iteration and context cleanup. A direct Testcontainers PostgreSQL integration test applies migration `028-ai-job-state.sql`, runs with a non-superuser runtime role and forced RLS, and verifies both tenant isolation and that two concurrent reconciliation claims produce exactly one durable claim.
- Enqueue availability uses PostgreSQL’s `CURRENT_TIMESTAMP` default so readiness comparisons use one database clock rather than application and database clocks.

Verification:

- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*AiJobReconciliationPollerTest' --tests '*AiJobWorkerServiceTest' --tests '*AiJobListenerTest'` — PASS.
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:integrationTest --tests '*AiJobReconciliationClaimIntegrationTest'` — PASS (2 tests, live PostgreSQL/Testcontainers).
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:spotlessJavaCheck` — PASS after formatting.
- The full `:modules:assistant:test` task still reports the pre-existing `AiCapabilityConventionTest` failure for the unrelated `adapter/out/storage` package lacking `package-info.java`; no unrelated file was changed.

The concrete AI job handlers remain intentionally disabled/deferred, and Redis remains an optional live-event/projection boundary. This remediation does not claim handler implementation, Redis queue semantics, or end-to-end application bootstrap coverage beyond the focused live PostgreSQL contract.

## 2026-08-31 — Final review closure: canonical worker payloads, retry evidence, and metrics

- `AiJobStatusStore.claimAndLoad` atomically claims an event-addressed row and returns the canonical durable payload and execution context. `AiJobWorkerService` executes only that returned request; the reconciliation path reloads the claimed row with `loadClaimed` before execution. A regression test proves altered event payloads and context do not reach the handler or completion transition.
- `028-ai-job-state.sql` now enables and forces row-level security. The Testcontainers suite relies on the migration’s `FORCE ROW LEVEL SECURITY` statement and verifies the catalog flag while running claims as a non-superuser role.
- `AiJobReconciliationClaimIntegrationTest` now executes the PostgreSQL retry lifecycle: first failure schedules a one-second retry, the second schedules a two-second retry, and the third failure transitions the row to `DEAD_LETTER` with its error code.
- Added `package-info.java` for the new `com.emme.ai.contracts.job`, `com.emme.assistant.ai.domain.job`, and `com.emme.assistant.ai.adapter.in.messaging` production packages. The existing unrelated `com.emme.assistant.ai.adapter.out.storage` package still lacks metadata; it remains intentionally unchanged and is the known failing assertion in `AiCapabilityConventionTest`.
- Added a small injected `AiJobMetrics` boundary with Micrometer and no-op implementations. It records executor queue depth, claim outcomes, failures, retries, dead-letter transitions, and tenant scheduling selections with bounded labels and no tenant-ID metric cardinality. Redis and concrete job handlers remain explicitly deferred.

Verification for this closure:

- Worker, listener, poller, Micrometer, and migration contract tests pass under Java 25.
- `AiJobReconciliationClaimIntegrationTest` passes against PostgreSQL 16/Testcontainers, including tenant isolation, concurrent claim prevention, forced RLS, retry timing, and dead-letter progression.
- Scoped Spotless checks pass for `modules:assistant`, `libraries:ai-contracts`, and `database`.
- The full assistant unit test task remains blocked by the pre-existing `adapter/out/storage/package-info.java` convention violation documented above and a separate missing `TenantImageReader` application-context bean.

The final full `:modules:assistant:test` run also reproduces the branch's unrelated application-context failure: 15 web/module tests cannot create `CatalogDesignImageReader` because no `TenantImageReader` bean is available. That dependency/application wiring is outside Task 5 and was not changed; the scoped job test suites remain green.

## 2026-08-31 — Final review remediation: schema ownership and constructor binding

- `JdbcAiJobStatusStore` now uses unqualified `ai_job_state` statements. Its Spring JDBC dependency continues to resolve through the primary core datasource, whose established connection search path owns the core schema; no architecture-test rule was weakened.
- `AiJobReconciliationClaimIntegrationTest` now applies that same search-path boundary on every admin and runtime connection. It retains live PostgreSQL coverage for concurrent claiming, tenant isolation, forced RLS, retry progression, and dead-lettering without embedding a core-schema reference in an assistant Java source file.
- `AiJobProperties` now marks its full record constructor with `@ConstructorBinding` and declares `@DefaultValue`s for all four limits. The convenience three-argument constructor and safe defaults remain available to existing callers.
- Added `AiJobPropertiesTest` using `ApplicationContextRunner` to prove configured values bind through the canonical constructor and absent values preserve the safe defaults.

Verification for this closure:

- `mise exec java@25.0.2 -- ./gradlew :applications:emme-platform:test --tests 'com.emme.SchemaOwnershipTest' --no-daemon` — PASS.
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*AiJobPropertiesTest' --no-daemon` — PASS (2 tests).
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*AiJob*' :database:test --tests '*AiJobMigrationContractTest' --no-daemon` — PASS.
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:integrationTest --tests '*AiJobReconciliationClaimIntegrationTest' --no-daemon` — PASS (3 tests, PostgreSQL 16/Testcontainers).
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:compileJava :modules:assistant:compileIntegrationTestJava :modules:assistant:spotlessJavaCheck --no-daemon` — PASS.

The full assistant test suite was not used as the acceptance gate because the worktree contains unrelated in-progress module changes; this closure is limited to the focused architecture, configuration, job, migration, integration, compilation, and formatting checks above.
