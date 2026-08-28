package com.emme.assistant.ai.adapter.out.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.state.AgentState;
import org.junit.jupiter.api.Test;

class QuoteWorkflowGraphTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void reachesQuoteReadyWhenApprovalIsNotRequired() throws Exception {
    CompiledGraph<AgentState> graph = graph();

    AgentState result =
        runWithContext(
            () ->
                graph
                    .invoke(
                        Map.of("needsReview", false),
                        RunnableConfig.builder().threadId(WORKFLOW_ID.toString()).build())
                    .orElseThrow());

    assertThat(result.<String>value("status")).contains("QUOTE_READY");
  }

  @Test
  void pausesAfterWaitingForStaffWhenApprovalIsRequired() throws Exception {
    CompiledGraph<AgentState> graph = graph();

    AgentState result =
        runWithContext(
            () ->
                graph
                    .invoke(
                        Map.of("needsReview", true),
                        RunnableConfig.builder().threadId(WORKFLOW_ID.toString()).build())
                    .orElseThrow());

    assertThat(result.<String>value("status")).contains("WAITING_FOR_STAFF");
  }

  @Test
  void resumesThroughTheApprovalGateAfterStaffApprovesTheExtractedAttributes() throws Exception {
    CompiledGraph<AgentState> graph = graph();
    RunnableConfig config = RunnableConfig.builder().threadId(WORKFLOW_ID.toString()).build();

    runWithContext(() -> graph.invoke(Map.of("needsReview", true), config).orElseThrow());
    RunnableConfig approvedConfig =
        runWithContext(
            () -> graph.updateState(config, Map.of("needsReview", false), "approval_gate"));

    AgentState result =
        runWithContext(() -> graph.invoke(GraphInput.resume(), approvedConfig).orElseThrow());

    assertThat(result.<String>value("status")).contains("QUOTE_READY");
  }

  private static CompiledGraph<AgentState> graph() throws Exception {
    return new QuoteWorkflowGraph(new TenantAwareCheckpointSaver(new MemorySaver())).compile();
  }

  private static <T> T runWithContext(CheckedSupplier<T> action) {
    AiExecutionContext context =
        new AiExecutionContext(
            TENANT_ID,
            PRINCIPAL_ID,
            Set.of("CLIENT"),
            CONVERSATION_ID,
            WORKFLOW_ID,
            "trace-1",
            "idempotency-1");
    return AiExecutionContextScope.call(context, action::get);
  }

  @FunctionalInterface
  private interface CheckedSupplier<T> {
    T get() throws Exception;
  }
}
