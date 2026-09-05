package com.emme.assistant.ai.application.rag;

import java.time.Duration;
import java.util.Objects;

/** Bounded controls for retrieval query transformation. */
public record QueryImprovementPolicy(
    int maximumAttempts,
    int maximumVariants,
    int maximumQueryCharacters,
    Duration maximumDuration,
    boolean allowCompression,
    boolean allowRewrite,
    boolean allowTranslation,
    boolean allowExpansion) {

  public QueryImprovementPolicy {
    if (maximumAttempts < 1) {
      throw new IllegalArgumentException("maximumAttempts must be positive");
    }
    if (maximumVariants < 0) {
      throw new IllegalArgumentException("maximumVariants must be non-negative");
    }
    if (maximumQueryCharacters < 1) {
      throw new IllegalArgumentException("maximumQueryCharacters must be positive");
    }
    maximumDuration = Objects.requireNonNull(maximumDuration, "maximumDuration must not be null");
    if (maximumDuration.isZero() || maximumDuration.isNegative()) {
      throw new IllegalArgumentException("maximumDuration must be positive");
    }
  }
}
