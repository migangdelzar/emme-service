package com.emme.ai.contracts.semantic;

/** Immutable identity of the embedding space shared by providers, indexes, and caches. */
public record EmbeddingModelConfiguration(String modelName, String modelVersion, int dimension) {

  public EmbeddingModelConfiguration {
    if (modelName == null || modelName.isBlank()) {
      throw new IllegalArgumentException("modelName must not be blank");
    }
    if (modelVersion == null || modelVersion.isBlank()) {
      throw new IllegalArgumentException("modelVersion must not be blank");
    }
    if (dimension <= 0) {
      throw new IllegalArgumentException("dimension must be positive");
    }
  }

  /** Collision-resistant namespace component for durable and hot semantic keys. */
  public String namespace() {
    return modelName + "@" + modelVersion + "#" + dimension;
  }

  public EmbeddingModelConfiguration withDimension(int newDimension) {
    return new EmbeddingModelConfiguration(modelName, modelVersion, newDimension);
  }

  public EmbeddingModelConfiguration requireSame(EmbeddingModelConfiguration other) {
    if (!equals(other)) {
      throw new IllegalArgumentException("Embedding model configuration does not match: " + other);
    }
    return this;
  }
}
