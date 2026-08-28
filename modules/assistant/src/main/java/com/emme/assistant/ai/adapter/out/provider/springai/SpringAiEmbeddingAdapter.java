package com.emme.assistant.ai.adapter.out.provider.springai;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.service.EmbeddingVector;
import java.util.Objects;
import java.util.stream.IntStream;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * Infrastructure adapter that translates Spring AI's embedding API into the application port.
 *
 * <p>The concrete Spring AI provider is supplied from the composition root. This keeps provider
 * selection outside the application and domain layers and allows the same port to wrap Ollama,
 * OpenAI, or another Spring AI embedding implementation.
 */
public final class SpringAiEmbeddingAdapter implements EmbeddingModelPort {

  private final EmbeddingModel model;
  private final String modelVersion;
  private final int dimension;

  public SpringAiEmbeddingAdapter(EmbeddingModel model, String modelVersion, int dimension) {
    this.model = Objects.requireNonNull(model, "model must not be null");
    if (modelVersion == null || modelVersion.isBlank()) {
      throw new IllegalArgumentException("Embedding model version must not be blank");
    }
    if (dimension <= 0) {
      throw new IllegalArgumentException("Embedding dimension must be positive");
    }
    this.modelVersion = modelVersion;
    this.dimension = dimension;
  }

  @Override
  public EmbeddingVector embed(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Embedding text must not be blank");
    }

    float[] values;
    try {
      values = model.embed(text);
    } catch (RuntimeException failure) {
      throw new EmbeddingProviderUnavailableException(
          "Spring AI embedding provider failed: " + failure.getMessage(), failure);
    }
    int actualDimension = values == null ? 0 : values.length;
    if (actualDimension != dimension) {
      throw new IllegalStateException(
          "Embedding dimension "
              + actualDimension
              + " does not match configured dimension "
              + dimension);
    }

    return new EmbeddingVector(
        modelVersion, IntStream.range(0, values.length).mapToObj(index -> values[index]).toList());
  }
}
