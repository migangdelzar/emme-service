package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatModel;
import com.emme.assistant.ai.application.provider.ChatModelSelector;
import com.emme.assistant.ai.application.provider.ChatProviderFailurePolicy;
import com.emme.assistant.ai.application.provider.TracingAiChatCompletion;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallbackProvider;

/** Builds the ordered provider-neutral chat model candidates from named Spring AI clients. */
public final class SpringAiChatProviderRegistry {

  private final List<ChatModelSelector.Provider> providers;

  public SpringAiChatProviderRegistry(
      Map<String, ChatClient> clients, SpringAiChatProperties properties) {
    this(clients, properties, List.of());
  }

  public SpringAiChatProviderRegistry(
      Map<String, ChatClient> clients,
      SpringAiChatProperties properties,
      List<? extends Advisor> advisors) {
    this(clients, properties, advisors, NoopAiTraceRecorder.INSTANCE);
  }

  public SpringAiChatProviderRegistry(
      Map<String, ChatClient> clients,
      SpringAiChatProperties properties,
      List<? extends Advisor> advisors,
      AiTraceRecorder traceRecorder) {
    this(clients, properties, advisors, traceRecorder, null);
  }

  public SpringAiChatProviderRegistry(
      Map<String, ChatClient> clients,
      SpringAiChatProperties properties,
      List<? extends Advisor> advisors,
      AiTraceRecorder traceRecorder,
      ToolCallbackProvider toolCallbackProvider) {
    Objects.requireNonNull(clients, "clients must not be null");
    Objects.requireNonNull(properties, "properties must not be null");
    Objects.requireNonNull(traceRecorder, "traceRecorder must not be null");
    Set<String> providerKeys = new HashSet<>();
    providers =
        properties.providers().stream()
            .map(
                configured -> {
                  ChatClient client = clients.get(configured.beanName());
                  if (client == null) {
                    throw new IllegalStateException(
                        "No Spring AI chat client bean configured for provider '"
                            + configured.key()
                            + "'");
                  }
                  if (!providerKeys.add(configured.key())) {
                    throw new IllegalArgumentException(
                        "Duplicate Spring AI chat provider key: " + configured.key());
                  }
                  SpringAiChatModel model =
                      toolCallbackProvider == null
                          ? new SpringAiChatModel(
                              client, configured.key(), configured.modelVersion(), advisors)
                          : new SpringAiChatModel(
                              client,
                              configured.key(),
                              configured.modelVersion(),
                              advisors,
                              toolCallbackProvider);
                  return new ChatModelSelector.Provider(
                      configured.key(),
                      new TracingAiChatCompletion(
                          applicationCompletion(model, configured.key(), configured.modelVersion()),
                          configured.key(),
                          configured.modelVersion(),
                          "chat-v1",
                          traceRecorder),
                      configured.modelVersion());
                })
            .toList();
    if (providers.isEmpty()) {
      throw new IllegalArgumentException("At least one Spring AI chat provider must be configured");
    }
  }

  private static AiChatCompletion applicationCompletion(
      SpringAiChatModel model, String providerKey, String modelVersion) {
    return request -> {
      try {
        return new ChatResponse(
            model.complete(request.conversationContext(), request.userMessage()),
            providerKey,
            modelVersion,
            0,
            0);
      } catch (RuntimeException failure) {
        throw ChatProviderFailurePolicy.preserveInputOrUnavailable(providerKey, failure);
      }
    };
  }

  public List<ChatModelSelector.Provider> providers() {
    return providers;
  }
}
