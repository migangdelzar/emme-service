package com.emme.ai.contracts.semantic;

import java.util.List;
import java.util.Objects;

/** Immutable embedding vector coupled to the exact model space that produced it. */
public record EmbeddingVector(List<Float> values, EmbeddingModelVersion model) {

  public EmbeddingVector {
    Objects.requireNonNull(values, "values must not be null");
    if (values.isEmpty()) {
      throw new IllegalArgumentException("values must not be empty");
    }
    if (values.size() != model.dimension()) {
      throw new IllegalArgumentException("values dimension must match model dimension");
    }
    if (values.stream().anyMatch(value -> value == null || !Float.isFinite(value))) {
      throw new IllegalArgumentException("values must contain only finite numbers");
    }
    values = List.copyOf(values);
  }
}
