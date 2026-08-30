package com.emme.assistant.ai.adapter.out.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
    CompiledGraph<AgentState> graph = graph(capabilities(new AtomicInteger(), true));
    RunnableConfig config = RunnableConfig.builder().threadId(WORKFLOW_ID.toString()).build();

    AgentState paused = runWithContext(() -> graph.invoke(input(), config).orElseThrow());

    assertThat(paused.<String>value("status")).contains("WAITING_FOR_APPROVAL");

    RunnableConfig approved =
        runWithContext(
            () ->
                graph.updateState(
                    config,
                    Map.of(ConversationWorkflowGraph.DECISION, "APPROVE"),
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

  @Test
  void resumesWithoutReexecutingCompletedCapabilities() throws Exception {
    AtomicInteger intentCalls = new AtomicInteger();
    ConversationWorkflowGraph graph =
        new ConversationWorkflowGraph(
            new TenantAwareCheckpointSaver(new MemorySaver()), capabilities(intentCalls, true));
    CompiledGraph<AgentState> compiled = graph.compile();
    RunnableConfig config = RunnableConfig.builder().threadId(WORKFLOW_ID.toString()).build();

    AgentState paused = runWithContext(() -> compiled.invoke(input(), config).orElseThrow());
    assertThat(paused.<String>value("status")).contains("WAITING_FOR_APPROVAL");

    RunnableConfig approved =
        runWithContext(
            () ->
                compiled.updateState(
                    config,
                    Map.of(ConversationWorkflowGraph.DECISION, "APPROVE"),
                    ConversationWorkflowGraph.APPROVAL_GATE));
    runWithContext(() -> compiled.invoke(GraphInput.resume(), approved).orElseThrow());

    assertThat(intentCalls).hasValue(1);
  }

  @Test
  void routesValidationOutcomesToExplicitTerminalStates() throws Exception {
    for (String terminal : Set.of("FAILED", "REJECTED", "CLARIFICATION_REQUIRED")) {
      ConversationWorkflowGraph graph =
          new ConversationWorkflowGraph(
              new TenantAwareCheckpointSaver(new MemorySaver()), terminalCapabilities(terminal));

      AgentState result =
          runWithContext(
              () ->
                  graph
                      .compile()
                      .invoke(
                          input(),
                          RunnableConfig.builder().threadId(WORKFLOW_ID.toString()).build())
                      .orElseThrow());

      assertThat(result.<String>value("status")).contains(terminal);
    }
  }

  private static CompiledGraph<AgentState> graph() throws Exception {
    return new ConversationWorkflowGraph(new TenantAwareCheckpointSaver(new MemorySaver()))
        .compile();
  }

  private static CompiledGraph<AgentState> graph(
      com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities capabilities)
      throws Exception {
    return new ConversationWorkflowGraph(
            new TenantAwareCheckpointSaver(new MemorySaver()), capabilities)
        .compile();
  }

  private static Map<String, Object> input() {
    return Map.of(
        ConversationWorkflowGraph.MESSAGE, "hello",
        ConversationWorkflowGraph.TENANT_ID, TENANT_ID.toString(),
        ConversationWorkflowGraph.PRINCIPAL_ID, PRINCIPAL_ID.toString(),
        ConversationWorkflowGraph.CONVERSATION_ID, CONVERSATION_ID.toString(),
        ConversationWorkflowGraph.WORKFLOW_ID, WORKFLOW_ID.toString());
  }

  private static com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities
      capabilities(AtomicInteger intentCalls, boolean needsApproval) {
    var defaults =
        com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities.defaults();
    return new com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities(
        request -> {
          intentCalls.incrementAndGet();
          return defaults.intentDetection().detect(request);
        },
        defaults.decomposition(),
        defaults.semanticRouting(),
        defaults.slotExtraction(),
        defaults.retrieval(),
        defaults.toolExecution(),
        request ->
            new com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities
                .WorkflowStep(Map.of(), needsApproval, false, null),
        defaults.responseComposition(),
        defaults.quoteWorkflow());
  }

  private static com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities
      terminalCapabilities(String terminal) {
    var defaults =
        com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities.defaults();
    return new com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities(
        defaults.intentDetection(),
        defaults.decomposition(),
        defaults.semanticRouting(),
        defaults.slotExtraction(),
        defaults.retrieval(),
        defaults.toolExecution(),
        request ->
            new com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities
                .WorkflowStep(Map.of(), false, false, terminal),
        defaults.responseComposition(),
        defaults.quoteWorkflow());
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
