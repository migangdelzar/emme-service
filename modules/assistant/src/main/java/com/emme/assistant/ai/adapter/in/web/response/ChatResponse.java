package com.emme.assistant.ai.adapter.in.web.response;

import java.util.UUID;

/** HTTP response containing a conversational AI answer. */
public record ChatResponse(String response, UUID conversationId, UUID workflowId) {

  public ChatResponse(String response) {
    this(response, null, null);
  }
}
