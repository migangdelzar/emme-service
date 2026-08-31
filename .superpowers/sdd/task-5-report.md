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
