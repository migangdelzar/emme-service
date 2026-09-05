package com.emme.ai.platform.adapter.out.capability;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.contracts.semantic.DistanceMetric;
import com.emme.ai.contracts.semantic.EmbeddingModelConfiguration;
import com.emme.ai.contracts.semantic.EmbeddingModelVersion;
import com.emme.ai.contracts.semantic.EmbeddingVector;
import com.emme.ai.platform.configuration.AiProviderProperties;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Adapts the provider-neutral embedding capability to the configured model provider. */
@Service
public class AiEmbeddingAdapter implements EmbeddingService {

  private final AiModelProvider provider;
  private final EmbeddingModelConfiguration configuration;

  @Autowired
  public AiEmbeddingAdapter(AiModelProvider provider, AiProviderProperties properties) {
    this(
        provider,
        Objects.requireNonNull(properties, "properties must not be null")
            .embeddingModelConfiguration());
  }

  public AiEmbeddingAdapter(AiModelProvider provider, EmbeddingModelConfiguration configuration) {
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
  }

  @Override
  public EmbeddingVector embed(String text) {
    return new EmbeddingVector(
        provider.embed(text),
        new EmbeddingModelVersion(
            configuration.modelName(),
            configuration.modelVersion(),
            configuration.dimension(),
            DistanceMetric.COSINE,
            "query-v1"));
  }
}
