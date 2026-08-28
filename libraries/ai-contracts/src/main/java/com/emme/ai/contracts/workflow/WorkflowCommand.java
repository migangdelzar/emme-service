package com.emme.ai.contracts.workflow;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Idempotent workflow input; tenant and authorization remain in execution context. */
public record WorkflowCommand(
    UUID workflowId, String workflowType, Map<String, Object> input, String idempotencyKey) {

  public WorkflowCommand {
    workflowType = requireText(workflowType, "workflowType");
    Objects.requireNonNull(input, "input must not be null");
    input = Map.copyOf(input);
    idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
