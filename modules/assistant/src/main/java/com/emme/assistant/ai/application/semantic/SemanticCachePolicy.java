package com.emme.assistant.ai.application.semantic;

import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Conservative threshold policy for returning a semantic-cache response. */
public record SemanticCachePolicy(double minimumSimilarity, double minimumMargin) {

  public SemanticCachePolicy(double minimumSimilarity) {
    this(minimumSimilarity, 0.0);
  }

  public SemanticCachePolicy {
    if (!Double.isFinite(minimumSimilarity)
        || minimumSimilarity < -1.0
        || minimumSimilarity > 1.0) {
      throw new IllegalArgumentException("minimumSimilarity must be between -1 and 1");
    }
    if (!Double.isFinite(minimumMargin) || minimumMargin < 0.0 || minimumMargin > 2.0) {
      throw new IllegalArgumentException("minimumMargin must be between 0 and 2");
    }
  }

  public Optional<SemanticCachePort.Candidate> select(
      List<SemanticCachePort.Candidate> candidates) {
    Objects.requireNonNull(candidates, "candidates must not be null");
    List<SemanticCachePort.Candidate> ranked =
        candidates.stream()
            .peek(candidate -> Objects.requireNonNull(candidate, "candidate must not be null"))
            .sorted(
                Comparator.comparingDouble(SemanticCachePort.Candidate::similarity)
                    .reversed()
                    .thenComparing(SemanticCachePort.Candidate::id))
            .toList();
    if (ranked.isEmpty()) {
      return Optional.empty();
    }
    if (ranked.size() == 1 && minimumMargin > 0.0) {
      return Optional.empty();
    }
    SemanticCachePort.Candidate top1 = ranked.getFirst();
    double top2 = ranked.size() > 1 ? ranked.get(1).similarity() : -1.0;
    return top1.similarity() >= minimumSimilarity && top1.similarity() - top2 >= minimumMargin
        ? Optional.of(top1)
        : Optional.empty();
  }
}
