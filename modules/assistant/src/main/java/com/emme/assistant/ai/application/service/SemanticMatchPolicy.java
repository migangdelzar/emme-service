package com.emme.assistant.ai.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Configurable top-score and top-two-margin gate for deterministic semantic routing. */
public record SemanticMatchPolicy(double minimumTop1Similarity, double minimumMargin) {

  public SemanticMatchPolicy {
    if (!Double.isFinite(minimumTop1Similarity)
        || minimumTop1Similarity < -1.0
        || minimumTop1Similarity > 1.0) {
      throw new IllegalArgumentException("minimumTop1Similarity must be between -1 and 1");
    }
    if (!Double.isFinite(minimumMargin) || minimumMargin < 0.0 || minimumMargin > 2.0) {
      throw new IllegalArgumentException("minimumMargin must be between 0 and 2");
    }
  }

  public SemanticDecision decide(List<SemanticMatch> matches) {
    if (matches == null) {
      throw new NullPointerException("matches must not be null");
    }
    List<SemanticMatch> ranked =
        matches.stream()
            .sorted(
                Comparator.comparingDouble(SemanticMatch::similarity)
                    .reversed()
                    .thenComparing(SemanticMatch::key))
            .toList();
    if (ranked.isEmpty()) {
      return new SemanticDecision(Optional.empty(), 0.0, 0.0, 0.0, false);
    }

    double top1 = ranked.get(0).similarity();
    double top2 = ranked.size() > 1 ? ranked.get(1).similarity() : -1.0;
    double margin = top1 - top2;
    boolean accepted = top1 >= minimumTop1Similarity && margin >= minimumMargin;
    return new SemanticDecision(
        accepted ? Optional.of(ranked.get(0).key()) : Optional.empty(),
        top1,
        top2,
        margin,
        accepted);
  }
}
