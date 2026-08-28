package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.provider.springai.SpringAiChatClientAdapter;
import com.emme.assistant.ai.application.service.ChatProviderChain;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

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
    Objects.requireNonNull(clients, "clients must not be null");
    Objects.requireNonNull(properties, "properties must not be null");
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
                  return new ChatProviderChain.Provider(
                      configured.key(),
                      new SpringAiChatClientAdapter(client, configured.key(), advisors));
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
