# Task 1 Report: Durable Conversation-Aware AI Request Boundary

## Status: DONE

## Scope

Completed the first AI platform slice in `emme-service`: a typed, tenant-bound
conversation request boundary around the existing `ChatUseCase` and durable
conversation application use cases. Existing callers remain compatible with
the two-argument `ChatRequest`, the legacy `AiController` constructor, and the
single-field `ChatResponse` constructor.

## TDD Evidence

### RED

After adding the compatibility and durable-identifier tests, ran:

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test \
  --tests '*ProcessConversationServiceTest' \
  --tests '*AiControllerConversationTest'
```

Result: **FAILED as expected during test compilation** because the new
backwards-compatibility test required the original four-argument
`AiController` constructor, which had not yet been restored.

### GREEN

Added the overloaded legacy constructor and the durable response identifiers,
then ran the same focused command.

Result: **BUILD SUCCESSFUL**; focused Task 1 tests passed.

### Final verification

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test :modules:assistant:spotlessCheck
```

Result: **BUILD SUCCESSFUL**; the complete assistant test suite and Spotless
checks passed.

## Implementation

- Added `ProcessConversationCommand`, `ProcessConversationResult`, and
  `ProcessConversationUseCase`.
- Added `ConversationMemoryPort` and its persistence adapter over the existing
  conversation application use cases.
- Added `ProcessConversationService`, which requires the current
  `AiExecutionContext`, validates conversation/idempotency identity, loads
  tenant-scoped history, appends the user event before chat orchestration, and
  appends the validated assistant response afterward.
- Declared the conversation orchestration service as
  `@Transactional(propagation = Propagation.NOT_SUPPORTED)` so long-running
  model work does not hold a database transaction while the existing
  persistence application use cases retain their own transaction boundaries.
- Added conversation-aware context creation using the backend tenant context,
  authenticated JWT identity, correlation ID, and idempotency key.
- Updated `/api/ai/chat` to accept `conversationId` and `Idempotency-Key`
  without accepting a frontend tenant ID.
- Durable responses now include `conversationId` and `workflowId`; legacy
  responses retain null identifiers through the compatibility constructor.
- Preserved the existing legacy chat path and existing model/provider logic.

## Changed files

- `modules/assistant/src/main/java/com/emme/assistant/ai/api/command/ProcessConversationCommand.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/api/result/ProcessConversationResult.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/api/usecase/ProcessConversationUseCase.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/ConversationMemoryPort.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ProcessConversationService.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/ConversationMemoryPersistenceAdapter.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/request/ChatRequest.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/response/ChatResponse.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/controller/AiController.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/security/AiWebExecutionContextFactory.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ProcessConversationServiceTest.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/in/web/controller/AiControllerConversationTest.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/in/web/security/AiWebExecutionContextFactoryTest.java`

## Commit

- `441540b9` — `feat(assistant): add durable conversation AI boundary`
- `edfb5067` — `fix(assistant): declare conversation orchestration transaction policy`

## Self-review

- Tenant identity is obtained only from `TenantContextHolder.requireCurrentTenantId()`.
- Conversation reads and writes delegate to existing tenant-keyed application
  queries/commands; the AI service does not access repositories directly.
- `AiExecutionContextScope.requireCurrent()` prevents execution without an
  authenticated AI context.
- Conversation and idempotency values supplied by the command must match the
  backend-created execution context.
- User persistence precedes orchestration and assistant persistence follows a
  non-blank response.
- The controller does not accept or forward a tenant ID from the request.
- No identity, subscriptions, or tenancy working-tree changes were staged.
- The repository-wide architecture hook now reports only the pre-existing
  transaction-policy failure in `identity/GetCurrentUserService`; the new Task
  1 service satisfies that check.

## Concerns / follow-ups

- Duplicate idempotency replay semantics are not implemented in this slice;
  the key is validated and propagated. Durable idempotency storage belongs to
  the planned workflow/job boundary in a later task.
- The legacy request path remains intentionally non-durable when no
  `conversationId` is supplied. New durable callers must provide both
  `conversationId` and `Idempotency-Key`.

---

## Review-fix addendum

### Status: DONE

### RED

