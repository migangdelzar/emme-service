package com.emme.ai.contracts.semantic;

/** Publishes a durable application event when a semantic-cache dependency changes. */
public interface SemanticCacheDependencyPublisher {

  void publish(SemanticCacheDependencyChanged event);
}
