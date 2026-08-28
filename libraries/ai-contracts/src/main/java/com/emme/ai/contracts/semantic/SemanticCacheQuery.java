package com.emme.ai.contracts.semantic;

import java.util.Objects;

/** Query key for a scoped semantic response lookup. */
public record SemanticCacheQuery(
    String audienceScope, EmbeddingVector embedding, String promptVersion, String policyVersion) {

  public SemanticCacheQuery {
    audienceScope = requireText(audienceScope, "audienceScope");
    embedding = Objects.requireNonNull(embedding, "embedding must not be null");
    promptVersion = requireText(promptVersion, "promptVersion");
    policyVersion = requireText(policyVersion, "policyVersion");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
