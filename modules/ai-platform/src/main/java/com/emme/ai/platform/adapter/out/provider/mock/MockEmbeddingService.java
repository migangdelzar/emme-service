package com.emme.ai.platform.adapter.out.provider.mock;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.semantic.DistanceMetric;
import com.emme.ai.contracts.semantic.EmbeddingModelVersion;
import com.emme.ai.contracts.semantic.EmbeddingVector;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Deterministic provider-neutral embedding capability for local and test execution. */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public final class MockEmbeddingService implements EmbeddingService {

  private final AiProviderProperties properties;
  private final EmbeddingModelVersion model;

  public MockEmbeddingService(AiProviderProperties properties) {
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
    this.model =
        new EmbeddingModelVersion(
            properties.embedding().model(),
            properties.embeddingModelVersion(),
            properties.embeddingDimension(),
            DistanceMetric.COSINE,
            "query-v1");
  }

  @Override
  public EmbeddingVector embed(String text) {
    AiExecutionContextScope.requireCurrent();
    int dimension = properties.embeddingDimension();
    float[] values = new float[dimension];
    for (String token : text.toLowerCase(Locale.ROOT).split("\\W+")) {
      if (!token.isBlank()) values[Math.floorMod(token.hashCode(), dimension)] += 1.0f;
    }
    double norm =
        Math.sqrt(
            IntStream.range(0, dimension).mapToDouble(i -> (double) values[i] * values[i]).sum());
    List<Float> vector = new ArrayList<>(dimension);
    for (float value : values) vector.add(norm == 0 ? 0.0f : (float) (value / norm));
    return new EmbeddingVector(vector, model);
  }
}
