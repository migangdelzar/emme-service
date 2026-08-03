package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.application.port.out.ModelProvider;
import org.springframework.stereotype.Service;

/** Executes the chat capability through the configured model-provider boundary. */
@Service
public class ChatService implements ChatUseCase {
  private final ModelProvider provider;

  public ChatService(ModelProvider provider) {
    this.provider = provider;
  }

  @Override
  public String chat(String conversationContext, String userMessage) {
    return provider.chat(conversationContext, userMessage);
  }
}
