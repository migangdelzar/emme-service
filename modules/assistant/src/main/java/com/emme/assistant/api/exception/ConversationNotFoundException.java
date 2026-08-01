package com.emme.assistant.api.exception;

import java.util.UUID;

public class ConversationNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ConversationNotFoundException(UUID conversationId) {
    super("Conversation not found: " + conversationId);
  }
}
