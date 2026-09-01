package com.emme.assistant.ai.adapter.out.observability;

import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Objects;

/** Micrometer semantic metrics with bounded outcome labels and no tenant-cardinality labels. */
public final class MicrometerSemanticMetrics implements SemanticMetrics {
  private final MeterRegistry registry;

  public MicrometerSemanticMetrics(MeterRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry must not be null");
  }

  @Override
  public void recordRouting(String outcome) {
    increment("emme.ai.semantic.routing", "Semantic routing outcomes", outcome);
  }

  @Override
  public void recordToolSelection(String outcome) {
    increment("emme.ai.semantic.tool_selection", "Semantic tool-selection outcomes", outcome);
  }

  @Override
  public void recordCacheLookup(String outcome) {
    increment("emme.ai.semantic.cache.lookup", "Semantic cache lookup outcomes", outcome);
  }

  @Override
  public void recordCacheWrite(String outcome) {
    increment("emme.ai.semantic.cache.write", "Semantic cache write outcomes", outcome);
  }

  @Override
  public void recordScores(String operation, double top1, double top2, double margin) {
    String boundedOperation = boundedLabel(operation, "operation");
    requireFinite(top1, "top1");
    requireFinite(top2, "top2");
    requireFinite(margin, "margin");
    summary("emme.ai.semantic.score.top1", "Semantic top-one similarity scores", boundedOperation)
        .record(top1);
    summary("emme.ai.semantic.score.top2", "Semantic top-two similarity scores", boundedOperation)
        .record(top2);
    summary("emme.ai.semantic.score.margin", "Semantic top-two score margins", boundedOperation)
        .record(margin);
  }

  @Override
  public void recordLatency(String operation, Duration duration) {
    Objects.requireNonNull(duration, "duration must not be null");
    if (duration.isNegative()) {
      throw new IllegalArgumentException("duration must not be negative");
    }
    Timer.builder("emme.ai.semantic.latency")
        .description("Semantic operation latency")
        .tag("operation", boundedLabel(operation, "operation"))
        .register(registry)
        .record(duration);
  }

  @Override
  public void recordFailure(String operation, String reason) {
    increment("emme.ai.semantic.failure", "Semantic operation failures", operation, reason);
  }

  @Override
  public void recordFallback(String operation, String reason) {
    increment("emme.ai.semantic.fallback", "Semantic fallback reasons", operation, reason);
  }

  @Override
  public void recordInvalidation(String dependency, String scope) {
    Counter.builder("emme.ai.semantic.invalidation")
        .description("Semantic cache invalidation events")
        .tag("dependency", boundedLabel(dependency, "dependency"))
        .tag("scope", boundedLabel(scope, "scope"))
        .register(registry)
        .increment();
  }

  private void increment(String name, String description, String outcome) {
    Counter.builder(name)
        .description(description)
        .tag("outcome", boundedOutcome(outcome))
        .register(registry)
        .increment();
  }

  private void increment(String name, String description, String operation, String reason) {
    Counter.builder(name)
        .description(description)
        .tag("operation", boundedLabel(operation, "operation"))
        .tag("reason", boundedLabel(reason, "reason"))
        .register(registry)
        .increment();
  }

  private DistributionSummary summary(String name, String description, String operation) {
    return DistributionSummary.builder(name)
        .description(description)
        .tag("operation", operation)
        .register(registry);
  }

  private static void requireFinite(double value, String field) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(field + " must be finite");
    }
  }

  private static String boundedOutcome(String outcome) {
    return boundedLabel(outcome, "outcome");
  }

  private static String boundedLabel(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank() || value.length() > 32 || !value.matches("[A-Za-z0-9_.-]+")) {
      throw new IllegalArgumentException(field + " must be a bounded metric label");
    }
    return value;
  }
}
