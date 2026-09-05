package com.emme.assistant.ai.application.provider;

import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.contracts.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Ordered embedding-model selection policy with unavailable-provider failover. */
public final class EmbeddingModelSelector implements EmbeddingModelPort {

  private final List<Provider> providers;
  private final Optional<ModelExecutionScheduler> scheduler;
  private final Duration admissionTimeout;

  public EmbeddingModelSelector(List<Provider> providers) {
    this(providers, Optional.empty(), Duration.ZERO);
  }

  public EmbeddingModelSelector(
      List<Provider> providers, ModelExecutionScheduler scheduler, Duration admissionTimeout) {
    this(
        providers,
        Optional.of(Objects.requireNonNull(scheduler, "scheduler must not be null")),
        admissionTimeout);
  }

  private EmbeddingModelSelector(
      List<Provider> providers,
      Optional<ModelExecutionScheduler> scheduler,
      Duration admissionTimeout) {
    Objects.requireNonNull(providers, "providers must not be null");
    if (providers.isEmpty()) {
      throw new IllegalArgumentException("At least one embedding provider is required");
    }
    this.providers = List.copyOf(providers);
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
    this.admissionTimeout =
        Objects.requireNonNull(admissionTimeout, "admissionTimeout must not be null");
    if (scheduler.isPresent() && (admissionTimeout.isZero() || admissionTimeout.isNegative())) {
      throw new IllegalArgumentException("admissionTimeout must be positive");
    }
  }

  @Override
  public EmbeddingVector embed(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Embedding text must not be blank");
    }

    EmbeddingProviderUnavailableException lastFailure = null;
    for (Provider provider : providers) {
      try {
        return execute(provider, text);
      } catch (EmbeddingProviderUnavailableException unavailable) {
        lastFailure = unavailable;
      }
    }

    String providerNames = providers.stream().map(Provider::key).collect(Collectors.joining(", "));
    throw new EmbeddingProviderUnavailableException(
        "All configured embedding providers are unavailable: " + providerNames, lastFailure);
  }

  private EmbeddingVector execute(Provider provider, String text) {
    if (scheduler.isEmpty()) {
      return provider.model().embed(text);
    }
    return scheduler
        .orElseThrow()
        .execute(
            ModelCapability.EMBEDDING,
            AiExecutionContextScope.requireCurrent(),
            admissionTimeout,
            () -> provider.model().embed(text));
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
