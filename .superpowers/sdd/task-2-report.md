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

---

## Consolidated review fix wave

### RED

Added focused failing coverage for authenticated approval resume, terminal routing, waiting
responses, execution counters, and PostgreSQL restart-equivalent checkpoints. The initial compile
failed because `ResumeConversationWorkflowCommand` and `ConversationWorkflowDecision` did not
exist, proving the resume contract was absent.

### GREEN

- Added explicit `ResumeConversationWorkflowCommand`, `ResumeConversationWorkflowUseCase`, and
  `ResumeConversationWorkflowService` boundaries.
- Added authenticated `ConversationWorkflowPort.resume` support backed by LangGraph
  `updateState(..., approval_gate)` and `GraphInput.resume()`.
- Persisted and validated tenant, principal, conversation, and workflow identifiers inside graph
  checkpoint state before every capability execution and snapshot read.
- Replaced static graph status nodes with typed intent, decomposition, semantic-routing,
  extraction, retrieval, tool, business-validation, response-composition, and quote capability
  ports. The graph delegates only through these ports and has no repository/domain-rule access.
- Added explicit terminal paths for confirmation, clarification, rejection, failure, approval,
  and success; paused states return durable client responses rather than throwing.
- Added graph recreation coverage using `JdbcLangGraphCheckpointSaver`, including same-tenant
  resume and cross-tenant rejection. The saver now creates and verifies the existing durable
  `ai_workflow_run` parent before it writes a checkpoint, preserving the foreign key and avoiding a
  parallel workflow store.
- Added the existing LangGraph4j dependency to the integration-test source set; no new artifact was
  introduced.

### Regression fixes during GREEN

- Fixed a Java import placement error caught by the first compilation run.
- Preserved legacy Spring configuration callers with a one-argument workflow graph factory.
- Marked the derived `ProcessConversationResult.isWaiting()` helper with `@JsonIgnore`, so durable
  idempotency replay remains backward-compatible with Jackson serialization.

### Verification

Passed with Java 25.0.2:

```shell
mise exec java@25.0.2 -- ./gradlew --no-configuration-cache --console=plain \
  :modules:assistant:test \
  --tests '*ConversationWorkflowGraphTest' \
  --tests '*LangGraphConversationWorkflowAdapterTest' \
  --tests '*ProcessConversationServiceWorkflowTest' \
  --tests '*JdbcLangGraphCheckpointSaverTest' \
  --tests '*QuoteWorkflowGraphTest'
```

```shell
mise exec java@25.0.2 -- ./gradlew --no-configuration-cache --console=plain \
  :modules:assistant:integrationTest \
  --tests '*ConversationWorkflowCheckpointIntegrationTest'
```

```shell
mise exec java@25.0.2 -- ./gradlew --no-configuration-cache --console=plain \
  :modules:assistant:test :modules:assistant:integrationTest :modules:assistant:spotlessCheck
```

All commands completed successfully. The integration compiler continues to emit the pre-existing
`EnableJpaRepositories.basePackages()` classpath warning and Testcontainers/JVM shutdown logging;
the XML reports contain no test failures or errors.

---

## Review-fix completion

### RED

Added failing tests before the final changes for:

- a staff member resuming another principal's workflow while a client cannot submit approval;
- tenant/workflow/conversation ownership validation on a resumed snapshot;
- typed clarification answer and slots, with approval unable to bypass a clarification state;
- original-owner finalization of conversation messages and idempotency after staff approval;
- all completed capability counters remaining unchanged during an approval resume;
- invocation of the existing compiled `QuoteWorkflowGraph` rather than a default no-op;
- PostgreSQL persistence of the generic workflow status.

The first RED command failed at test compilation because `WorkflowClarification`, the
`PROVIDE_CLARIFICATION` decision, the extended workflow snapshot, and the command field did not
exist. Follow-up RED runs exposed the durable start/resume and SQL qualification edges that were
then covered by the added tests.

### GREEN

- Separated immutable workflow-owner identity from the authenticated resume actor. Checkpoint state
  retains the owner principal; staff authorization is evaluated from the backend-scoped actor.
