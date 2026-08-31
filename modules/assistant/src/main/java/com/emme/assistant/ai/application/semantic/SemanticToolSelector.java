package com.emme.assistant.ai.application.semantic;

import com.emme.assistant.ai.application.port.out.NoopSemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import java.util.Objects;
import java.util.Set;

/** Deterministic semantic tool selector constrained by backend authorization. */
public final class SemanticToolSelector {

  private static final int CANDIDATE_LIMIT = 2;

  private final SemanticReferenceSearchPort search;
  private final SemanticMatchPolicy policy;
  private final SemanticMetrics metrics;

  public SemanticToolSelector(SemanticReferenceSearchPort search, SemanticMatchPolicy policy) {
    this(search, policy, NoopSemanticMetrics.INSTANCE);
  }

  public SemanticToolSelector(
      SemanticReferenceSearchPort search, SemanticMatchPolicy policy, SemanticMetrics metrics) {
    this.search = Objects.requireNonNull(search, "search must not be null");
    this.policy = Objects.requireNonNull(policy, "policy must not be null");
    this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
  }

  public SemanticDecision select(
      String locale, EmbeddingVector query, Set<String> authorizedToolKeys) {
    requireLocale(locale);
    Objects.requireNonNull(query, "query must not be null");
    Objects.requireNonNull(authorizedToolKeys, "authorizedToolKeys must not be null");
    Set<String> authorized = Set.copyOf(authorizedToolKeys);
    if (authorized.isEmpty()) {
      record("abstained");
      return new SemanticDecision(java.util.Optional.empty(), 0.0, 0.0, 0.0, false);
    }

    SemanticDecision decision =
        policy.decide(search.searchTools(locale, query, authorized, CANDIDATE_LIMIT));
    if (decision.selectedKey().isPresent()
        && !authorized.contains(decision.selectedKey().orElseThrow())) {
      record("unauthorized");
      return rejected(decision);
    }
    record(decision.accepted() ? "accepted" : "abstained");
    return decision;
  }

  private void record(String outcome) {
    try {
      metrics.recordToolSelection(outcome);
    } catch (RuntimeException ignored) {
      // Observability must not change selection semantics.
    }
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
