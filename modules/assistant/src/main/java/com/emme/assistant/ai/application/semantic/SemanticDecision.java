package com.emme.assistant.ai.application.semantic;

import java.util.Optional;

/** Deterministic acceptance decision for a ranked semantic match. */
public record SemanticDecision(
    Optional<String> selectedKey,
    double top1Similarity,
    double top2Similarity,
    double margin,
    boolean accepted) {

  public SemanticDecision {
    if (selectedKey == null) {
      throw new NullPointerException("selectedKey must not be null");
    }
    if (!Double.isFinite(top1Similarity)
        || !Double.isFinite(top2Similarity)
        || !Double.isFinite(margin)) {
      throw new IllegalArgumentException("Semantic decision scores must be finite");
    }
    if (margin < 0) {
      throw new IllegalArgumentException("Semantic decision margin must not be negative");
    }
    if (accepted != selectedKey.isPresent()) {
      throw new IllegalArgumentException("Accepted decisions must select exactly one key");
    }
  }
}
