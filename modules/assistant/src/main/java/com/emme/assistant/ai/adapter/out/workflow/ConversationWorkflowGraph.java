package com.emme.assistant.ai.adapter.out.workflow;

import com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities.WorkflowRequest;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities.WorkflowStep;
import com.emme.assistant.ai.application.workflow.NodeGuardrailPolicy;
import com.emme.assistant.ai.application.workflow.NodeMemoryPolicy;
import com.emme.assistant.ai.application.workflow.NodeModelRole;
import com.emme.assistant.ai.application.workflow.NodePolicyRegistry;
import com.emme.assistant.ai.application.workflow.NodeProfile;
import com.emme.assistant.ai.application.workflow.NodeToolPolicy;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
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
 * Generic, checkpointed conversation workflow.
 *
 * <p>The graph coordinates typed application capabilities only. Business decisions stay in the use
 * cases behind those capabilities and every checkpoint carries the authenticated execution identity
 * needed to reject cross-tenant or cross-principal resume attempts.
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
  public static final String EXECUTE_QUOTE_WORKFLOW = "execute_quote_workflow";
  public static final String VALIDATE_BUSINESS_RESULT = "validate_business_result";
  public static final String APPROVAL_GATE = "approval_gate";
  public static final String WAIT_FOR_APPROVAL = "wait_for_approval";
  public static final String WAIT_FOR_CONFIRMATION = "wait_for_confirmation";
  public static final String CLARIFICATION_REQUIRED = "clarification_required";
  public static final String REJECTED = "rejected";
  public static final String FAILED = "failed";
  public static final String COMPOSE_RESPONSE = "compose_response";
  public static final String FINISH = "finish";

  public static final String STATUS = "status";
  public static final String LAST_NODE = "lastNode";
  public static final String MESSAGE = "message";
  public static final String IDEMPOTENCY_KEY = "idempotencyKey";
  public static final String RESPONSE = "response";
  public static final String CLARIFICATION_ANSWER = "clarificationAnswer";
  public static final String CLARIFICATION_SLOTS = "clarificationSlots";
  public static final String TENANT_ID = "tenantId";
  public static final String PRINCIPAL_ID = "principalId";
  public static final String CONVERSATION_ID = "conversationId";
  public static final String WORKFLOW_ID = "workflowId";
  public static final String NEEDS_APPROVAL = "needsApproval";
  public static final String NEEDS_CONFIRMATION = "needsConfirmation";
  public static final String TERMINAL_STATUS = "terminalStatus";
  public static final String DECISION = "decision";
  private static final String ROUTE = "route";

  private final BaseCheckpointSaver checkpointSaver;
  private final ConversationWorkflowCapabilities capabilities;
  private final NodePolicyRegistry nodePolicies;

  public ConversationWorkflowGraph(
      BaseCheckpointSaver checkpointSaver, ConversationWorkflowCapabilities capabilities) {
    this(checkpointSaver, capabilities, defaultNodePolicyRegistry());
  }

  public ConversationWorkflowGraph(
      BaseCheckpointSaver checkpointSaver,
      ConversationWorkflowCapabilities capabilities,
      NodePolicyRegistry nodePolicies) {
    this.checkpointSaver =
        Objects.requireNonNull(checkpointSaver, "checkpointSaver must not be null");
    this.capabilities = Objects.requireNonNull(capabilities, "capabilities must not be null");
    this.nodePolicies = Objects.requireNonNull(nodePolicies, "nodePolicies must not be null");
    validateNodePolicies(nodePolicies);
  }

  public static NodePolicyRegistry defaultNodePolicyRegistry() {
    return new NodePolicyRegistry(
        workflowNodeIds().stream().map(ConversationWorkflowGraph::defaultProfile).toList());
  }

  public CompiledGraph<AgentState> compile() throws GraphStateException {
    StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);
    graph
        .addNode(RECEIVE_REQUEST, this::receive)
        .addNode(RESOLVE_AUTHENTICATED_CONTEXT, status("RUNNING", RESOLVE_AUTHENTICATED_CONTEXT))
        .addNode(INITIALIZE_WORKFLOW, status("RUNNING", INITIALIZE_WORKFLOW))
        .addNode(NORMALIZE_INPUT, status("RUNNING", NORMALIZE_INPUT))
        .addNode(
            DETECT_EXPLICIT_INTENT,
            capability(DETECT_EXPLICIT_INTENT, capabilities.intentDetection()::detect))
        .addNode(
            DECOMPOSE_MULTI_INTENT_REQUEST,
            capability(DECOMPOSE_MULTI_INTENT_REQUEST, capabilities.decomposition()::decompose))
        .addNode(
            SEMANTIC_ROUTE_WITH_PGVECTOR,
            capability(SEMANTIC_ROUTE_WITH_PGVECTOR, capabilities.semanticRouting()::route))
        .addNode(CONFIDENCE_GATE, status("RUNNING", CONFIDENCE_GATE))
        .addNode(
            EXTRACT_REQUIRED_SLOTS,
            capability(EXTRACT_REQUIRED_SLOTS, capabilities.slotExtraction()::extract))
        .addNode(
            RETRIEVE_CONTEXT_IF_NEEDED,
            capability(RETRIEVE_CONTEXT_IF_NEEDED, capabilities.retrieval()::retrieve))
        .addNode(EXECUTE_TOOL, capability(EXECUTE_TOOL, capabilities.toolExecution()::execute))
        .addNode(
            EXECUTE_QUOTE_WORKFLOW,
            capability(EXECUTE_QUOTE_WORKFLOW, capabilities.quoteWorkflow()::execute))
        .addNode(
            VALIDATE_BUSINESS_RESULT,
            capability(VALIDATE_BUSINESS_RESULT, capabilities.businessValidation()::validate))
        .addNode(APPROVAL_GATE, status("RUNNING", APPROVAL_GATE))
        .addNode(WAIT_FOR_APPROVAL, status("WAITING_FOR_APPROVAL", WAIT_FOR_APPROVAL))
        .addNode(WAIT_FOR_CONFIRMATION, status("WAITING_FOR_CONFIRMATION", WAIT_FOR_CONFIRMATION))
        .addNode(CLARIFICATION_REQUIRED, status("CLARIFICATION_REQUIRED", CLARIFICATION_REQUIRED))
        .addNode(REJECTED, status("REJECTED", REJECTED))
        .addNode(FAILED, status("FAILED", FAILED))
        .addNode(
            COMPOSE_RESPONSE,
            capability(COMPOSE_RESPONSE, capabilities.responseComposition()::compose))
        .addNode(FINISH, status("SUCCEEDED", FINISH));

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
        .addConditionalEdges(
            EXECUTE_TOOL,
            routeFromTool(),
            Map.of(
                EXECUTE_QUOTE_WORKFLOW,
                EXECUTE_QUOTE_WORKFLOW,
                VALIDATE_BUSINESS_RESULT,
                VALIDATE_BUSINESS_RESULT))
        .addEdge(EXECUTE_QUOTE_WORKFLOW, VALIDATE_BUSINESS_RESULT)
        .addConditionalEdges(VALIDATE_BUSINESS_RESULT, terminalOrApproval(), routes())
        .addConditionalEdges(APPROVAL_GATE, approvalDecision(), routes())
        .addEdge(WAIT_FOR_APPROVAL, GraphDefinition.END)
        .addEdge(WAIT_FOR_CONFIRMATION, GraphDefinition.END)
        .addEdge(CLARIFICATION_REQUIRED, GraphDefinition.END)
        .addEdge(REJECTED, GraphDefinition.END)
        .addEdge(FAILED, GraphDefinition.END)
        .addEdge(COMPOSE_RESPONSE, FINISH)
        .addEdge(FINISH, GraphDefinition.END);

    return graph.compile(
        CompileConfig.builder()
            .checkpointSaver(checkpointSaver)
            .interruptAfter(WAIT_FOR_APPROVAL, WAIT_FOR_CONFIRMATION, CLARIFICATION_REQUIRED)
            .build());
  }

  private CompletableFuture<Map<String, Object>> receive(AgentState state) {
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    Map<String, Object> update = identity(context);
    update.put(STATUS, "RECEIVED");
    update.put(LAST_NODE, RECEIVE_REQUEST);
    return CompletableFuture.completedFuture(update);
  }

  private AsyncNodeAction<AgentState> capability(
      String node, Function<WorkflowRequest, WorkflowStep> capability) {
    return state -> {
      AiExecutionContext context = AiExecutionContextScope.requireCurrent();
      verifyIdentity(state, context);
      NodeProfile profile = nodePolicies.profile(node);
      long startedAt = System.nanoTime();
      WorkflowStep step = capability.apply(request(state, context, profile));
      if ((step.needsApproval() || step.needsConfirmation()) && !profile.mayInterrupt()) {
        String decision = step.needsApproval() ? "approval" : "confirmation";
        throw new IllegalStateException("Node " + node + " cannot interrupt for " + decision);
      }
      if (Duration.ofNanos(System.nanoTime() - startedAt).compareTo(profile.timeout()) > 0) {
        throw new IllegalStateException("Node " + node + " exceeded its configured timeout");
      }
      Map<String, Object> update = new HashMap<>(step.updates());
      update.put(STATUS, "RUNNING");
      update.put(LAST_NODE, node);
      update.put(NEEDS_APPROVAL, step.needsApproval());
      update.put(NEEDS_CONFIRMATION, step.needsConfirmation());
      update.put(TERMINAL_STATUS, step.terminalStatus() == null ? "" : step.terminalStatus());
      if (EXTRACT_REQUIRED_SLOTS.equals(node)
          && "PROVIDE_CLARIFICATION".equals(state.<String>value(DECISION).orElse(""))) {
        update.put(DECISION, "");
      }
      return CompletableFuture.completedFuture(update);
    };
  }

  private static AsyncNodeAction<AgentState> status(String status, String node) {
    return state -> CompletableFuture.completedFuture(Map.of(STATUS, status, LAST_NODE, node));
  }

  private static AsyncEdgeAction<AgentState> routeFromTool() {
    return state ->
        CompletableFuture.completedFuture(
            "QUOTE_DESIGN".equals(state.<String>value(ROUTE).orElse(""))
                ? EXECUTE_QUOTE_WORKFLOW
                : VALIDATE_BUSINESS_RESULT);
  }

  private static AsyncEdgeAction<AgentState> terminalOrApproval() {
    return state ->
        CompletableFuture.completedFuture(
            switch (state.<String>value(TERMINAL_STATUS).orElse("")) {
              case "FAILED" -> FAILED;
              case "REJECTED" -> REJECTED;
              case "CLARIFICATION_REQUIRED" -> CLARIFICATION_REQUIRED;
              default -> APPROVAL_GATE;
            });
  }

  private static AsyncEdgeAction<AgentState> approvalDecision() {
    return state -> {
      String decision = state.<String>value(DECISION).orElse("");
      String target =
          switch (decision) {
            case "APPROVE" -> COMPOSE_RESPONSE;
            case "REQUEST_CONFIRMATION" -> WAIT_FOR_CONFIRMATION;
            case "REQUEST_CLARIFICATION" -> CLARIFICATION_REQUIRED;
            case "PROVIDE_CLARIFICATION" -> EXTRACT_REQUIRED_SLOTS;
            case "REJECT" -> REJECTED;
            default ->
                state.<Boolean>value(NEEDS_APPROVAL).orElse(false)
                    ? WAIT_FOR_APPROVAL
                    : state.<Boolean>value(NEEDS_CONFIRMATION).orElse(false)
                        ? WAIT_FOR_CONFIRMATION
                        : COMPOSE_RESPONSE;
          };
      return CompletableFuture.completedFuture(target);
    };
  }

  private static Map<String, String> routes() {
    return Map.of(
        WAIT_FOR_APPROVAL, WAIT_FOR_APPROVAL,
        WAIT_FOR_CONFIRMATION, WAIT_FOR_CONFIRMATION,
        CLARIFICATION_REQUIRED, CLARIFICATION_REQUIRED,
        REJECTED, REJECTED,
        FAILED, FAILED,
        EXTRACT_REQUIRED_SLOTS, EXTRACT_REQUIRED_SLOTS,
        COMPOSE_RESPONSE, COMPOSE_RESPONSE,
        APPROVAL_GATE, APPROVAL_GATE);
  }

  private static void validateNodePolicies(NodePolicyRegistry policies) {
    for (String nodeId : workflowNodeIds()) {
      if (!policies.nodeIds().contains(nodeId)) {
        throw new IllegalArgumentException("Missing node policy for node: " + nodeId);
      }
    }
    policies.nodeIds().stream()
        .filter(nodeId -> !workflowNodeIds().contains(nodeId))
        .findFirst()
        .ifPresent(
            nodeId -> {
              throw new IllegalArgumentException("Unknown workflow node policy: " + nodeId);
            });
  }

  private static List<String> workflowNodeIds() {
    return List.of(
        RECEIVE_REQUEST,
        RESOLVE_AUTHENTICATED_CONTEXT,
        INITIALIZE_WORKFLOW,
        NORMALIZE_INPUT,
        DETECT_EXPLICIT_INTENT,
        DECOMPOSE_MULTI_INTENT_REQUEST,
        SEMANTIC_ROUTE_WITH_PGVECTOR,
        CONFIDENCE_GATE,
        EXTRACT_REQUIRED_SLOTS,
        RETRIEVE_CONTEXT_IF_NEEDED,
        EXECUTE_TOOL,
        EXECUTE_QUOTE_WORKFLOW,
        VALIDATE_BUSINESS_RESULT,
        APPROVAL_GATE,
        WAIT_FOR_APPROVAL,
        WAIT_FOR_CONFIRMATION,
        CLARIFICATION_REQUIRED,
        REJECTED,
        FAILED,
        COMPOSE_RESPONSE,
        FINISH);
  }

  private static NodeProfile defaultProfile(String nodeId) {
    NodeModelRole modelRole =
        switch (nodeId) {
          case DETECT_EXPLICIT_INTENT,
              DECOMPOSE_MULTI_INTENT_REQUEST,
              SEMANTIC_ROUTE_WITH_PGVECTOR ->
              NodeModelRole.ROUTER;
          case EXTRACT_REQUIRED_SLOTS -> NodeModelRole.EXTRACTOR;
          case COMPOSE_RESPONSE -> NodeModelRole.ANSWER;
          default -> NodeModelRole.NONE;
        };
    boolean mayInterrupt =
        Set.of(
                VALIDATE_BUSINESS_RESULT,
                WAIT_FOR_APPROVAL,
                WAIT_FOR_CONFIRMATION,
                CLARIFICATION_REQUIRED)
            .contains(nodeId);
    return new NodeProfile(
        nodeId,
        modelRole,
        new NodeToolPolicy(Set.of(), true, false),
        new NodeMemoryPolicy(Set.of(), 0, false),
        new NodeGuardrailPolicy(true, true, true, true, true),
        0,
        Duration.ofSeconds(30),
        mayInterrupt,
        APPROVAL_GATE.equals(nodeId));
  }

  private static WorkflowRequest request(
      AgentState state, AiExecutionContext context, NodeProfile profile) {
    return new WorkflowRequest(
        state.<String>value(MESSAGE).orElse(""), context, Map.copyOf(state.data()), profile);
  }

  private static Map<String, Object> identity(AiExecutionContext context) {
    Map<String, Object> identity = new HashMap<>();
    identity.put(TENANT_ID, context.tenantId().toString());
    identity.put(PRINCIPAL_ID, context.principalId().toString());
    identity.put(CONVERSATION_ID, context.conversationId().toString());
    identity.put(WORKFLOW_ID, context.workflowId().toString());
    return identity;
  }

  static void verifyIdentity(AgentState state, AiExecutionContext context) {
    if (!context.tenantId().toString().equals(state.<String>value(TENANT_ID).orElse(null))
        || !context
            .conversationId()
            .toString()
            .equals(state.<String>value(CONVERSATION_ID).orElse(null))
        || !context.workflowId().toString().equals(state.<String>value(WORKFLOW_ID).orElse(null))) {
      throw new SecurityException(
          "Conversation workflow checkpoint does not match authenticated context");
    }
  }

  static java.util.UUID ownerPrincipalId(AgentState state) {
    String value = state.<String>value(PRINCIPAL_ID).orElse(null);
    if (value == null) {
      throw new SecurityException("Conversation workflow checkpoint has no owner principal");
    }
    try {
      return java.util.UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw new SecurityException(
          "Conversation workflow checkpoint has an invalid owner principal", exception);
    }
  }
}
