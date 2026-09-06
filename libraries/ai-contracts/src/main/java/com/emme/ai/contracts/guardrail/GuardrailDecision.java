package com.emme.ai.contracts.guardrail;

import java.util.Map;
import java.util.Objects;

/** Safe, bounded result of one guardrail evaluation. */
public record GuardrailDecision(
    GuardrailAction action, String code, Map<String, String> safeAttributes) {

  private static final int MAX_ATTRIBUTES = 16;
  private static final int MAX_KEY_LENGTH = 64;
  private static final int MAX_VALUE_LENGTH = 256;

  public GuardrailDecision {
    Objects.requireNonNull(action, "action must not be null");
    requireText(code, "code");
    Objects.requireNonNull(safeAttributes, "safeAttributes must not be null");
    if (safeAttributes.size() > MAX_ATTRIBUTES) {
      throw new IllegalArgumentException("safeAttributes must contain at most 16 entries");
    }
    safeAttributes.forEach(
        (key, value) -> {
          requireBoundedText(key, "safe attribute key", MAX_KEY_LENGTH);
          requireBoundedText(value, "safe attribute value", MAX_VALUE_LENGTH);
        });
    safeAttributes = Map.copyOf(safeAttributes);
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }

  private static void requireBoundedText(String value, String field, int maximumLength) {
    requireText(value, field);
    if (value.length() > maximumLength) {
      throw new IllegalArgumentException(
          field + " must not exceed " + maximumLength + " characters");
    }
  }
}
