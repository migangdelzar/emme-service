package com.emme.ai.contracts.guardrail;

import java.util.Objects;

/** Channel delivery facts presented to the delivery guard. */
public record DeliveryRequest(
    String channel, String content, int maximumCharacters, boolean streaming) {

  public DeliveryRequest {
    requireText(channel, "channel");
    Objects.requireNonNull(content, "content must not be null");
    if (maximumCharacters <= 0) {
      throw new IllegalArgumentException("maximumCharacters must be positive");
    }
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
  }
}
