package com.emme.assistant.ai.application.semantic;

import com.emme.assistant.ai.application.port.out.NoopSemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import java.util.Objects;

/** Deterministic intent classifier backed by tenant-scoped vector references. */
public final class SemanticIntentClassifier {

  private static final int CANDIDATE_LIMIT = 2;

  private final SemanticReferenceSearchPort search;
  private final SemanticMatchPolicy policy;
  private final SemanticMetrics metrics;

  public SemanticIntentClassifier(SemanticReferenceSearchPort search, SemanticMatchPolicy policy) {
    this(search, policy, NoopSemanticMetrics.INSTANCE);
  }

  public SemanticIntentClassifier(
      SemanticReferenceSearchPort search, SemanticMatchPolicy policy, SemanticMetrics metrics) {
    this.search = Objects.requireNonNull(search, "search must not be null");
    this.policy = Objects.requireNonNull(policy, "policy must not be null");
    this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
  }

  public SemanticDecision classify(String locale, EmbeddingVector query) {
    requireLocale(locale);
    Objects.requireNonNull(query, "query must not be null");
    SemanticDecision decision = policy.decide(search.searchIntents(locale, query, CANDIDATE_LIMIT));
    record(decision.accepted() ? "accepted" : "abstained");
    return decision;
  }

  private void record(String outcome) {
    try {
      metrics.recordRouting(outcome);
    } catch (RuntimeException ignored) {
      // Observability must not change routing semantics.
    }
  }

  private static void requireLocale(String locale) {
    if (locale == null || locale.isBlank()) {
      throw new IllegalArgumentException("locale must not be blank");
    }
  }
}
