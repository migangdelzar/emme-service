package com.emme.ai.contracts.guardrail;

import java.util.Objects;

/** Model output facts presented to the output guard. */
public record OutputRequest(
    String channel, String content, boolean structured, boolean containsBusinessClaim) {

  public OutputRequest {
    requireText(channel, "channel");
    Objects.requireNonNull(content, "content must not be null");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
  }
}
