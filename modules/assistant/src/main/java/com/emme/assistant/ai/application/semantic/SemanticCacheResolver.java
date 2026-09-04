package com.emme.assistant.ai.application.semantic;

import com.emme.assistant.ai.application.port.out.NoopSemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.trace.AiSemanticExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTracePersistenceFailureReporter;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Resolves a semantic-cache hit before an LLM pipeline is invoked. */
public final class SemanticCacheResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(SemanticCacheResolver.class);

  private static final int CANDIDATE_LIMIT = 2;

  private final SemanticCachePort cache;
  private final SemanticCachePolicy policy;
  private final SemanticMetrics metrics;
  private final AiTraceRecorder traceRecorder;

  public SemanticCacheResolver(SemanticCachePort cache, SemanticCachePolicy policy) {
    this(cache, policy, NoopSemanticMetrics.INSTANCE);
  }

  public SemanticCacheResolver(
      SemanticCachePort cache, SemanticCachePolicy policy, SemanticMetrics metrics) {
    this(cache, policy, metrics, NoopAiTraceRecorder.INSTANCE);
  }

  public SemanticCacheResolver(
      SemanticCachePort cache,
      SemanticCachePolicy policy,
      SemanticMetrics metrics,
      AiTraceRecorder traceRecorder) {
    this.cache = Objects.requireNonNull(cache, "cache must not be null");
    this.policy = Objects.requireNonNull(policy, "policy must not be null");
    this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    this.traceRecorder = Objects.requireNonNull(traceRecorder, "traceRecorder must not be null");
  }

  public Optional<SemanticCachePort.Candidate> lookup(SemanticCachePort.Lookup lookup) {
    return lookup(lookup, candidate -> true);
  }

  public Optional<SemanticCachePort.Candidate> lookup(
      SemanticCachePort.Lookup lookup, Predicate<SemanticCachePort.Candidate> validator) {
    Objects.requireNonNull(lookup, "lookup must not be null");
    Objects.requireNonNull(validator, "validator must not be null");
    long started = System.nanoTime();
    try {
      return confirm(cache.find(lookup, CANDIDATE_LIMIT), validator);
    } catch (RuntimeException failure) {
      recordFailure("lookup", SemanticFailureReason.code(failure));
      recordTrace(List.of(), "failed");
      throw failure;
    } finally {
      recordLatency("lookup", Duration.ofNanos(System.nanoTime() - started));
    }
  }

  /** Confirms a hot projection hit against the durable cache before it is returned. */
  public Optional<SemanticCachePort.Candidate> confirm(
      List<SemanticCachePort.Candidate> candidates) {
    return confirm(candidates, candidate -> true);
  }

  /** Confirms a candidate only after the caller's payload/safety validation succeeds. */
  public Optional<SemanticCachePort.Candidate> confirm(
      List<SemanticCachePort.Candidate> candidates,
      Predicate<SemanticCachePort.Candidate> validator) {
    Objects.requireNonNull(candidates, "candidates must not be null");
    Objects.requireNonNull(validator, "validator must not be null");
    recordScores(candidates);
    Optional<SemanticCachePort.Candidate> selected = policy.select(candidates);
    Optional<SemanticCachePort.Candidate> confirmed =
        selected.filter(validator).filter(candidate -> cache.recordHit(candidate.id()));
    recordTrace(candidates, confirmed.isPresent() ? "hit" : "miss");
    return confirmed;
  }

  private void recordTrace(List<SemanticCachePort.Candidate> candidates, String outcome) {
    double top1 =
        candidates.stream().mapToDouble(SemanticCachePort.Candidate::similarity).max().orElse(0.0);
    double top2 =
        candidates.stream()
            .mapToDouble(SemanticCachePort.Candidate::similarity)
            .sorted()
            .skip(Math.max(0, candidates.size() - 2L))
            .findFirst()
            .orElse(0.0);
    if (candidates.size() < 2) top2 = 0.0;
    double margin = Math.max(0.0, top1 - top2);
    double finalTop2 = top2;
    try {
      traceRecorder.recordSemanticOutcome(
          new AiSemanticExecutionTrace(
              UUID.randomUUID(),
              null,
              null,
              "semantic_cache",
              outcome,
              top1,
              finalTop2,
              margin,
              candidates.stream().map(candidate -> candidate.id().toString()).toList(),
              null,
              null,
              null,
              0));
    } catch (RuntimeException failure) {
      recordSafely(() -> metrics.recordFailure("trace", "trace_persistence_failed"));
      AiTracePersistenceFailureReporter.report(LOGGER, "semantic_cache", failure);
    }
  }

  private void recordScores(List<SemanticCachePort.Candidate> candidates) {
    double top1 =
        candidates.stream().mapToDouble(SemanticCachePort.Candidate::similarity).max().orElse(0.0);
    double top2 =
        candidates.stream()
            .mapToDouble(SemanticCachePort.Candidate::similarity)
            .sorted()
            .skip(Math.max(0, candidates.size() - 2L))
            .findFirst()
            .orElse(-1.0);
    if (candidates.size() < 2) {
      top2 = -1.0;
    }
    double finalTop1 = top1;
    double finalTop2 = top2;
    recordSafely(() -> metrics.recordScores("cache", finalTop1, finalTop2, finalTop1 - finalTop2));
  }

  private void recordLatency(String operation, Duration duration) {
    recordSafely(() -> metrics.recordLatency("cache." + operation, duration));
  }

  private void recordFailure(String operation, String reason) {
    recordSafely(() -> metrics.recordFailure("cache." + operation, reason));
  }

  private static void recordSafely(Runnable recorder) {
    try {
      recorder.run();
    } catch (RuntimeException ignored) {
      // Observability must not change cache semantics.
    }
  }
}
