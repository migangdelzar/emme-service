package com.emme.assistant.ai.adapter.in.web.request;

import java.util.UUID;

/** HTTP request for a conversational AI response. */
public record ChatRequest(String userMessage, String conversationContext, UUID conversationId) {

  public ChatRequest(String userMessage, String conversationContext) {
    this(userMessage, conversationContext, null);
  }
}
