package com.emme.ai.contracts.routing;

import java.util.Objects;

/** One ranked intent candidate and its bounded similarity score. */
public record IntentMatch(IntentDefinition definition, double similarity) {

  public IntentMatch {
    definition = Objects.requireNonNull(definition, "definition must not be null");
    if (!Double.isFinite(similarity) || similarity < 0 || similarity > 1) {
      throw new IllegalArgumentException("similarity must be between 0 and 1");
    }
  }
}
