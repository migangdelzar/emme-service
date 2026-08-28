package com.emme.assistant.ai.application.semantic;

import java.util.Objects;

/** Indexed reference text for deterministic intent, tool, or cache matching. */
public record SemanticReference(String key, EmbeddingVector embedding) {

  public SemanticReference {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("Semantic reference key must not be blank");
    }
    Objects.requireNonNull(embedding, "embedding must not be null");
  }
}
