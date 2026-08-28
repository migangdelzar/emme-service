package com.emme.ai.contracts.model;

import java.util.Objects;

/** Model output metadata; business authority remains in application results. */
public record ChatResponse(
    String content, String provider, String modelVersion, int inputTokens, int outputTokens) {

  public ChatResponse {
    content = requireText(content, "content");
    provider = requireText(provider, "provider");
    modelVersion = requireText(modelVersion, "modelVersion");
    if (inputTokens < 0 || outputTokens < 0) {
      throw new IllegalArgumentException("token counts must be non-negative");
    }
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
