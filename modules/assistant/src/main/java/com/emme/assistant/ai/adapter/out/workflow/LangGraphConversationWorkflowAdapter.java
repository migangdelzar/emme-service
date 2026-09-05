package com.emme.assistant.ai.adapter.out.workflow;

import com.emme.assistant.ai.api.command.ProcessConversationCommand;
import com.emme.assistant.ai.api.command.ResumeConversationWorkflowCommand;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowPort;
import com.emme.assistant.ai.application.security.AiStaffRolePolicy;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowDecision;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.StateSnapshot;

/** LangGraph4j adapter for the trusted generic conversation workflow boundary. */
public final class LangGraphConversationWorkflowAdapter implements ConversationWorkflowPort {

  private final CompiledGraph<AgentState> graph;

  public LangGraphConversationWorkflowAdapter(CompiledGraph<AgentState> graph) {
    this.graph = Objects.requireNonNull(graph, "graph must not be null");
  }

  @Override
  public boolean ownsResponse() {
    return true;
  }

  @Override
  public ConversationWorkflowSnapshot startOrResume(
      ProcessConversationCommand command, AiExecutionContext context) {
    validate(command, context);
    RunnableConfig config =
        RunnableConfig.builder().threadId(context.workflowId().toString()).build();
    try {
      var existing = graph.lastStateOf(config);
      if (existing.isPresent()) {
        return snapshot(existing.get(), context);
      }
      AgentState state =
          graph
              .invoke(initialState(command, context), config)
              .orElseThrow(
                  () -> new IllegalStateException("Conversation workflow returned no state"));
      return snapshot(state, context);
    } catch (CompletionException exception) {
      throw new IllegalStateException(
          "Unable to start conversation workflow: " + context.workflowId(), exception);
    } catch (RuntimeException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException(
          "Unable to start conversation workflow: " + context.workflowId(), exception);
    }
  }

  @Override
  public ConversationWorkflowSnapshot resume(
      ResumeConversationWorkflowCommand command, AiExecutionContext context) {
    validate(command, context);
    RunnableConfig config =
        RunnableConfig.builder().threadId(context.workflowId().toString()).build();
    try {
      var existing =
          graph
              .lastStateOf(config)
              .orElseThrow(
                  () -> new IllegalStateException("Conversation workflow checkpoint not found"));
      ConversationWorkflowSnapshot persisted = snapshot(existing, context);
      if (!isWaiting(persisted.status())) {
        return persisted;
      }
      validateResumeDecision(command, persisted, context);
      RunnableConfig updated = updateForDecision(config, command, persisted);
      AgentState resumed =
          graph
              .invoke(GraphInput.resume(), updated)
              .orElseThrow(
                  () ->
                      new IllegalStateException("Conversation workflow returned no resumed state"));
      return snapshot(resumed, context);
    } catch (CompletionException exception) {
      throw new IllegalStateException(
          "Unable to resume conversation workflow: " + context.workflowId(), exception);
    } catch (RuntimeException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException(
          "Unable to resume conversation workflow: " + context.workflowId(), exception);
    }
  }

