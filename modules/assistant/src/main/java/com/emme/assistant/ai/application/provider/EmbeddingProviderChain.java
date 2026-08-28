package com.emme.assistant.ai.application.provider;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Ordered embedding-provider failover policy.
 *
 * <p>Fallback is deliberately limited to {@link EmbeddingProviderUnavailableException}. Invalid
 * vectors and other application failures propagate immediately instead of being hidden by a
 * different provider.
 */
public final class EmbeddingProviderChain implements EmbeddingModelPort {

  private final List<Provider> providers;

  public EmbeddingProviderChain(List<Provider> providers) {
    Objects.requireNonNull(providers, "providers must not be null");
    if (providers.isEmpty()) {
      throw new IllegalArgumentException("At least one embedding provider is required");
    }
    this.providers = List.copyOf(providers);
  }

  @Override
  public EmbeddingVector embed(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Embedding text must not be blank");
    }

    EmbeddingProviderUnavailableException lastFailure = null;
    for (Provider provider : providers) {
      try {
        return provider.model().embed(text);
      } catch (EmbeddingProviderUnavailableException unavailable) {
        lastFailure = unavailable;
      }
    }

    String providerNames = providers.stream().map(Provider::key).collect(Collectors.joining(", "));
    throw new EmbeddingProviderUnavailableException(
        "All configured embedding providers are unavailable: " + providerNames, lastFailure);
  }

  /** A named provider in the configured failover order. */
  public record Provider(String key, EmbeddingModelPort model) {
    public Provider {
      if (key == null || key.isBlank()) {
        throw new IllegalArgumentException("Embedding provider key must not be blank");
      }
      Objects.requireNonNull(model, "model must not be null");
    }
  }
}
