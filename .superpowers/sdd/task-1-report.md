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
