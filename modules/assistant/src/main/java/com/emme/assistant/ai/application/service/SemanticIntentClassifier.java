package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import java.util.Objects;

/** Deterministic intent classifier backed by tenant-scoped vector references. */
public final class SemanticIntentClassifier {

  private static final int CANDIDATE_LIMIT = 2;

  private final SemanticReferenceSearchPort search;
  private final SemanticMatchPolicy policy;

  public SemanticIntentClassifier(SemanticReferenceSearchPort search, SemanticMatchPolicy policy) {
    this.search = Objects.requireNonNull(search, "search must not be null");
    this.policy = Objects.requireNonNull(policy, "policy must not be null");
  }

  public SemanticDecision classify(String locale, EmbeddingVector query) {
    requireLocale(locale);
    Objects.requireNonNull(query, "query must not be null");
    return policy.decide(search.searchIntents(locale, query, CANDIDATE_LIMIT));
  }

  private static void requireLocale(String locale) {
    if (locale == null || locale.isBlank()) {
      throw new IllegalArgumentException("locale must not be blank");
    }
  }
}