  private static void validate(ProcessConversationCommand command, AiExecutionContext context) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(context, "context must not be null");
    AiExecutionContext current = AiExecutionContextScope.requireCurrent();
    if (!current.equals(context)) {
      throw new SecurityException("AI execution context must be the authenticated current context");
    }
    if (!context.conversationId().equals(command.conversationId())) {
      throw new IllegalArgumentException("conversationId does not match AI execution context");
    }
    if (!context.idempotencyKey().equals(command.idempotencyKey())) {
      throw new IllegalArgumentException("idempotencyKey does not match AI execution context");
    }
  }

  private static void validate(
      ResumeConversationWorkflowCommand command, AiExecutionContext context) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(context, "context must not be null");
    AiExecutionContext current = AiExecutionContextScope.requireCurrent();
    if (!current.equals(context)) {
      throw new SecurityException("AI execution context must be the authenticated current context");
    }
    if (!context.workflowId().equals(command.workflowId())) {
      throw new IllegalArgumentException("workflowId does not match AI execution context");
    }
    if (!context.conversationId().equals(command.conversationId())) {
      throw new IllegalArgumentException("conversationId does not match AI execution context");
    }
    if (command.decision() != ConversationWorkflowDecision.PROVIDE_CLARIFICATION
        && !AiStaffRolePolicy.isStaff(context.roles())) {
      throw new SecurityException("Staff role is required to resume a conversation workflow");
    }
  }

  private static ConversationWorkflowSnapshot snapshot(
      StateSnapshot<AgentState> persisted, AiExecutionContext context) {
    return snapshot(persisted.state(), context);
  }

  private static ConversationWorkflowSnapshot snapshot(
      AgentState state, AiExecutionContext context) {
    ConversationWorkflowGraph.verifyIdentity(state, context);
    String value =
        state
            .<String>value(ConversationWorkflowGraph.STATUS)
            .orElseThrow(
                () -> new IllegalStateException("Conversation workflow state has no status"));
    try {
      return new ConversationWorkflowSnapshot(
          context.workflowId(),
          context.conversationId(),
          ConversationWorkflowStatus.valueOf(value),
          context.tenantId(),
          ConversationWorkflowGraph.ownerPrincipalId(state),
          state
              .<String>value(ConversationWorkflowGraph.MESSAGE)
              .orElseThrow(
                  () -> new IllegalStateException("Conversation workflow state has no message")),
          state
              .<String>value(ConversationWorkflowGraph.IDEMPOTENCY_KEY)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Conversation workflow state has no idempotency key")),
          state.<String>value(ConversationWorkflowGraph.RESPONSE).orElse(null));
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException(
          "Conversation workflow state has an invalid status: " + value, exception);
    }
  }

  private static Map<String, Object> initialState(
      ProcessConversationCommand command, AiExecutionContext context) {
    return Map.of(
        ConversationWorkflowGraph.MESSAGE, command.message(),
        ConversationWorkflowGraph.IDEMPOTENCY_KEY, command.idempotencyKey(),
        ConversationWorkflowGraph.TENANT_ID, context.tenantId().toString(),
        ConversationWorkflowGraph.PRINCIPAL_ID, context.principalId().toString(),
        ConversationWorkflowGraph.CONVERSATION_ID, context.conversationId().toString(),
        ConversationWorkflowGraph.WORKFLOW_ID, context.workflowId().toString());
  }

  private RunnableConfig updateForDecision(
      RunnableConfig config,
      ResumeConversationWorkflowCommand command,
      ConversationWorkflowSnapshot persisted)
      throws Exception {
    if (persisted.status() == ConversationWorkflowStatus.CLARIFICATION_REQUIRED) {
      return graph.updateState(
          config,
          Map.of(
              ConversationWorkflowGraph.DECISION, command.decision().name(),
              ConversationWorkflowGraph.CLARIFICATION_ANSWER, command.clarification().answer(),
              ConversationWorkflowGraph.CLARIFICATION_SLOTS, command.clarification().slots(),
              ConversationWorkflowGraph.TERMINAL_STATUS, ""),
          ConversationWorkflowGraph.APPROVAL_GATE);
    }
    return graph.updateState(
        config,
        Map.of(ConversationWorkflowGraph.DECISION, command.decision().name()),
        ConversationWorkflowGraph.APPROVAL_GATE);
  }

  private static void validateResumeDecision(
      ResumeConversationWorkflowCommand command,
      ConversationWorkflowSnapshot persisted,
      AiExecutionContext actorContext) {
    if (persisted.status() == ConversationWorkflowStatus.CLARIFICATION_REQUIRED) {
      if (command.decision() != ConversationWorkflowDecision.PROVIDE_CLARIFICATION) {
        throw new IllegalStateException(
            "Clarification is required before the workflow can continue");
      }
      if (!persisted.principalId().equals(actorContext.principalId())) {
        throw new SecurityException("Only the workflow owner can provide clarification");
      }
      return;
    }
    if (command.decision() == ConversationWorkflowDecision.PROVIDE_CLARIFICATION) {
      throw new IllegalStateException("The workflow is not waiting for clarification");
    }
  }

  private static boolean isWaiting(ConversationWorkflowStatus status) {
    return status == ConversationWorkflowStatus.WAITING_FOR_APPROVAL
        || status == ConversationWorkflowStatus.WAITING_FOR_CONFIRMATION
        || status == ConversationWorkflowStatus.CLARIFICATION_REQUIRED;
  }
}
