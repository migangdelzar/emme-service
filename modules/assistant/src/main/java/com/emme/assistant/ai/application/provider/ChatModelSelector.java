package com.emme.assistant.ai.application.provider;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Ordered chat-model selection policy with unavailable-provider failover. */
public final class ChatModelSelector implements AiChatCompletion {

  private final List<Provider> providers;
  private final Optional<ModelExecutionScheduler> scheduler;
  private final Duration admissionTimeout;

  public ChatModelSelector(List<Provider> providers) {
    this(providers, Optional.empty(), Duration.ZERO);
  }

  public ChatModelSelector(
      List<Provider> providers, ModelExecutionScheduler scheduler, Duration admissionTimeout) {
    this(
        providers,
        Optional.of(Objects.requireNonNull(scheduler, "scheduler must not be null")),
        admissionTimeout);
  }

  private ChatModelSelector(
      List<Provider> providers,
      Optional<ModelExecutionScheduler> scheduler,
      Duration admissionTimeout) {
    Objects.requireNonNull(providers, "providers must not be null");
    if (providers.isEmpty()) {
      throw new IllegalArgumentException("At least one chat provider is required");
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
  public ChatResponse complete(AiChatCompletion.Request request) {
    Objects.requireNonNull(request, "request must not be null");
    if (!AiExecutionContextScope.requireCurrent().equals(request.executionContext())) {
      throw new IllegalArgumentException(
          "chat request context must match the bound AI execution context");
    }
    List<Provider> admittedProviders = admittedProviders(request.providerPolicy());
    return complete(admittedProviders, request, request.providerPolicy().fallbackAllowed());
  }

  private ChatResponse complete(
      List<Provider> candidates, AiChatCompletion.Request request, boolean fallbackAllowed) {
    ChatProviderUnavailableException lastFailure = null;
    List<Provider> attempts = fallbackAllowed ? candidates : List.of(candidates.get(0));
    for (Provider provider : attempts) {
      try {
        ChatResponse response = execute(provider, request);
        return new ChatResponse(
            response.content(),
            provider.key(),
            provider.modelVersion(),
            response.inputTokens(),
            response.outputTokens());
      } catch (ChatProviderUnavailableException unavailable) {
        lastFailure = unavailable;
      }
    }
    String providerNames = attempts.stream().map(Provider::key).collect(Collectors.joining(", "));
    throw new ChatProviderUnavailableException(
        "All configured chat providers are unavailable: " + providerNames, lastFailure);
  }

  private List<Provider> admittedProviders(AiChatCompletion.ProviderPolicy policy) {
    List<Provider> admitted =
        policy.admittedProviders().stream()
            .flatMap(
                key -> providers.stream().filter(provider -> provider.key().equals(key)).limit(1))
            .toList();
    if (admitted.isEmpty()) {
      throw new IllegalArgumentException("No configured chat provider is admitted by the request");
    }
    return admitted;
  }

  private ChatResponse execute(Provider provider, AiChatCompletion.Request request) {
    if (scheduler.isEmpty()) {
      return provider.model().complete(request);
    }
    return scheduler
        .orElseThrow()
        .execute(
            ModelCapability.GENERATION,
            request.executionContext(),
            admissionTimeout,
            () -> provider.model().complete(request));
  }

  public record Provider(String key, AiChatCompletion model, String modelVersion) {
    public Provider(String key, AiChatCompletion model) {
      this(key, model, "unknown-model");
    }

    public Provider {
      if (key == null || key.isBlank()) {
        throw new IllegalArgumentException("Chat provider key must not be blank");
      }
      Objects.requireNonNull(model, "model must not be null");
      if (modelVersion == null || modelVersion.isBlank()) {
        throw new IllegalArgumentException("Chat provider model version must not be blank");
      }
    }
  }
}
