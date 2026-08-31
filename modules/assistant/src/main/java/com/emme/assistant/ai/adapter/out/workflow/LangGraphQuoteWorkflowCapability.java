package com.emme.assistant.ai.adapter.out.workflow;

import com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities;
import java.util.Map;
import java.util.Objects;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.state.AgentState;

/** Invokes the existing compiled quote graph as the quote capability of the generic workflow. */
public final class LangGraphQuoteWorkflowCapability
    implements ConversationWorkflowCapabilities.QuoteWorkflowCapability {

  private static final String QUOTE_NAMESPACE = "quote";

  private final CompiledGraph<AgentState> quoteGraph;

  public LangGraphQuoteWorkflowCapability(CompiledGraph<AgentState> quoteGraph) {
    this.quoteGraph = Objects.requireNonNull(quoteGraph, "quoteGraph must not be null");
  }

  @Override
  public ConversationWorkflowCapabilities.WorkflowStep execute(
      ConversationWorkflowCapabilities.WorkflowRequest request) {
    try {
      RunnableConfig config =
          RunnableConfig.builder()
              .threadId(request.context().workflowId() + ":" + QUOTE_NAMESPACE)
              .build();
      AgentState state =
          quoteGraph
              .invoke(
                  Map.of(
                      "needsReview",
                      request.state().getOrDefault("needsQuoteReview", Boolean.FALSE)),
                  config)
              .orElseThrow(() -> new IllegalStateException("Quote workflow returned no state"));
      String status = state.<String>value("status").orElse("FAILED");
      return new ConversationWorkflowCapabilities.WorkflowStep(
          Map.of("quoteWorkflowStatus", status),
          "WAITING_FOR_STAFF".equals(status),
          false,
          "FAILED".equals(status) ? "FAILED" : null);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to execute quote workflow capability", exception);
    }
  }
}
