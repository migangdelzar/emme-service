package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Conservative threshold policy for returning a semantic-cache response. */
public record SemanticCachePolicy(double minimumSimilarity) {

  public SemanticCachePolicy {
    if (!Double.isFinite(minimumSimilarity)
        || minimumSimilarity < -1.0
        || minimumSimilarity > 1.0) {
      throw new IllegalArgumentException("minimumSimilarity must be between -1 and 1");
    }
  }

  public Optional<SemanticCachePort.Candidate> select(
      List<SemanticCachePort.Candidate> candidates) {
    Objects.requireNonNull(candidates, "candidates must not be null");
    return candidates.stream()
        .peek(candidate -> Objects.requireNonNull(candidate, "candidate must not be null"))
        .sorted(
            Comparator.comparingDouble(SemanticCachePort.Candidate::similarity)
                .reversed()
                .thenComparing(SemanticCachePort.Candidate::id))
        .filter(candidate -> candidate.similarity() >= minimumSimilarity)
        .findFirst();
  }
}
