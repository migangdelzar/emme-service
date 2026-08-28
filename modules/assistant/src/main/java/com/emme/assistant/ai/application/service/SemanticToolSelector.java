package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import java.util.Objects;
import java.util.Set;

/** Deterministic semantic tool selector constrained by backend authorization. */
public final class SemanticToolSelector {

  private static final int CANDIDATE_LIMIT = 2;

  private final SemanticReferenceSearchPort search;
  private final SemanticMatchPolicy policy;

  public SemanticToolSelector(SemanticReferenceSearchPort search, SemanticMatchPolicy policy) {
    this.search = Objects.requireNonNull(search, "search must not be null");
    this.policy = Objects.requireNonNull(policy, "policy must not be null");
  }

  public SemanticDecision select(
      String locale, EmbeddingVector query, Set<String> authorizedToolKeys) {
    requireLocale(locale);
    Objects.requireNonNull(query, "query must not be null");
    Objects.requireNonNull(authorizedToolKeys, "authorizedToolKeys must not be null");
    Set<String> authorized = Set.copyOf(authorizedToolKeys);
    if (authorized.isEmpty()) {
      return new SemanticDecision(java.util.Optional.empty(), 0.0, 0.0, 0.0, false);
    }

    SemanticDecision decision =
        policy.decide(search.searchTools(locale, query, authorized, CANDIDATE_LIMIT));
    if (decision.selectedKey().isPresent()
        && !authorized.contains(decision.selectedKey().orElseThrow())) {
      return rejected(decision);
    }
    return decision;
  }

  private static SemanticDecision rejected(SemanticDecision decision) {
    return new SemanticDecision(
        java.util.Optional.empty(),
        decision.top1Similarity(),
        decision.top2Similarity(),
        decision.margin(),
        false);
  }

  private static void requireLocale(String locale) {
    if (locale == null || locale.isBlank()) {
      throw new IllegalArgumentException("locale must not be blank");
    }
  }
}
