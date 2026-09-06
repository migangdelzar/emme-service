package com.emme.assistant.ai.adapter.out.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.assistant.ai.WorkflowTestCapabilities;
import com.emme.assistant.ai.application.workflow.NodeGuardrailPolicy;
import com.emme.assistant.ai.application.workflow.NodeMemoryPolicy;
import com.emme.assistant.ai.application.workflow.NodeModelRole;
import com.emme.assistant.ai.application.workflow.NodePolicyRegistry;
import com.emme.assistant.ai.application.workflow.NodeProfile;
import com.emme.assistant.ai.application.workflow.NodeToolPolicy;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.List;
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
    InvocationCounts calls = new InvocationCounts();
    ConversationWorkflowGraph graph =
        new ConversationWorkflowGraph(
            new TenantAwareCheckpointSaver(new MemorySaver()), capabilities(calls, true));
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

    assertThat(calls.intent()).hasValue(1);
    assertThat(calls.decomposition()).hasValue(1);
    assertThat(calls.routing()).hasValue(1);
    assertThat(calls.extraction()).hasValue(1);
    assertThat(calls.retrieval()).hasValue(1);
    assertThat(calls.tool()).hasValue(1);
    assertThat(calls.validation()).hasValue(1);
    assertThat(calls.response()).hasValue(1);
    assertThat(calls.quote()).hasValue(0);
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

  @Test
  void rejectsARegistryThatDoesNotProfileEveryGraphNode() {
    assertThatThrownBy(
            () ->
                new ConversationWorkflowGraph(
                    new TenantAwareCheckpointSaver(new MemorySaver()),
                    WorkflowTestCapabilities.basic(),
                    new NodePolicyRegistry(
                        List.of(profile(ConversationWorkflowGraph.RECEIVE_REQUEST)))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Missing node policy for node: resolve_authenticated_context");
  }

  private static CompiledGraph<AgentState> graph() throws Exception {
    return new ConversationWorkflowGraph(
            new TenantAwareCheckpointSaver(new MemorySaver()), WorkflowTestCapabilities.basic())
        .compile();
  }

  private static NodeProfile profile(String nodeId) {
    return new NodeProfile(
        nodeId,
        NodeModelRole.NONE,
        new NodeToolPolicy(Set.of(), true, false),
        new NodeMemoryPolicy(Set.of(), 0, false),
        new NodeGuardrailPolicy(true, true, true, true, true),
        0,
        Duration.ofSeconds(1),
        false,
        false);
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
    return capabilities(new InvocationCounts(intentCalls), needsApproval);
  }

  private static com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities
      capabilities(InvocationCounts calls, boolean needsApproval) {
    var defaults = WorkflowTestCapabilities.basic();
    return new com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities(
        request -> {
          calls.intent().incrementAndGet();
          return defaults.intentDetection().detect(request);
        },
        request -> {
          calls.decomposition().incrementAndGet();
          return defaults.decomposition().decompose(request);
        },
        request -> {
          calls.routing().incrementAndGet();
          return defaults.semanticRouting().route(request);
        },
        request -> {
          calls.extraction().incrementAndGet();
          return defaults.slotExtraction().extract(request);
        },
        request -> {
          calls.retrieval().incrementAndGet();
          return defaults.retrieval().retrieve(request);
        },
        request -> {
          calls.tool().incrementAndGet();
          return defaults.toolExecution().execute(request);
        },
        request -> {
          calls.validation().incrementAndGet();
          return new com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities
              .WorkflowStep(Map.of(), needsApproval, false, null);
        },
        request -> {
          calls.response().incrementAndGet();
          return defaults.responseComposition().compose(request);
        },
        request -> {
          calls.quote().incrementAndGet();
          return defaults.quoteWorkflow().execute(request);
        });
  }

  private static com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities
      terminalCapabilities(String terminal) {
    var defaults = WorkflowTestCapabilities.basic();
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

  private record InvocationCounts(
      AtomicInteger intent,
      AtomicInteger decomposition,
      AtomicInteger routing,
      AtomicInteger extraction,
      AtomicInteger retrieval,
      AtomicInteger tool,
      AtomicInteger validation,
      AtomicInteger response,
      AtomicInteger quote) {

    private InvocationCounts() {
      this(
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger());
    }

    private InvocationCounts(AtomicInteger intent) {
      this(
          intent,
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger(),
          new AtomicInteger());
    }
  }
}
