package com.emme.assistant.ai.application.workflow;

import java.util.Objects;
import java.util.Set;

/** Immutable allow-list for the memory projection visible to one workflow node. */
public record NodeMemoryPolicy(Set<String> allowedScopes, int maxTurns, boolean includeLongTerm) {

  public NodeMemoryPolicy {
    Objects.requireNonNull(allowedScopes, "allowedScopes must not be null");
    if (allowedScopes.stream().anyMatch(scope -> scope == null || scope.isBlank())) {
      throw new IllegalArgumentException("allowedScopes must not contain blank values");
    }
    if (maxTurns < 0) {
      throw new IllegalArgumentException("maxTurns must not be negative");
    }
    allowedScopes = Set.copyOf(allowedScopes);
  }
}
