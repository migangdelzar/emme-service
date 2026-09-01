package com.emme.assistant.ai.application.semantic;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.assistant.ai.application.port.out.NoopSemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticCacheHotStore;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.trace.AiSemanticExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Coordinates durable and hot semantic-cache invalidation for dependency changes. */
public final class SemanticCacheInvalidationService {

  private static final String CACHE_KIND = "CHAT_INFORMATIONAL";

  private final SemanticCachePort durable;
  private final Optional<SemanticCacheHotStore> hotStore;
  private final SemanticMetrics metrics;
  private final AiTraceRecorder traceRecorder;

  public SemanticCacheInvalidationService(
      SemanticCachePort durable, Optional<SemanticCacheHotStore> hotStore) {
    this(durable, hotStore, NoopSemanticMetrics.INSTANCE);
  }

  public SemanticCacheInvalidationService(
      SemanticCachePort durable,
      Optional<SemanticCacheHotStore> hotStore,
      SemanticMetrics metrics) {
    this(durable, hotStore, metrics, NoopAiTraceRecorder.INSTANCE);
  }

  public SemanticCacheInvalidationService(
      SemanticCachePort durable,
      Optional<SemanticCacheHotStore> hotStore,
      SemanticMetrics metrics,
      AiTraceRecorder traceRecorder) {
    this.durable = Objects.requireNonNull(durable, "durable must not be null");
    this.hotStore = Objects.requireNonNull(hotStore, "hotStore must not be null");
    this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    this.traceRecorder = Objects.requireNonNull(traceRecorder, "traceRecorder must not be null");
  }

  public void invalidate(SemanticCacheDependencyChanged event) {
    Objects.requireNonNull(event, "event must not be null");
    SemanticCacheInvalidation invalidation =
        new SemanticCacheInvalidation(
            event.tenantId(), event.principalId(), CACHE_KIND, event.dependency(), event.version());
    recordSafely(
        () ->
            metrics.recordInvalidation(
                event.dependency().name(), event.principalId() == null ? "tenant" : "principal"));
    recordTrace(event);
    try {
      durable.invalidate(invalidation);
    } catch (RuntimeException failure) {
      recordSafely(() -> metrics.recordFailure("invalidation", "durable_store_unavailable"));
      throw failure;
    }
    hotStore.ifPresent(store -> safelyInvalidateHotStore(store, invalidation));
  }

  private void recordTrace(SemanticCacheDependencyChanged event) {
    recordSafely(
        () ->
            traceRecorder.recordSemanticOutcome(
                new AiSemanticExecutionTrace(
                    event.eventId(),
                    event.tenantId(),
                    event.principalId(),
                    "cache_invalidation",
                    "requested",
                    0.0,
                    0.0,
                    0.0,
                    List.of(),
                    event.dependency().name(),
                    event.version(),
                    "occurredAt=" + event.occurredAt(),
                    0)));
  }

  private void safelyInvalidateHotStore(
      SemanticCacheHotStore store, SemanticCacheInvalidation invalidation) {
    try {
      store.invalidate(invalidation);
    } catch (RuntimeException ignored) {
      // Redis is an optimization; durable invalidation has already succeeded.
      recordSafely(() -> metrics.recordFailure("invalidation", "hot_store_unavailable"));
      recordSafely(() -> metrics.recordFallback("invalidation", "hot_store_unavailable"));
    }
  }

  private void recordSafely(Runnable recorder) {
    try {
      recorder.run();
    } catch (RuntimeException ignored) {
      // Observability must not change invalidation semantics.
    }
  }
}
