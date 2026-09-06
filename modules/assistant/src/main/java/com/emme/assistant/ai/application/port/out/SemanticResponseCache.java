package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.application.semantic.SemanticCacheIdentity;
import com.emme.assistant.ai.application.semantic.SemanticQuery;
import java.util.Optional;

/** Optional response-cache boundary used only for deterministic safe chat shortcuts. */
public interface SemanticResponseCache {

  Optional<String> lookup(String conversationContext, SemanticQuery query);

  Optional<java.util.UUID> store(String conversationContext, SemanticQuery query, String response);

  /** Stores a response with the provider identity that actually produced it. */
  default Optional<java.util.UUID> store(
      String conversationContext,
      SemanticQuery query,
      String response,
      SemanticCacheIdentity producingIdentity) {
    return store(conversationContext, query, response);
  }

  /** Invalidates the current authenticated principal's informational cache entries. */
  void invalidate();
}
