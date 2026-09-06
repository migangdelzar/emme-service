package com.emme.ai.contracts.guardrail;

import java.util.Objects;

/** Bounded input facts presented to the input guard. */
public record InputRequest(
    String message, long contentBytes, int attachmentCount, String idempotencyKey) {

  public InputRequest {
    Objects.requireNonNull(message, "message must not be null");
    requireNonNegative(contentBytes, "contentBytes");
    requireNonNegative(attachmentCount, "attachmentCount");
    requireText(idempotencyKey, "idempotencyKey");
  }

  private static void requireNonNegative(long value, String field) {
    if (value < 0) throw new IllegalArgumentException(field + " must not be negative");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
  }
}
