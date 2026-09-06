package com.emme.assistant.ai.application.workflow;

import java.util.Objects;
import java.util.Set;

/** Immutable allow-list and execution policy for one workflow node. */
public record NodeToolPolicy(
    Set<String> allowedKeys, boolean readOnly, boolean requiresConfirmation) {

  public NodeToolPolicy {
    Objects.requireNonNull(allowedKeys, "allowedKeys must not be null");
    if (allowedKeys.stream().anyMatch(key -> key == null || key.isBlank())) {
      throw new IllegalArgumentException("allowedKeys must not contain blank values");
    }
    allowedKeys = Set.copyOf(allowedKeys);
  }
}
