package com.emme.ai.contracts.semantic;

import java.util.Objects;

/** Immutable identity of an embedding space and its query instructions. */
public record EmbeddingModelVersion(
    String modelName,
    String version,
    int dimension,
    DistanceMetric distanceMetric,
    String queryInstructionVersion) {

  public EmbeddingModelVersion {
    modelName = requireText(modelName, "modelName");
    version = requireText(version, "version");
    if (dimension <= 0) {
      throw new IllegalArgumentException("dimension must be positive");
    }
    distanceMetric = Objects.requireNonNull(distanceMetric, "distanceMetric must not be null");
    queryInstructionVersion = requireText(queryInstructionVersion, "queryInstructionVersion");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
