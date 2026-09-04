package com.emme.assistant.ai.application.port.out;

import com.emme.kernel.context.AiExecutionContext;
import java.util.Map;
import java.util.Objects;

/**
 * Typed application capability boundary used by the generic conversation graph.
 *
 * <p>Every capability receives only trusted backend context and immutable graph input. Implementors
 * delegate to application use cases; graph nodes never access repositories or domain rules.
 */
public record ConversationWorkflowCapabilities(
    IntentDetectionPort intentDetection,
    MultiIntentDecompositionPort decomposition,
    SemanticRoutingPort semanticRouting,
    RequiredSlotExtractionPort slotExtraction,
    ContextRetrievalPort retrieval,
    ToolExecutionPort toolExecution,
    BusinessResultValidationPort businessValidation,
    ResponseCompositionPort responseComposition,
    QuoteWorkflowCapability quoteWorkflow) {

  public ConversationWorkflowCapabilities {
    Objects.requireNonNull(intentDetection, "intentDetection must not be null");
    Objects.requireNonNull(decomposition, "decomposition must not be null");
    Objects.requireNonNull(semanticRouting, "semanticRouting must not be null");
    Objects.requireNonNull(slotExtraction, "slotExtraction must not be null");
    Objects.requireNonNull(retrieval, "retrieval must not be null");
    Objects.requireNonNull(toolExecution, "toolExecution must not be null");
    Objects.requireNonNull(businessValidation, "businessValidation must not be null");
    Objects.requireNonNull(responseComposition, "responseComposition must not be null");
    Objects.requireNonNull(quoteWorkflow, "quoteWorkflow must not be null");
  }

  public ConversationWorkflowCapabilities withQuoteWorkflow(
      QuoteWorkflowCapability quoteCapability) {
    return new ConversationWorkflowCapabilities(
        intentDetection,
        decomposition,
        semanticRouting,
        slotExtraction,
        retrieval,
        toolExecution,
        businessValidation,
        responseComposition,
        quoteCapability);
  }

  public record WorkflowRequest(
      String message, AiExecutionContext context, Map<String, Object> state) {
    public WorkflowRequest {
      Objects.requireNonNull(message, "message must not be null");
      Objects.requireNonNull(context, "context must not be null");
      state = Map.copyOf(Objects.requireNonNull(state, "state must not be null"));
    }
  }

  /** A capability update; terminalStatus is null when the graph should continue. */
  public record WorkflowStep(
      Map<String, Object> updates,
      boolean needsApproval,
      boolean needsConfirmation,
      String terminalStatus) {
    public WorkflowStep {
      updates = Map.copyOf(Objects.requireNonNull(updates, "updates must not be null"));
      if (terminalStatus != null && terminalStatus.isBlank()) {
        throw new IllegalArgumentException("terminalStatus must not be blank");
      }
    }

    public static WorkflowStep empty() {
      return new WorkflowStep(Map.of(), false, false, null);
    }
  }

  @FunctionalInterface
  public interface IntentDetectionPort {
    WorkflowStep detect(WorkflowRequest request);
  }

  @FunctionalInterface
  public interface MultiIntentDecompositionPort {
    WorkflowStep decompose(WorkflowRequest request);
  }

  @FunctionalInterface
  public interface SemanticRoutingPort {
    WorkflowStep route(WorkflowRequest request);
  }

  @FunctionalInterface
  public interface RequiredSlotExtractionPort {
    WorkflowStep extract(WorkflowRequest request);
  }

  @FunctionalInterface
  public interface ContextRetrievalPort {
    WorkflowStep retrieve(WorkflowRequest request);
  }

  @FunctionalInterface
  public interface ToolExecutionPort {
    WorkflowStep execute(WorkflowRequest request);
  }

  @FunctionalInterface
  public interface BusinessResultValidationPort {
    WorkflowStep validate(WorkflowRequest request);
  }

  @FunctionalInterface
  public interface ResponseCompositionPort {
    WorkflowStep compose(WorkflowRequest request);
  }

  /** Capability adapter around the existing quote workflow graph. */
  @FunctionalInterface
  public interface QuoteWorkflowCapability {
    WorkflowStep execute(WorkflowRequest request);
  }
}
