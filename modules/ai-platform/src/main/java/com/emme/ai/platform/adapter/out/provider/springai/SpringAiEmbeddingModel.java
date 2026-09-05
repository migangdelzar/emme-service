package com.emme.ai.platform.adapter.out.provider.springai;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.semantic.DistanceMetric;
import com.emme.ai.contracts.semantic.EmbeddingModelVersion;
import com.emme.ai.contracts.semantic.EmbeddingVector;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.stream.IntStream;
import org.springframework.ai.embedding.EmbeddingModel;

/** Thin provider-identified adapter over a Spring AI {@link EmbeddingModel}. */
public final class SpringAiEmbeddingModel implements EmbeddingService {

  private final EmbeddingModel model;
  private final String provider;
  private final EmbeddingModelVersion modelIdentity;

  public SpringAiEmbeddingModel(
      EmbeddingModel model, String provider, String modelVersion, int dimension) {
    this(
        model,
        provider,
        new EmbeddingModelVersion(
            provider, modelVersion, dimension, DistanceMetric.COSINE, "query-v1"));
  }

  public SpringAiEmbeddingModel(
      EmbeddingModel model, String provider, EmbeddingModelVersion modelIdentity) {
    this.model = Objects.requireNonNull(model, "model must not be null");
    this.provider = requireText(provider, "provider");
    this.modelIdentity = Objects.requireNonNull(modelIdentity, "modelIdentity must not be null");
  }

  public String provider() {
    return provider;
  }

  public String modelVersion() {
    return modelIdentity.version();
  }

  public int dimension() {
    return modelIdentity.dimension();
  }

  public EmbeddingVector embed(String text) {
    AiExecutionContextScope.requireCurrent();
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Embedding text must not be blank");
    }
    float[] values = model.embed(text);
    int actualDimension = values == null ? 0 : values.length;
    if (actualDimension != dimension()) {
      throw new IllegalArgumentException(
          "Embedding dimension "
              + actualDimension
              + " does not match configured dimension "
              + dimension());
    }
    return new EmbeddingVector(
        IntStream.range(0, values.length).mapToObj(index -> values[index]).toList(), modelIdentity);
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
