package com.emme.assistant.ai.application.semantic;

import java.util.List;
import java.util.Objects;

/** Immutable embedding and the model version that produced it. */
public record EmbeddingVector(String modelVersion, List<Float> values) {

  public EmbeddingVector {
    if (modelVersion == null || modelVersion.isBlank()) {
      throw new IllegalArgumentException("Embedding model version must not be blank");
    }
    Objects.requireNonNull(values, "Embedding values must not be null");
    if (values.isEmpty()) {
      throw new IllegalArgumentException("Embedding vector must not be empty");
    }
    if (values.stream().anyMatch(value -> value == null || !Float.isFinite(value))) {
      throw new IllegalArgumentException("Embedding vector values must be finite");
    }
    values = List.copyOf(values);
  }
}
