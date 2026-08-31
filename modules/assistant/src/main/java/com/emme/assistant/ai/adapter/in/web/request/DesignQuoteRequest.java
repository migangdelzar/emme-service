package com.emme.assistant.ai.adapter.in.web.request;

import java.util.UUID;

/** Multipart quote fields; tenant identity is deliberately absent. */
public record DesignQuoteRequest(UUID conversationId, String templateKey, String inputText) {
  public DesignQuoteRequest {
    if (conversationId == null)
      throw new IllegalArgumentException("conversationId must not be null");
    if (templateKey == null || templateKey.isBlank())
      throw new IllegalArgumentException("templateKey must not be blank");
  }
}
