package com.emme.ai.contracts.model;

import com.emme.kernel.context.AiExecutionContext;
import java.util.List;
import java.util.Objects;

/** Policy-facing chat completion capability with trusted context and provider metadata. */
@FunctionalInterface
public interface AiChatCompletion {

  ChatResponse complete(Request request);

  /** Prepared chat input plus backend-owned execution and provider-selection policy. */
  record Request(
      String conversationContext,
      String userMessage,
      AiExecutionContext executionContext,
      ProviderPolicy providerPolicy) {

    public Request {
      conversationContext = requireText(conversationContext, "conversationContext");
      userMessage = requireText(userMessage, "userMessage");
      executionContext =
          Objects.requireNonNull(executionContext, "executionContext must not be null");
      providerPolicy = Objects.requireNonNull(providerPolicy, "providerPolicy must not be null");
    }
  }

  /** Ordered provider admission and fallback policy selected by the application. */
  record ProviderPolicy(List<String> admittedProviders, boolean fallbackAllowed) {

    public ProviderPolicy {
      Objects.requireNonNull(admittedProviders, "admittedProviders must not be null");
      if (admittedProviders.isEmpty()
          || admittedProviders.stream()
              .anyMatch(provider -> provider == null || provider.isBlank())) {
        throw new IllegalArgumentException("admittedProviders must contain nonblank values");
      }
      admittedProviders = List.copyOf(admittedProviders);
    }
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
