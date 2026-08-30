# Task 2 — Generic LangGraph4j workflow and persisted resume

## Scope

Implemented the generic, tenant-safe conversation workflow boundary in `modules/assistant`.
The graph uses the existing `TenantAwareCheckpointSaver`, so LangGraph4j checkpoint thread IDs
remain bound to the backend-derived `AiExecutionContext.workflowId`. Conversation history remains
behind `ConversationMemoryPort`; graph state contains orchestration status only.

## RED

Added failing tests before production code:

- `ConversationWorkflowGraphTest` for a paused approval checkpoint and resume to `SUCCEEDED`.
- `LangGraphConversationWorkflowAdapterTest` for trusted workflow/conversation identity.
- `ProcessConversationServiceWorkflowTest` for workflow invocation before the normal chat path.

Command run:

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test \
  --tests '*ConversationWorkflowGraphTest' \
  --tests '*LangGraphConversationWorkflowAdapterTest' \
  --tests '*ProcessConversationServiceWorkflowTest'
```

Result: failed at `compileTestJava` with the expected missing generic graph, adapter, port, and
workflow-domain symbols.

## GREEN

Added:

- `ConversationWorkflowPort`, `ConversationWorkflowStatus`, and `ConversationWorkflowSnapshot`.
- `ConversationWorkflowGraph` with the approved lifecycle nodes, conditional approval gate, and
  LangGraph interrupt/checkpoint behavior.
- `LangGraphConversationWorkflowAdapter`, which derives all graph identity from the authenticated
  `AiExecutionContext` and returns existing persisted state instead of re-invoking the graph.
- Spring configuration for separately named generic and quote compiled graphs.
- `ProcessConversationService` integration using the generic workflow port while retaining Task 1
  idempotency recovery before workflow execution.

## Refactor and self-review

- Promoted node names to graph constants.
- Kept workflow state separate from durable chat-memory events.
- Added qualified injection for both compiled graph beans; this prevents a runtime ambiguity when
  the quote resume port and generic conversation port are enabled together.
- Restored the assistant-marker recovery ordering after review: a retry after assistant persistence
  but before idempotency completion reconciles the durable response without graph/model re-execution.
- No repositories are called by graph nodes; business operations remain behind application ports.
- Existing quote graph behavior is untouched and continues using the same `TenantAwareCheckpointSaver`.

## Verification

Passed on Java 25.0.2:

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test \
  --tests '*ProcessConversationServiceTest' \
  --tests '*ProcessConversationServiceWorkflowTest' \
  --tests '*ConversationWorkflowGraphTest' \
  --tests '*LangGraphConversationWorkflowAdapterTest' \
  --tests '*QuoteWorkflowGraphTest' \
  --tests '*SpringAiLangGraphConfigurationTest' \
  :modules:assistant:spotlessCheck
```

Result: `BUILD SUCCESSFUL`.

Also passed:

```shell
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test \
  :modules:assistant:integrationTest \
  :modules:assistant:spotlessCheck
```

Result: completed successfully. Testcontainers emitted an existing Docker prune `409` warning from
its shutdown hook; the integration XML results contain no failures or errors.

## Files

- `modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/ConversationWorkflowPort.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/domain/workflow/ConversationWorkflowStatus.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/domain/workflow/ConversationWorkflowSnapshot.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/ConversationWorkflowGraph.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/workflow/LangGraphConversationWorkflowAdapter.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ProcessConversationService.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiLangGraphConfiguration.java`
- Task 2 unit tests and the configuration regression test.

## Concerns

- The generic graph intentionally owns orchestration state only. Concrete quote extraction and
  appointment tool execution remain application capabilities and are connected by their respective
  vertical-slice tasks; no LLM or graph node gains repository/domain-rule access.
- The pre-existing unrelated identity, subscriptions, and tenancy working-tree edits were not
  staged or modified.
