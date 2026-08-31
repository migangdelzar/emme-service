## 2026-08-05 — Task 5: Tenant Activation Listener

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

Task 5 complete. The provisioning chain now has its final step:
- `TenantCreated` → `TenantSchemaReady` → `TenantRealmReady` → `TenantActivated` (@Externalized to Kafka)

## 2026-08-31 — Durable AI jobs and backpressure slice

Added the smallest job boundary over existing Spring Modulith JDBC publication and PostgreSQL; no new queue dependency or Kafka admission-control path was introduced. The slice adds job contracts, idempotent claim/complete/fail worker orchestration, bounded platform-thread executor configuration, and durable tenant-scoped job state migration.

Tests:

- Red: `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*AiJobWorkerServiceTest'` failed at expected missing-contract compilation.
- Green: `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*AiJobWorkerServiceTest' --tests '*AiJobExecutorConfigurationTest' :database:test --tests '*AiJobMigrationContractTest'` — `BUILD SUCCESSFUL`.

Limitations: no JDBC status-store adapter or Redis Streams consumer group is wired yet; delayed broker redelivery/backoff remains a follow-up. Model handlers must continue to use the existing `BoundedModelExecutionScheduler`.

### Next Up

Kafka event contract tests (step 6 — validate `TenantActivated` event serialization/message structure)
