package com.emme.assistant.ai.application.provider;

import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.IdentifiedChatCompletionPort;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Ordered chat-provider failover policy. */
public final class ChatProviderChain implements IdentifiedChatCompletionPort {

  private final List<Provider> providers;
  private final Optional<ModelExecutionScheduler> scheduler;
  private final Duration admissionTimeout;

  public ChatProviderChain(List<Provider> providers) {
    this(providers, Optional.empty(), Duration.ZERO);
  }

  public ChatProviderChain(
      List<Provider> providers, ModelExecutionScheduler scheduler, Duration admissionTimeout) {
    this(
        providers,
        Optional.of(Objects.requireNonNull(scheduler, "scheduler must not be null")),
        admissionTimeout);
  }

  private ChatProviderChain(
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
  public String complete(String conversationContext, String userMessage) {
    return completeWithIdentity(conversationContext, userMessage).content();
  }

  @Override
  public IdentifiedChatCompletionPort.ChatCompletionResult completeWithIdentity(
      String conversationContext, String userMessage) {
    ChatProviderUnavailableException lastFailure = null;
    for (Provider provider : providers) {
      try {
        return new IdentifiedChatCompletionPort.ChatCompletionResult(
            execute(provider, conversationContext, userMessage), provider.key(), provider.modelVersion());
      } catch (ChatProviderUnavailableException unavailable) {
        lastFailure = unavailable;
      }
    }
    String providerNames = providers.stream().map(Provider::key).collect(Collectors.joining(", "));
    throw new ChatProviderUnavailableException(
        "All configured chat providers are unavailable: " + providerNames, lastFailure);
  }

  private String execute(Provider provider, String conversationContext, String userMessage) {
    if (scheduler.isEmpty()) {
      return provider.model().complete(conversationContext, userMessage);
    }
    return scheduler
        .orElseThrow()
        .execute(
            ModelCapability.GENERATION,
            AiExecutionContextScope.requireCurrent(),
            admissionTimeout,
            () -> provider.model().complete(conversationContext, userMessage));
  }

  public record Provider(String key, ChatCompletionPort model, String modelVersion) {
    public Provider(String key, ChatCompletionPort model) {
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
