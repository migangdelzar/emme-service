package com.emme.assistant.ai.adapter.out.provider.springai;

import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallbackProvider;

/** Adapts a named Spring AI ChatClient to the application chat boundary. */
public final class SpringAiChatClientAdapter implements ChatCompletionPort {

  private static final String SYSTEM_PROMPT =
      "You are EMME, a helpful salon assistant. Answer in the user's language. "
          + "Never invent prices, availability, permissions, policies, or appointments.";

  private final ChatClient client;
  private final String providerKey;
  private final List<Advisor> advisors;
  private final Optional<ToolCallbackProvider> toolCallbackProvider;

  public SpringAiChatClientAdapter(ChatClient client, String providerKey) {
    this(client, providerKey, List.of(), Optional.empty());
  }

  public SpringAiChatClientAdapter(
      ChatClient client, String providerKey, List<? extends Advisor> advisors) {
    this(client, providerKey, advisors, Optional.empty());
  }

  public SpringAiChatClientAdapter(
      ChatClient client,
      String providerKey,
      List<? extends Advisor> advisors,
      ToolCallbackProvider toolCallbackProvider) {
    this(
        client,
        providerKey,
        advisors,
        Optional.of(
            Objects.requireNonNull(toolCallbackProvider, "toolCallbackProvider must not be null")));
  }

  private SpringAiChatClientAdapter(
      ChatClient client,
      String providerKey,
      List<? extends Advisor> advisors,
      Optional<ToolCallbackProvider> toolCallbackProvider) {
    this.client = Objects.requireNonNull(client, "client must not be null");
    if (providerKey == null || providerKey.isBlank()) {
      throw new IllegalArgumentException("providerKey must not be blank");
    }
    this.providerKey = providerKey;
    this.advisors = List.copyOf(Objects.requireNonNull(advisors, "advisors must not be null"));
    this.toolCallbackProvider =
        Objects.requireNonNull(toolCallbackProvider, "toolCallbackProvider must not be null");
  }

  @Override
  public String complete(String conversationContext, String userMessage) {
    if (userMessage == null || userMessage.isBlank()) {
      throw new IllegalArgumentException("userMessage must not be blank");
    }
    String prompt =
        conversationContext == null || conversationContext.isBlank()
            ? userMessage
            : "Conversation context:\n" + conversationContext + "\n\nUser message:\n" + userMessage;
    try {
      ChatClient.ChatClientRequestSpec request = client.prompt();
      if (!advisors.isEmpty()) {
        request = request.advisors(advisors);
      }
      if (toolCallbackProvider.isPresent()) {
        request = request.tools(toolCallbackProvider.orElseThrow());
      }
      String content = request.system(SYSTEM_PROMPT).user(prompt).call().content();
      if (content == null || content.isBlank()) {
        throw new ChatProviderUnavailableException(
            "Chat provider '" + providerKey + "' returned an empty response");
      }
      return content.strip();
    } catch (ChatProviderUnavailableException unavailable) {
      throw unavailable;
    } catch (RuntimeException exception) {
      throw new ChatProviderUnavailableException(
          "Chat provider '" + providerKey + "' is unavailable", exception);
    }
  }
}
