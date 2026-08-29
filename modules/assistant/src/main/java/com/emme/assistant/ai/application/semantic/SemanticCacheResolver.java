package com.emme.assistant.ai.application.semantic;

import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Resolves a semantic-cache hit before an LLM pipeline is invoked. */
public final class SemanticCacheResolver {

  private static final int CANDIDATE_LIMIT = 2;

  private final SemanticCachePort cache;
  private final SemanticCachePolicy policy;

  public SemanticCacheResolver(SemanticCachePort cache, SemanticCachePolicy policy) {
    this.cache = Objects.requireNonNull(cache, "cache must not be null");
    this.policy = Objects.requireNonNull(policy, "policy must not be null");
  }

  public Optional<SemanticCachePort.Candidate> lookup(SemanticCachePort.Lookup lookup) {
    Objects.requireNonNull(lookup, "lookup must not be null");
    return confirm(cache.find(lookup, CANDIDATE_LIMIT));
  }

  /** Confirms a hot projection hit against the durable cache before it is returned. */
  public Optional<SemanticCachePort.Candidate> confirm(
      List<SemanticCachePort.Candidate> candidates) {
    Objects.requireNonNull(candidates, "candidates must not be null");
    return policy.select(candidates).filter(candidate -> cache.recordHit(candidate.id()));
  }
}
