package com.emme.ai.contracts.tool;

import java.util.Map;
import java.util.Objects;

/** Result of an application-layer or authenticated external tool invocation. */
public record ToolResult(String toolKey, Map<String, Object> payload, boolean authoritative) {

  public ToolResult {
    toolKey = requireText(toolKey, "toolKey");
    Objects.requireNonNull(payload, "payload must not be null");
    payload = Map.copyOf(payload);
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
