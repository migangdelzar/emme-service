package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Ordered chat-provider failover policy. */
public final class ChatProviderChain implements ChatCompletionPort {

  private final List<Provider> providers;

  public ChatProviderChain(List<Provider> providers) {
    Objects.requireNonNull(providers, "providers must not be null");
    if (providers.isEmpty()) {
      throw new IllegalArgumentException("At least one chat provider is required");
    }
    this.providers = List.copyOf(providers);
  }

  @Override
  public String complete(String conversationContext, String userMessage) {
    ChatProviderUnavailableException lastFailure = null;
    for (Provider provider : providers) {
      try {
        return provider.model().complete(conversationContext, userMessage);
      } catch (ChatProviderUnavailableException unavailable) {
        lastFailure = unavailable;
      }
    }
    String providerNames = providers.stream().map(Provider::key).collect(Collectors.joining(", "));
    throw new ChatProviderUnavailableException(
        "All configured chat providers are unavailable: " + providerNames, lastFailure);
  }

  public record Provider(String key, ChatCompletionPort model) {
    public Provider {
      if (key == null || key.isBlank()) {
        throw new IllegalArgumentException("Chat provider key must not be blank");
      }
      Objects.requireNonNull(model, "model must not be null");
    }
  }
}
