package com.emme.assistant.ai.application.port.out;

import java.util.Optional;

/** Optional response-cache boundary used only for deterministic safe chat shortcuts. */
public interface SemanticResponseCache {

  Optional<String> lookup(String conversationContext, String userMessage);

  Optional<java.util.UUID> store(String conversationContext, String userMessage, String response);

  /** Invalidates the current authenticated principal's informational cache entries. */
  void invalidate();
}
