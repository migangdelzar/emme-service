package com.emme.assistant.ai.adapter.out.workflow;

import com.emme.assistant.ai.application.port.out.QuoteWorkflowResumePort;
import com.emme.assistant.ai.domain.workflow.QuoteReviewDecisionType;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.state.AgentState;

/** Resumes the persisted quote graph after an authorized staff decision. */
public final class LangGraphQuoteWorkflowResumeAdapter implements QuoteWorkflowResumePort {

  private static final String APPROVAL_GATE_NODE = "approval_gate";
  private static final String QUOTE_NAMESPACE = "quote";

  private final CompiledGraph<AgentState> graph;

  public LangGraphQuoteWorkflowResumeAdapter(CompiledGraph<AgentState> graph) {
    this.graph = Objects.requireNonNull(graph, "graph must not be null");
  }

  @Override
  public void resume(UUID workflowId, QuoteReviewDecisionType decision) {
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(decision, "decision must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    if (!context.workflowId().equals(workflowId)) {
      throw new IllegalArgumentException("workflowId does not match AI execution context");
    }
    if (decision == QuoteReviewDecisionType.REJECTED) {
      return;
    }

    RunnableConfig config =
        RunnableConfig.builder().threadId(workflowId + ":" + QUOTE_NAMESPACE).build();
    try {
      RunnableConfig updated =
          graph.updateState(config, Map.of("needsReview", false), APPROVAL_GATE_NODE);
      graph.invoke(GraphInput.resume(), updated);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to resume quote workflow: " + workflowId, exception);
    }
  }
}
