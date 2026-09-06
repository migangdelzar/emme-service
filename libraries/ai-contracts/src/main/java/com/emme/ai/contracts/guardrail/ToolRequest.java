package com.emme.ai.contracts.guardrail;

import java.util.Map;
import java.util.Objects;

/** Typed tool invocation facts presented to the tool guard. */
public record ToolRequest(
    String toolKey,
    Map<String, String> arguments,
    boolean mutating,
    boolean confirmed,
    String idempotencyKey) {

  public ToolRequest {
    requireText(toolKey, "toolKey");
    Objects.requireNonNull(arguments, "arguments must not be null");
    if (arguments.entrySet().stream()
        .anyMatch(entry -> entry.getKey() == null || entry.getValue() == null)) {
      throw new IllegalArgumentException("arguments must not contain null keys or values");
    }
    arguments = Map.copyOf(arguments);
    requireText(idempotencyKey, "idempotencyKey");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
  }
}
