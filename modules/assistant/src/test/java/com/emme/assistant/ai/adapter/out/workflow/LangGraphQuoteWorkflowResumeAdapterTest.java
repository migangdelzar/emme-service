package com.emme.assistant.ai.adapter.out.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.emme.assistant.ai.domain.workflow.QuoteReviewDecisionType;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.StateSnapshot;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings({"unchecked", "rawtypes"})
class LangGraphQuoteWorkflowResumeAdapterTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void resumesOnlyAfterAnApprovalOrEditDecision() throws Exception {
    CompiledGraph<AgentState> graph = mock(CompiledGraph.class);
    RunnableConfig updated = RunnableConfig.builder().threadId(WORKFLOW_ID.toString()).build();
    StateSnapshot<AgentState> checkpoint = mock(StateSnapshot.class);
    org.mockito.Mockito.when(graph.lastStateOf(any(RunnableConfig.class)))
        .thenReturn(Optional.of(checkpoint));
    org.mockito.Mockito.when(
            graph.updateState(any(RunnableConfig.class), anyMap(), eq("approval_gate")))
        .thenReturn(updated);
    LangGraphQuoteWorkflowResumeAdapter adapter = new LangGraphQuoteWorkflowResumeAdapter(graph);

    AiExecutionContextScope.run(
        context(),
        () -> {
          adapter.resume(WORKFLOW_ID, QuoteReviewDecisionType.APPROVED);
          adapter.resume(WORKFLOW_ID, QuoteReviewDecisionType.EDITED);
        });

    verify(graph, org.mockito.Mockito.times(2))
        .updateState(
            any(RunnableConfig.class), eq(Map.of("needsReview", false)), eq("approval_gate"));
    ArgumentCaptor<RunnableConfig> configs = ArgumentCaptor.forClass(RunnableConfig.class);
    verify(graph, org.mockito.Mockito.times(2))
        .updateState(configs.capture(), anyMap(), eq("approval_gate"));
    assertThat(configs.getAllValues())
        .extracting(config -> config.threadId().orElseThrow())
        .containsOnly(WORKFLOW_ID + ":quote");
    verify(graph, org.mockito.Mockito.times(2)).invoke(any(GraphInput.class), eq(updated));
  }

  @Test
  void doesNotResumeARejectedWorkflow() {
    CompiledGraph<AgentState> graph = mock(CompiledGraph.class);
    LangGraphQuoteWorkflowResumeAdapter adapter = new LangGraphQuoteWorkflowResumeAdapter(graph);

    AiExecutionContextScope.run(
        context(), () -> adapter.resume(WORKFLOW_ID, QuoteReviewDecisionType.REJECTED));

    verifyNoInteractions(graph);
  }

  @Test
  void rejectsAWorkflowIdThatIsNotTheAuthenticatedWorkflowContext() {
    LangGraphQuoteWorkflowResumeAdapter adapter =
        new LangGraphQuoteWorkflowResumeAdapter(mock(CompiledGraph.class));

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.run(
                    context(),
                    () -> adapter.resume(UUID.randomUUID(), QuoteReviewDecisionType.APPROVED)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("workflowId does not match AI execution context");
  }

  @Test
  void rejectsResumeWhenTheQuoteWorkflowCheckpointDoesNotExist() throws Exception {
    CompiledGraph<AgentState> graph = mock(CompiledGraph.class);
    org.mockito.Mockito.when(graph.lastStateOf(any(RunnableConfig.class)))
        .thenReturn(Optional.empty());
    LangGraphQuoteWorkflowResumeAdapter adapter = new LangGraphQuoteWorkflowResumeAdapter(graph);

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.run(
                    context(), () -> adapter.resume(WORKFLOW_ID, QuoteReviewDecisionType.APPROVED)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to resume quote workflow: " + WORKFLOW_ID)
        .hasRootCauseMessage("Quote workflow checkpoint not found");

    verify(graph, never()).updateState(any(RunnableConfig.class), anyMap(), eq("approval_gate"));
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        TENANT_ID,
        PRINCIPAL_ID,
        Set.of("tenant_staff"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-1",
        "review-1");
  }
}
