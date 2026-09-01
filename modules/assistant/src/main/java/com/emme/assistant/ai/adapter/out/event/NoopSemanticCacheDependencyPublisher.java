package com.emme.assistant.ai.adapter.out.event;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher;

/** Safe default when semantic caching is disabled. */
public final class NoopSemanticCacheDependencyPublisher
    implements SemanticCacheDependencyPublisher {

  @Override
  public void publish(SemanticCacheDependencyChanged event) {
    // Deliberately empty: disabled semantic caching has no invalidation side effects.
  }
}
