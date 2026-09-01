package com.emme.assistant.ai.adapter.out.provider.springai;

import com.emme.ai.contracts.semantic.EmbeddingModelConfiguration;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/** Adapts the configured application embedding chain to Spring AI's vector-store contract. */
public final class SpringAiEmbeddingModelAdapter implements EmbeddingModel {

  private final EmbeddingModelPort embeddings;
  private final EmbeddingModelConfiguration configuration;

  public SpringAiEmbeddingModelAdapter(
      EmbeddingModelPort embeddings, EmbeddingModelConfiguration configuration) {
    this.embeddings = Objects.requireNonNull(embeddings, "embeddings must not be null");
    this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
  }

  @Override
  public EmbeddingResponse call(EmbeddingRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    List<?> instructions = request.getInstructions();
    return new EmbeddingResponse(
        IntStream.range(0, instructions.size())
            .mapToObj(index -> embedding(instructions.get(index), index))
            .toList());
  }

  @Override
  public float[] embed(Document document) {
    Objects.requireNonNull(document, "document must not be null");
    return values(embeddings.embed(document.getText()));
  }

  @Override
  public int dimensions() {
    return configuration.dimension();
  }

  private Embedding embedding(Object instruction, int index) {
    if (instruction instanceof String text) {
      return new Embedding(values(embeddings.embed(text)), index);
    }
    if (instruction instanceof float[] values) {
      return new Embedding(rawValues(values), index);
    }
    throw new IllegalArgumentException("Embedding instructions must contain text or vectors");
  }

  private float[] values(EmbeddingVector embedding) {
    if (!configuration.modelVersion().equals(embedding.modelVersion())) {
      throw new IllegalArgumentException("Embedding model version must match configured model");
    }
    if (embedding.values().size() != configuration.dimension()) {
      throw new IllegalArgumentException("Embedding dimensions must match configured model");
    }
    float[] values = new float[embedding.values().size()];
    for (int index = 0; index < values.length; index++) {
      values[index] = embedding.values().get(index);
    }
    return values;
  }

  private float[] rawValues(float[] values) {
    if (values.length != configuration.dimension()) {
      throw new IllegalArgumentException("Embedding dimensions must match configured model");
    }
    return Arrays.copyOf(values, values.length);
  }
}
