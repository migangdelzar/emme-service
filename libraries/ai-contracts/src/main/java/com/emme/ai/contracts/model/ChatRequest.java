package com.emme.ai.contracts.model;

import java.util.Objects;

/** Prepared, non-authoritative model input for a chat completion. */
public record ChatRequest(String conversationContext, String userMessage) {

  public ChatRequest {
    conversationContext = requireText(conversationContext, "conversationContext");
    userMessage = requireText(userMessage, "userMessage");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
