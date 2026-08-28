package com.emme.ai.contracts.tool;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Model- or user-supplied business arguments kept separate from trusted execution context. */
public record ToolExecutionRequest(
    String toolKey,
    Map<String, Object> arguments,
    boolean confirmationProvided,
    String idempotencyKey) {

  private static final Set<String> TRUSTED_ARGUMENT_NAMES =
      Set.of("tenantid", "userid", "principalid", "roles", "permissions", "workflowid");

  public ToolExecutionRequest {
    toolKey = requireText(toolKey, "toolKey");
    Objects.requireNonNull(arguments, "arguments must not be null");
    if (arguments.entrySet().stream()
        .anyMatch(entry -> entry.getKey() == null || entry.getKey().isBlank())) {
      throw new IllegalArgumentException("arguments must not contain blank keys");
    }
    if (arguments.keySet().stream()
        .map(key -> key.replace("_", "").replace("-", "").toLowerCase())
        .anyMatch(TRUSTED_ARGUMENT_NAMES::contains)) {
      throw new IllegalArgumentException("arguments must not contain trusted security fields");
    }
    arguments = Map.copyOf(arguments);
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
