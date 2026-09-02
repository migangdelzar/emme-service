package com.emme.ai.platform.adapter.out.provider.springai;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.springframework.ai.embedding.EmbeddingModel;

/** Thin provider-identified adapter over a Spring AI {@link EmbeddingModel}. */
public final class SpringAiEmbeddingModel {

  private final EmbeddingModel model;
  private final String provider;
  private final String modelVersion;
  private final int dimension;

  public SpringAiEmbeddingModel(
      EmbeddingModel model, String provider, String modelVersion, int dimension) {
    this.model = Objects.requireNonNull(model, "model must not be null");
    this.provider = requireText(provider, "provider");
    this.modelVersion = requireText(modelVersion, "modelVersion");
    if (dimension <= 0) {
      throw new IllegalArgumentException("dimension must be positive");
    }
    this.dimension = dimension;
  }

  public String provider() {
    return provider;
  }

  public String modelVersion() {
    return modelVersion;
  }

  public int dimension() {
    return dimension;
  }

  public List<Float> embed(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Embedding text must not be blank");
    }
    float[] values = model.embed(text);
    int actualDimension = values == null ? 0 : values.length;
    if (actualDimension != dimension) {
      throw new IllegalArgumentException(
          "Embedding dimension "
              + actualDimension
              + " does not match configured dimension "
              + dimension);
    }
    return IntStream.range(0, values.length).mapToObj(index -> values[index]).toList();
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
