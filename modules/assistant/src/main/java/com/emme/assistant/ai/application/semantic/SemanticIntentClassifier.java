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
import java.util.UUID;

/** Deterministic intent classifier backed by tenant-scoped vector references. */
public final class SemanticIntentClassifier {

  private static final int CANDIDATE_LIMIT = 2;

  private final SemanticReferenceSearchPort search;
  private final SemanticMatchPolicy policy;
  private final SemanticMetrics metrics;
  private final AiTraceRecorder traceRecorder;

  public SemanticIntentClassifier(SemanticReferenceSearchPort search, SemanticMatchPolicy policy) {
    this(search, policy, NoopSemanticMetrics.INSTANCE);
  }

  public SemanticIntentClassifier(
      SemanticReferenceSearchPort search, SemanticMatchPolicy policy, SemanticMetrics metrics) {
    this(search, policy, metrics, NoopAiTraceRecorder.INSTANCE);
  }

  public SemanticIntentClassifier(
      SemanticReferenceSearchPort search,
      SemanticMatchPolicy policy,
      SemanticMetrics metrics,
      AiTraceRecorder traceRecorder) {
    this.search = Objects.requireNonNull(search, "search must not be null");
    this.policy = Objects.requireNonNull(policy, "policy must not be null");
    this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    this.traceRecorder = Objects.requireNonNull(traceRecorder, "traceRecorder must not be null");
  }

  public SemanticDecision classify(String locale, EmbeddingVector query) {
    requireLocale(locale);
    Objects.requireNonNull(query, "query must not be null");
    long started = System.nanoTime();
    try {
      List<SemanticMatch> matches = search.searchIntents(locale, query, CANDIDATE_LIMIT);
      SemanticDecision decision = policy.decide(matches);
      recordScores(decision);
      record(decision.accepted() ? "accepted" : "abstained");
      recordTrace(decision, matches, decision.accepted() ? "accepted" : "abstained");
      return decision;
    } catch (RuntimeException failure) {
      recordSafely(() -> metrics.recordFailure("routing", SemanticFailureReason.code(failure)));
      recordTrace(
          new SemanticDecision(java.util.Optional.empty(), 0.0, 0.0, 0.0, false),
          List.of(),
          "failed");
      throw failure;
    } finally {
      recordSafely(
          () -> metrics.recordLatency("routing", Duration.ofNanos(System.nanoTime() - started)));
    }
  }

  private void recordTrace(
      SemanticDecision decision, List<SemanticMatch> matches, String outcome) {
    try {
      traceRecorder.recordSemanticOutcome(
          new AiSemanticExecutionTrace(
              UUID.randomUUID(),
              null,
              null,
              "intent_routing",
              outcome,
              decision.top1Similarity(),
              decision.top2Similarity(),
              decision.margin(),
              matches.stream().map(SemanticMatch::key).toList(),
              null,
              null,
              null,
              0));
    } catch (RuntimeException failure) {
      recordSafely(() -> metrics.recordFailure("trace", "trace_persistence_failed"));
    }
  }

  private void recordScores(SemanticDecision decision) {
    recordSafely(
        () ->
            metrics.recordScores(
                "routing",
                decision.top1Similarity(),
                decision.top2Similarity(),
                decision.margin()));
  }

  private void record(String outcome) {
    recordSafely(() -> metrics.recordRouting(outcome));
  }

  private static void recordSafely(Runnable recorder) {
    try {
      recorder.run();
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
