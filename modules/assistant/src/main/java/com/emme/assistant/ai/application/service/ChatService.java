package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.application.AiService;
import org.springframework.stereotype.Service;

/** Executes the chat capability through the configured model-provider boundary. */
@Service
public class ChatService implements ChatUseCase {
  private final AiService aiService;

  public ChatService(AiService aiService) {
    this.aiService = aiService;
  }

  @Override
  public String chat(String conversationContext, String userMessage) {
    return aiService.chat(conversationContext, userMessage);
  }
}
