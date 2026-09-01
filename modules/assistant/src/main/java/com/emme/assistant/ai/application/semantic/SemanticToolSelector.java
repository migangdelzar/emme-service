package com.emme.assistant.ai.application.semantic;

import com.emme.assistant.ai.application.port.out.NoopSemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import com.emme.assistant.ai.application.trace.AiSemanticExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Deterministic semantic tool selector constrained by backend authorization. */
public final class SemanticToolSelector {

  private static final int CANDIDATE_LIMIT = 2;

  private final SemanticReferenceSearchPort search;
  private final SemanticMatchPolicy policy;
  private final SemanticMetrics metrics;
  private final AiTraceRecorder traceRecorder;

  public SemanticToolSelector(SemanticReferenceSearchPort search, SemanticMatchPolicy policy) {
    this(search, policy, NoopSemanticMetrics.INSTANCE);
  }

  public SemanticToolSelector(
      SemanticReferenceSearchPort search, SemanticMatchPolicy policy, SemanticMetrics metrics) {
    this(search, policy, metrics, NoopAiTraceRecorder.INSTANCE);
  }

  public SemanticToolSelector(
      SemanticReferenceSearchPort search,
      SemanticMatchPolicy policy,
      SemanticMetrics metrics,
      AiTraceRecorder traceRecorder) {
    this.search = Objects.requireNonNull(search, "search must not be null");
    this.policy = Objects.requireNonNull(policy, "policy must not be null");
    this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    this.traceRecorder = Objects.requireNonNull(traceRecorder, "traceRecorder must not be null");
  }

  public SemanticDecision select(
      String locale, EmbeddingVector query, Set<String> authorizedToolKeys) {
    requireLocale(locale);
    Objects.requireNonNull(query, "query must not be null");
    Objects.requireNonNull(authorizedToolKeys, "authorizedToolKeys must not be null");
    Set<String> authorized = Set.copyOf(authorizedToolKeys);
    long started = System.nanoTime();
    try {
      if (authorized.isEmpty()) {
        record("abstained");
        return new SemanticDecision(java.util.Optional.empty(), 0.0, 0.0, 0.0, false);
      }

      List<SemanticMatch> matches = search.searchTools(locale, query, authorized, CANDIDATE_LIMIT);
      SemanticDecision decision = policy.decide(matches);
      recordScores(decision);
      if (decision.selectedKey().isPresent()
          && !authorized.contains(decision.selectedKey().orElseThrow())) {
        record("unauthorized");
        return rejected(decision);
      }
      record(decision.accepted() ? "accepted" : "abstained");
      recordTrace(decision, matches);
      return decision;
    } catch (RuntimeException failure) {
      recordSafely(
          () -> metrics.recordFailure("tool_selection", failure.getClass().getSimpleName()));
      throw failure;
    } finally {
      recordSafely(
          () ->
              metrics.recordLatency(
                  "tool_selection", Duration.ofNanos(System.nanoTime() - started)));
    }
  }

  private void recordTrace(SemanticDecision decision, List<SemanticMatch> matches) {
    recordSafely(
        () ->
            traceRecorder.recordSemanticOutcome(
                new AiSemanticExecutionTrace(
                    UUID.randomUUID(),
                    null,
                    null,
                    "tool_selection",
                    decision.accepted() ? "accepted" : "abstained",
                    decision.top1Similarity(),
                    decision.top2Similarity(),
                    decision.margin(),
                    matches.stream().map(SemanticMatch::key).toList(),
                    null,
                    null,
                    null,
                    0)));
  }

  private void recordScores(SemanticDecision decision) {
    recordSafely(
        () ->
            metrics.recordScores(
                "tool_selection",
                decision.top1Similarity(),
                decision.top2Similarity(),
                decision.margin()));
  }

  private void record(String outcome) {
    recordSafely(() -> metrics.recordToolSelection(outcome));
  }

  private static void recordSafely(Runnable recorder) {
    try {
      recorder.run();
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
