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
 * Generic, checkpointed conversation workflow shell.
 *
 * <p>The graph owns orchestration state only. Conversation history, pricing, appointment rules, and
 * other business data stay behind application ports and are never copied into graph state.
 */
public final class ConversationWorkflowGraph {

  public static final String RECEIVE_REQUEST = "receive_request";
  public static final String RESOLVE_AUTHENTICATED_CONTEXT = "resolve_authenticated_context";
  public static final String INITIALIZE_WORKFLOW = "initialize_workflow";
  public static final String NORMALIZE_INPUT = "normalize_input";
  public static final String DETECT_EXPLICIT_INTENT = "detect_explicit_intent";
  public static final String DECOMPOSE_MULTI_INTENT_REQUEST = "decompose_multi_intent_request";
  public static final String SEMANTIC_ROUTE_WITH_PGVECTOR = "semantic_route_with_pgvector";
  public static final String CONFIDENCE_GATE = "confidence_gate";
  public static final String EXTRACT_REQUIRED_SLOTS = "extract_required_slots";
  public static final String RETRIEVE_CONTEXT_IF_NEEDED = "retrieve_context_if_needed";
  public static final String EXECUTE_TOOL = "execute_tool";
  public static final String VALIDATE_BUSINESS_RESULT = "validate_business_result";
  public static final String APPROVAL_GATE = "approval_gate";
  public static final String WAIT_FOR_APPROVAL = "wait_for_approval";
  public static final String COMPOSE_RESPONSE = "compose_response";
  public static final String FINISH = "finish";

  private static final String STATUS = "status";
  private static final String LAST_NODE = "lastNode";
  private static final String NEEDS_APPROVAL = "needsApproval";

  private final BaseCheckpointSaver checkpointSaver;

  public ConversationWorkflowGraph(BaseCheckpointSaver checkpointSaver) {
    this.checkpointSaver =
        Objects.requireNonNull(checkpointSaver, "checkpointSaver must not be null");
  }

  public CompiledGraph<AgentState> compile() throws GraphStateException {
    StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);
    graph
        .addNode(RECEIVE_REQUEST, state("RECEIVED", RECEIVE_REQUEST))
        .addNode(RESOLVE_AUTHENTICATED_CONTEXT, state("RUNNING", RESOLVE_AUTHENTICATED_CONTEXT))
        .addNode(INITIALIZE_WORKFLOW, state("RUNNING", INITIALIZE_WORKFLOW))
        .addNode(NORMALIZE_INPUT, state("RUNNING", NORMALIZE_INPUT))
        .addNode(DETECT_EXPLICIT_INTENT, state("RUNNING", DETECT_EXPLICIT_INTENT))
        .addNode(DECOMPOSE_MULTI_INTENT_REQUEST, state("RUNNING", DECOMPOSE_MULTI_INTENT_REQUEST))
        .addNode(SEMANTIC_ROUTE_WITH_PGVECTOR, state("RUNNING", SEMANTIC_ROUTE_WITH_PGVECTOR))
        .addNode(CONFIDENCE_GATE, state("RUNNING", CONFIDENCE_GATE))
        .addNode(EXTRACT_REQUIRED_SLOTS, state("RUNNING", EXTRACT_REQUIRED_SLOTS))
        .addNode(RETRIEVE_CONTEXT_IF_NEEDED, state("RUNNING", RETRIEVE_CONTEXT_IF_NEEDED))
        .addNode(EXECUTE_TOOL, state("RUNNING", EXECUTE_TOOL))
        .addNode(VALIDATE_BUSINESS_RESULT, state("RUNNING", VALIDATE_BUSINESS_RESULT))
        .addNode(APPROVAL_GATE, state("RUNNING", APPROVAL_GATE))
        .addNode(WAIT_FOR_APPROVAL, state("WAITING_FOR_APPROVAL", WAIT_FOR_APPROVAL))
        .addNode(COMPOSE_RESPONSE, state("RUNNING", COMPOSE_RESPONSE))
        .addNode(FINISH, state("SUCCEEDED", FINISH));

    graph
        .addEdge(GraphDefinition.START, RECEIVE_REQUEST)
        .addEdge(RECEIVE_REQUEST, RESOLVE_AUTHENTICATED_CONTEXT)
        .addEdge(RESOLVE_AUTHENTICATED_CONTEXT, INITIALIZE_WORKFLOW)
        .addEdge(INITIALIZE_WORKFLOW, NORMALIZE_INPUT)
        .addEdge(NORMALIZE_INPUT, DETECT_EXPLICIT_INTENT)
        .addEdge(DETECT_EXPLICIT_INTENT, DECOMPOSE_MULTI_INTENT_REQUEST)
        .addEdge(DECOMPOSE_MULTI_INTENT_REQUEST, SEMANTIC_ROUTE_WITH_PGVECTOR)
        .addEdge(SEMANTIC_ROUTE_WITH_PGVECTOR, CONFIDENCE_GATE)
        .addEdge(CONFIDENCE_GATE, EXTRACT_REQUIRED_SLOTS)
        .addEdge(EXTRACT_REQUIRED_SLOTS, RETRIEVE_CONTEXT_IF_NEEDED)
        .addEdge(RETRIEVE_CONTEXT_IF_NEEDED, EXECUTE_TOOL)
        .addEdge(EXECUTE_TOOL, VALIDATE_BUSINESS_RESULT)
        .addEdge(VALIDATE_BUSINESS_RESULT, APPROVAL_GATE)
        .addConditionalEdges(
            APPROVAL_GATE,
            (AsyncEdgeAction<AgentState>)
                workflowState ->
                    CompletableFuture.completedFuture(
                        workflowState.<Boolean>value(NEEDS_APPROVAL).orElse(false)
                            ? WAIT_FOR_APPROVAL
                            : COMPOSE_RESPONSE),
            Map.of(
                WAIT_FOR_APPROVAL, WAIT_FOR_APPROVAL,
                COMPOSE_RESPONSE, COMPOSE_RESPONSE))
        .addEdge(WAIT_FOR_APPROVAL, GraphDefinition.END)
        .addEdge(COMPOSE_RESPONSE, FINISH)
        .addEdge(FINISH, GraphDefinition.END);

    return graph.compile(
        CompileConfig.builder()
            .checkpointSaver(checkpointSaver)
            .interruptAfter(WAIT_FOR_APPROVAL)
            .build());
  }

  private static AsyncNodeAction<AgentState> state(String status, String node) {
    return workflowState ->
        CompletableFuture.completedFuture(Map.of(STATUS, status, LAST_NODE, node));
  }
}
