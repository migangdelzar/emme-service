package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.provider.springai.SpringAiChatClientAdapter;
import com.emme.assistant.ai.application.provider.ChatProviderChain;
import com.emme.assistant.ai.application.provider.TracingChatCompletionPort;
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

/** Builds the ordered provider-neutral chat chain from explicitly named clients. */
public final class SpringAiChatProviderRegistry {

  private final List<ChatProviderChain.Provider> providers;

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
                  SpringAiChatClientAdapter adapter =
                      toolCallbackProvider == null
                          ? new SpringAiChatClientAdapter(client, configured.key(), advisors)
                          : new SpringAiChatClientAdapter(
                              client, configured.key(), advisors, toolCallbackProvider);
                  return new ChatProviderChain.Provider(
                      configured.key(),
                      new TracingChatCompletionPort(
                          adapter,
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

  public List<ChatProviderChain.Provider> providers() {
    return providers;
  }
}
