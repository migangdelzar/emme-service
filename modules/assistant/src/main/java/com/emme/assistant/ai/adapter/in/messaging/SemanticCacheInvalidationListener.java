package com.emme.assistant.ai.adapter.in.messaging;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.assistant.ai.application.semantic.SemanticCacheInvalidationService;
import org.springframework.modulith.events.ApplicationModuleListener;

/** Replays durable tenant dependency events into both semantic-cache tiers. */
public final class SemanticCacheInvalidationListener {

  private final SemanticCacheInvalidationService invalidation;

  public SemanticCacheInvalidationListener(SemanticCacheInvalidationService invalidation) {
    this.invalidation = invalidation;
  }

  @ApplicationModuleListener(id = "assistant.semantic-cache-invalidation")
  public void onDependencyChange(SemanticCacheDependencyChanged event) {
    invalidation.invalidate(event);
  }
}