Added replay, active-reservation, durable-adapter, and cleanup tests before
the implementation. The following focused test command failed as expected
during compilation because the conversation idempotency port/adapter and the
three-argument command did not yet exist:

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test \
  --tests '*ProcessConversationServiceTest' \
  --tests '*ConversationTurnIdempotencyAdapterTest'
```

Added a cleanup-preservation test afterward. It failed as expected because a
failed idempotency release masked the original model failure.

### GREEN

- Reused `AiToolIdempotencyStore` and its existing PostgreSQL lease/replay
  ledger through `ConversationTurnIdempotencyAdapter`; no Redis state or new
  idempotency table was introduced.
- The operation key is deterministic: `processConversation:{conversationId}:{idempotencyKey}`.
  The existing durable store additionally scopes it by the authenticated
  tenant and principal.
- A completed result is serialized as the existing durable `AiToolResult`
  payload and replayed before conversation loading, event writes, or model
  invocation.
- A reserved but incomplete turn returns `AI conversation turn is already in
  progress`; failed execution releases its reservation without masking the
  original error.
- Removed the unused `channel` field from `ProcessConversationCommand`; the
  durable HTTP contract remains coherent and legacy chat callers remain
  unchanged.
- Added PostgreSQL/Spring integration coverage proving tenant A cannot load,
  read, or append to tenant B's conversation through
  `ConversationMemoryPersistenceAdapter` and the existing conversation
  application use cases.

### Files

- `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/ConversationTurnIdempotencyPort.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/ConversationTurnIdempotencyAdapter.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ProcessConversationService.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/api/command/ProcessConversationCommand.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/controller/AiController.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ProcessConversationServiceTest.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/persistence/ConversationTurnIdempotencyAdapterTest.java`
- `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/ConversationMemoryTenantIsolationIntegrationTest.java`

### Verification

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test \
  --tests '*ProcessConversationServiceTest' \
  --tests '*ConversationTurnIdempotencyAdapterTest' \
  --tests '*AiControllerConversationTest'

mise exec java@25.0.2 -- ./gradlew :modules:assistant:test :modules:assistant:spotlessCheck

mise exec java@25.0.2 -- ./gradlew :modules:assistant:integrationTest
```

All commands completed successfully. The integration suite emitted existing
Testcontainers Docker-prune and shutdown connection warnings but reported
`BUILD SUCCESSFUL`.

### Commit

- `77aec2ad` — `fix(assistant): make conversation turns idempotent`

### Concerns

- A process crash after a durable assistant event has been appended but before
  the idempotency ledger is completed still requires reconciliation in the
  planned durable workflow/job boundary; this change prevents normal retry
  duplication after a completed turn and rejects concurrent retries.

---

## Re-review finalization-recovery addendum

### Status: DONE

### RED

Added the regression case in which `ConversationTurnIdempotencyPort.complete`
fails after the assistant event has committed. The first implementation had no
durable assistant-event marker, so a retry could re-enter chat and append a
second turn. The initial focused test and migration-contract run failed as
expected because the marker API and schema migration did not exist.

An additional cleanup test then failed as expected because a lookup failure
occurred before the service's reservation cleanup boundary.

### GREEN

- Added a nullable `conversation_event.idempotency_key` and a tenant-scoped,
  conversation-scoped partial unique index. This is durable PostgreSQL state;
  Redis is not involved.
- Persists the assistant message with the conversation turn idempotency key.
- Before loading context or invoking the model, `ProcessConversationService`
  reads the tenant-scoped event history for that marker. If found, it completes
  (or replays) the existing durable idempotency record and returns the stored
  response without appending events or calling chat.
- If completion fails after the assistant event commits, the reservation is
  deliberately retained. A retry sees the durable marker even when its lease
  cannot be re-reserved, reconciles completion, and returns the original
  result.
- Retains reservation cleanup for failures before the assistant response is
  durably persisted.
- Corrected the existing JSONB entity mapping and mapper so assistant text is
  stored as a JSON string and read back as the original domain payload; the
  real PostgreSQL integration test covers this path.

### Verification

