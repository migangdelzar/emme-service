package com.emme.assistant.ai.adapter.out.workflow;

import com.emme.assistant.ai.api.command.ProcessConversationCommand;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowPort;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Objects;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.StateSnapshot;

/** LangGraph4j adapter for the trusted generic conversation workflow boundary. */
public final class LangGraphConversationWorkflowAdapter implements ConversationWorkflowPort {

  private static final String STATUS = "status";

  private final CompiledGraph<AgentState> graph;

  public LangGraphConversationWorkflowAdapter(CompiledGraph<AgentState> graph) {
    this.graph = Objects.requireNonNull(graph, "graph must not be null");
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
              .invoke(Map.of("needsApproval", false), config)
              .orElseThrow(
                  () -> new IllegalStateException("Conversation workflow returned no state"));
      return snapshot(state, context);
    } catch (Exception exception) {
      throw new IllegalStateException(
          "Unable to start conversation workflow: " + context.workflowId(), exception);
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

  private static ConversationWorkflowSnapshot snapshot(
      StateSnapshot<AgentState> persisted, AiExecutionContext context) {
    return snapshot(persisted.state(), context);
  }

  private static ConversationWorkflowSnapshot snapshot(
      AgentState state, AiExecutionContext context) {
    String value =
        state
            .<String>value(STATUS)
            .orElseThrow(
                () -> new IllegalStateException("Conversation workflow state has no status"));
    try {
      return new ConversationWorkflowSnapshot(
          context.workflowId(),
          context.conversationId(),
          ConversationWorkflowStatus.valueOf(value));
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException(
          "Conversation workflow state has an invalid status: " + value, exception);
    }
  }
}
