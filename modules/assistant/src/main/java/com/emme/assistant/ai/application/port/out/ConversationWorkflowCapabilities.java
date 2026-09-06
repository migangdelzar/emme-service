package com.emme.assistant.ai.application.port.out;

import com.emme.kernel.context.AiExecutionContext;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    private static final int MAX_PATCH_ENTRIES = 64;
    private static final int MAX_PATCH_DEPTH = 8;
    private static final int MAX_PATCH_TEXT_LENGTH = 4096;

    public WorkflowStep {
      updates = immutableJsonMap(updates, 0);
      if (terminalStatus != null && terminalStatus.isBlank()) {
        throw new IllegalArgumentException("terminalStatus must not be blank");
      }
    }

    public static WorkflowStep empty() {
      return new WorkflowStep(Map.of(), false, false, null);
    }

    private static Map<String, Object> immutableJsonMap(Map<String, Object> values, int depth) {
      Objects.requireNonNull(values, "updates must not be null");
      if (values.size() > MAX_PATCH_ENTRIES) {
        throw new IllegalArgumentException("State patch must contain at most 64 entries");
      }
      Map<String, Object> copy = new LinkedHashMap<>();
      values.forEach(
          (key, value) -> {
            if (key == null || key.isBlank()) {
              throw new IllegalArgumentException("State patch keys must not be blank");
            }
            copy.put(key, immutableJsonValue(value, depth + 1));
          });
      return Map.copyOf(copy);
    }

    private static Object immutableJsonValue(Object value, int depth) {
      if (value == null) {
        throw new IllegalArgumentException("State patch values must be JSON-safe");
      }
      if (depth > MAX_PATCH_DEPTH) {
        throw new IllegalArgumentException("State patch nesting exceeds 8 levels");
      }
      if (value instanceof String text) {
        if (text.length() > MAX_PATCH_TEXT_LENGTH) {
          throw new IllegalArgumentException("State patch text exceeds 4096 characters");
        }
        return text;
      }
      if (value instanceof Boolean || value instanceof BigInteger || value instanceof BigDecimal) {
        return value;
      }
      if (value instanceof Byte
          || value instanceof Short
          || value instanceof Integer
          || value instanceof Long) {
        return value;
      }
      if (value instanceof Float floatValue) {
        if (!Float.isFinite(floatValue)) {
          throw new IllegalArgumentException("State patch values must be JSON-safe");
        }
        return value;
      }
      if (value instanceof Double doubleValue) {
        if (!Double.isFinite(doubleValue)) {
          throw new IllegalArgumentException("State patch values must be JSON-safe");
        }
        return value;
      }
      if (value instanceof Map<?, ?> map) {
        if (map.size() > MAX_PATCH_ENTRIES) {
          throw new IllegalArgumentException("State patch must contain at most 64 entries");
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        map.forEach(
            (key, nestedValue) -> {
              if (!(key instanceof String textKey) || textKey.isBlank()) {
                throw new IllegalArgumentException("State patch keys must not be blank");
              }
              copy.put(textKey, immutableJsonValue(nestedValue, depth + 1));
            });
        return Map.copyOf(copy);
      }
      if (value instanceof List<?> list) {
        if (list.size() > MAX_PATCH_ENTRIES) {
          throw new IllegalArgumentException("State patch lists must contain at most 64 entries");
        }
        List<Object> copy = new ArrayList<>(list.size());
        list.forEach(item -> copy.add(immutableJsonValue(item, depth + 1)));
        return List.copyOf(copy);
      }
      throw new IllegalArgumentException("State patch values must be JSON-safe");
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