```shell
mise exec java@25.0.2 -- ./gradlew -q --console=plain \
  :modules:assistant:test --tests '*ProcessConversationServiceTest' \
  :modules:assistant:integrationTest \
  --tests '*ConversationMemoryTenantIsolationIntegrationTest' \
  :database:test --tests '*ConversationEventIdempotencyMigrationContractTest' \
  :modules:assistant:spotlessCheck :database:spotlessCheck --rerun-tasks
```

Result: **BUILD SUCCESSFUL** (exit code 0). The focused service regression,
PostgreSQL tenant-isolation/idempotency integration test, migration contract,
and Spotless checks all passed with Java 25.

### Files

- `database/src/main/resources/db/emme-studio/releases/0.1.0/025-conversation-event-idempotency.sql`
- `database/src/test/java/com/emme/database/ConversationEventIdempotencyMigrationContractTest.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ProcessConversationService.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/ConversationMemoryPersistenceAdapter.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/out/persistence/entity/ConversationEventEntity.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/out/persistence/mapper/ConversationEventPersistenceMapper.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ProcessConversationServiceTest.java`
- `modules/assistant/src/integrationTest/java/com/emme/assistant/ai/ConversationMemoryTenantIsolationIntegrationTest.java`

### Commit

- `85943c9b` — `fix(assistant): reconcile persisted conversation turns`

### Concerns

- The migration must be deployed before this reconciliation path is enabled in
  an environment. The partial unique index remains tenant- and
  conversation-scoped and does not change RLS.

---

## Consolidated reviewer-fix wave

### Status: DONE

### RED

Added regression coverage before implementation for three invariants:

- a retry after model failure writes only one user event;
- a principal in the same tenant cannot recover another principal's assistant
  finalization response; and
- blank or NUL-corrupt recovered assistant payloads are rejected before any
  idempotency completion or replay.

The initial focused run failed as expected:

```shell
mise exec java@25.0.2 -- ./gradlew \
  :modules:assistant:test --tests '*ProcessConversationServiceTest' \
  :modules:assistant:integrationTest \
  --tests '*ConversationMemoryTenantIsolationIntegrationTest' \
  :database:test --tests '*ConversationEventIdempotencyMigrationContractTest'
```

It reported the absent `idempotency_principal_id` migration contract, two user
message appends after a model failure, and replay of a blank recovered answer.
The behavioral uniqueness test then failed at test compilation until the
idempotent user-marker API was added.

### GREEN

- `conversation_event` now stores nullable `idempotency_principal_id` beside
  its turn key. The partial unique index is tenant-, principal-, conversation-,
  event-type-, and key-scoped, so one durable user marker and one durable
  assistant marker can share a turn while duplicate markers of either event
  type are rejected by PostgreSQL.
- The persistence adapter derives marker ownership only from
  `AiExecutionContext.principalId()` and filters both user and assistant marker
  lookup by that principal. No frontend-provided principal is accepted.
- `ProcessConversationService` writes the user marker before model execution,
  releases the active idempotency lease after a pre-response failure, and skips
  that write on retry when the durable user marker already exists. The original
  model exception remains the outward failure.
- Normal and recovered assistant results share one validation function. It
  rejects blank values and embedded NUL control characters before completing or
  replaying the turn.
- The PostgreSQL integration test explicitly executes the packaged migration
  against the Testcontainers schema, proving its partial unique index rejects a
  duplicate marker. The assistant integration-test classpath now depends on the
  existing `:database` project only for that migration resource.

### Verification

```shell
mise exec java@25.0.2 -- ./gradlew -q \
  :modules:assistant:test \
  :modules:assistant:integrationTest \
  :database:test \
  :modules:assistant:spotlessCheck \
  :database:spotlessCheck \
  --rerun-tasks
```

Result: **BUILD SUCCESSFUL** (exit code 0) with Java 25. This reran the full
assistant unit and integration suites, database migration tests, and both
Spotless checks.

### Testcontainers warning investigation

The earlier `SQLSTATE 08006` shutdown warning was emitted while Spring
Modulith's event-publication registry was closing after a deliberately failed
integration-test process. The clean green reruns completed without a test
failure or that shutdown stack trace. No lifecycle code was changed because it
is unrelated to the conversation marker behavior; retain it as a CI
infrastructure observation if it recurs on passing builds.
