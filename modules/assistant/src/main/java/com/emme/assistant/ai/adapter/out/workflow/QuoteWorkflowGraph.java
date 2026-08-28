package com.emme.assistant.ai.adapter.out.workflow;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.state.AgentState;

/**
 * LangGraph orchestration shell for the design-quote workflow.
 *
 * <p>Nodes only coordinate application boundaries and expose durable workflow states. They do not
 * calculate prices, authorize users, or call repositories directly; those responsibilities stay in
 * the Emme application and domain layers.
 */
public final class QuoteWorkflowGraph {

  private static final String RECEIVE_REQUEST = "receive_request";
  private static final String NORMALIZE_INPUT = "normalize_input";
  private static final String EXTRACT_REQUIRED_SLOTS = "extract_required_slots";
  private static final String CALCULATE_QUOTE = "calculate_quote";
  private static final String APPROVAL_GATE = "approval_gate";
  private static final String WAIT_FOR_STAFF = "wait_for_staff";
  private static final String COMPOSE_RESPONSE = "compose_response";

  private final BaseCheckpointSaver checkpointSaver;

  public QuoteWorkflowGraph(BaseCheckpointSaver checkpointSaver) {
    this.checkpointSaver =
        Objects.requireNonNull(checkpointSaver, "checkpointSaver must not be null");
  }

  public CompiledGraph<AgentState> compile() throws GraphStateException {
    StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);
    graph
        .addNode(RECEIVE_REQUEST, status("RECEIVED"))
        .addNode(NORMALIZE_INPUT, status("EXTRACTING"))
        .addNode(EXTRACT_REQUIRED_SLOTS, status("EXTRACTING"))
        .addNode(CALCULATE_QUOTE, status("QUOTE_CALCULATED"))
        .addNode(APPROVAL_GATE, noUpdate())
        .addNode(WAIT_FOR_STAFF, status("WAITING_FOR_STAFF"))
        .addNode(COMPOSE_RESPONSE, status("QUOTE_READY"));

    graph
        .addEdge(GraphDefinition.START, RECEIVE_REQUEST)
        .addEdge(RECEIVE_REQUEST, NORMALIZE_INPUT)
        .addEdge(NORMALIZE_INPUT, EXTRACT_REQUIRED_SLOTS)
        .addEdge(EXTRACT_REQUIRED_SLOTS, CALCULATE_QUOTE)
        .addEdge(CALCULATE_QUOTE, APPROVAL_GATE)
        .addConditionalEdges(
            APPROVAL_GATE,
            (AsyncEdgeAction<AgentState>)
                state ->
                    CompletableFuture.completedFuture(
                        state.<Boolean>value("needsReview").orElse(false)
                            ? WAIT_FOR_STAFF
                            : COMPOSE_RESPONSE),
            Map.of(WAIT_FOR_STAFF, WAIT_FOR_STAFF, COMPOSE_RESPONSE, COMPOSE_RESPONSE))
        .addEdge(WAIT_FOR_STAFF, GraphDefinition.END)
        .addEdge(COMPOSE_RESPONSE, GraphDefinition.END);

    return graph.compile(
        CompileConfig.builder()
            .checkpointSaver(checkpointSaver)
            .interruptAfter(WAIT_FOR_STAFF)
            .build());
  }

  private static AsyncNodeAction<AgentState> status(String status) {
    return state -> CompletableFuture.completedFuture(Map.of("status", status));
  }

  private static AsyncNodeAction<AgentState> noUpdate() {
    return state -> CompletableFuture.completedFuture(Map.of());
  }
}