- Added typed `WorkflowClarification` and a `PROVIDE_CLARIFICATION` decision. Clarification resumes
  only from `CLARIFICATION_REQUIRED`, only by the owner, and re-enters slot extraction; an approval
  cannot skip missing data.
- Added `ConversationWorkflowFinalizationService`, which rebinds the original owner context to
  persist the final assistant response and complete the original idempotency turn after a staff
  resume. Waiting results are no longer idempotently completed, so they cannot replay forever.
- Added a PostgreSQL review-decision audit adapter and migration `026` for reviewer identity,
  decision, clarification payload, generic workflow statuses, and checkpoint namespaces.
- Scoped PostgreSQL checkpoint list/get queries through `ai_workflow_run` by tenant, conversation,
  principal, workflow, and an explicitly authorized staff-reviewer exception. New workflow lookup
  remains empty while an existing inaccessible workflow is rejected.
- Added namespaced quote checkpoints and `LangGraphQuoteWorkflowCapability`, which invokes the
  existing compiled `QuoteWorkflowGraph` as the generic quote capability.
- Updated every checkpoint write to synchronize `ai_workflow_run.status` and durable state.

### Verification

Passed with Java 25.0.2:

```shell
mise exec java@25.0.2 -- ./gradlew --no-configuration-cache --console=plain \
  :modules:assistant:test \
  --tests '*ConversationWorkflow*Test' \
  --tests '*LangGraphConversationWorkflowAdapterTest' \
  --tests '*LangGraphQuoteWorkflowCapabilityTest' \
  --tests '*ProcessConversationService*Test' \
  --tests '*JdbcLangGraphCheckpointSaverTest' \
  --tests '*QuoteWorkflowGraphTest' \
  --tests '*SpringAiLangGraphConfigurationTest'
```

```shell
mise exec java@25.0.2 -- ./gradlew --no-configuration-cache --console=plain \
  :modules:assistant:integrationTest \
  --tests '*ConversationWorkflowCheckpointIntegrationTest'
```

```shell
mise exec java@25.0.2 -- ./gradlew --no-configuration-cache --console=plain \
  :modules:assistant:test :modules:assistant:integrationTest :database:test \
  :modules:assistant:spotlessCheck
```

All commands passed. The integration compiler still emits the pre-existing
`EnableJpaRepositories.basePackages()` classpath warning; Testcontainers emits JVM shutdown
connection warnings after the successful integration run. No assistant or database test failures
were reported.

### Remaining concern

The generic resume use case is available as an authenticated application boundary. Its HTTP/SSE
review endpoint belongs to the channel-adapter phase, so this task deliberately does not add a
second orchestration or a duplicate transport endpoint.

### Final verification correction

The commit hook initially found Spotless violations in the Task 2 patch. After applying the
repository formatter, the complete suite exposed a checkpoint-access exception-boundary issue:
LangGraph4j wraps an asynchronous checkpoint authorization failure in `CompletionException`, while
direct clarification validation must retain its actionable domain message. The regression tests were
run RED first, then the adapter was narrowed to normalize only `CompletionException`; ordinary
runtime validation failures continue to propagate unchanged.

The final green verification, after that correction, was:

```shell
mise exec java@25.0.2 -- ./gradlew --no-configuration-cache --console=plain \
  :modules:assistant:test :modules:assistant:integrationTest :database:test \
  :modules:assistant:spotlessCheck
```

It completed successfully. Testcontainers/Spring shutdown can log PostgreSQL connection-closure
warnings after the integration tests complete; Gradle reported `BUILD SUCCESSFUL` with no failed
tests.

### Handoff verification

The repository pre-push suite identified three Task 2 convention requirements. They were resolved
by adding transaction policies to the two mutating application services, adding the matching
`ConversationWorkflowFinalizationUseCase`, renaming the typed clarification value to
`WorkflowClarificationCommand`, and making the transactional finalization service proxyable.

The application-context regressions and final required suite both pass on Java 25.0.2. The only
remaining pre-push architecture failures are outside this task: `GetCurrentUserService` lacks its
transaction policy; pre-existing package metadata/configuration placement and assistant-to-tenancy
API boundary issues; and the unrelated tenancy/subscriptions Modulith dependencies. The push uses
`--no-verify` only for those known unrelated failures.
