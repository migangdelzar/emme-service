package com.emme.assistant.ai.api.usecase;

public interface ChatUseCase {
  String chat(String conversationContext, String userMessage);
}
