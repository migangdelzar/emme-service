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

class ConversationWorkflowGraphTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void pausesAtApprovalAndResumesFromThePersistedCheckpoint() throws Exception {
    CompiledGraph<AgentState> graph = graph();
    RunnableConfig config = RunnableConfig.builder().threadId(WORKFLOW_ID.toString()).build();

    AgentState paused =
        runWithContext(() -> graph.invoke(Map.of("needsApproval", true), config).orElseThrow());

    assertThat(paused.<String>value("status")).contains("WAITING_FOR_APPROVAL");

    RunnableConfig approved =
        runWithContext(
            () ->
                graph.updateState(
                    config,
                    Map.of("needsApproval", false, "approval", "approved"),
                    ConversationWorkflowGraph.APPROVAL_GATE));
    AgentState resumed =
        runWithContext(() -> graph.invoke(GraphInput.resume(), approved).orElseThrow());

    assertThat(resumed.<String>value("status")).contains("SUCCEEDED");
  }

  @Test
  void completesTheGenericLifecycleWithoutApproval() throws Exception {
    CompiledGraph<AgentState> graph = graph();

    AgentState result =
        runWithContext(
            () ->
                graph
                    .invoke(
                        Map.of("needsApproval", false),
                        RunnableConfig.builder().threadId(WORKFLOW_ID.toString()).build())
                    .orElseThrow());

    assertThat(result.<String>value("status")).contains("SUCCEEDED");
    assertThat(result.<String>value("lastNode")).contains(ConversationWorkflowGraph.FINISH);
  }

  private static CompiledGraph<AgentState> graph() throws Exception {
    return new ConversationWorkflowGraph(new TenantAwareCheckpointSaver(new MemorySaver()))
        .compile();
  }

  private static <T> T runWithContext(CheckedSupplier<T> action) {
    AiExecutionContext context =
        new AiExecutionContext(
            TENANT_ID,
            PRINCIPAL_ID,
            Set.of("CLIENT"),
            CONVERSATION_ID,
            WORKFLOW_ID,
            "trace-2",
            "idempotency-2");
    return AiExecutionContextScope.call(context, action::get);
  }

  @FunctionalInterface
  private interface CheckedSupplier<T> {
    T get() throws Exception;
  }
}
