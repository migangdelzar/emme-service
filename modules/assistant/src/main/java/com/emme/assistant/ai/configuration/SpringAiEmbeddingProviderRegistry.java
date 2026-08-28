package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.provider.springai.SpringAiEmbeddingAdapter;
import com.emme.assistant.ai.application.service.EmbeddingProviderChain;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.ai.embedding.EmbeddingModel;

/** Builds the ordered application provider list from explicitly named Spring AI beans. */
public final class SpringAiEmbeddingProviderRegistry {

  private final List<EmbeddingProviderChain.Provider> providers;

  public SpringAiEmbeddingProviderRegistry(
      Map<String, EmbeddingModel> embeddingModels,
      SpringAiEmbeddingProperties properties,
      int dimension) {
    Objects.requireNonNull(embeddingModels, "embeddingModels must not be null");
    Objects.requireNonNull(properties, "properties must not be null");

    Set<String> providerKeys = new HashSet<>();
    this.providers =
        properties.providers().stream()
            .map(
                configured -> {
                  EmbeddingModel model = embeddingModels.get(configured.beanName());
                  if (model == null) {
                    throw new IllegalStateException(
                        "No Spring AI embedding model bean configured for provider '"
                            + configured.key()
                            + "'");
                  }
                  if (!providerKeys.add(configured.key())) {
                    throw new IllegalArgumentException(
                        "Duplicate Spring AI embedding provider key: " + configured.key());
                  }
                  return new EmbeddingProviderChain.Provider(
                      configured.key(),
                      new SpringAiEmbeddingAdapter(model, configured.modelVersion(), dimension));
                })
            .toList();
    if (providers.isEmpty()) {
      throw new IllegalArgumentException(
          "At least one Spring AI embedding provider must be configured");
    }
  }

  public List<EmbeddingProviderChain.Provider> providers() {
    return providers;
  }
}
