package com.emme.assistant.ai.adapter.out.observability;

import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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

  private void increment(String name, String description, String outcome) {
    Counter.builder(name)
        .description(description)
        .tag("outcome", boundedOutcome(outcome))
        .register(registry)
        .increment();
  }

  private static String boundedOutcome(String outcome) {
    Objects.requireNonNull(outcome, "outcome must not be null");
    if (outcome.isBlank() || outcome.length() > 32 || !outcome.matches("[A-Za-z0-9_.-]+")) {
      throw new IllegalArgumentException("outcome must be a bounded metric label");
    }
    return outcome;
  }
}
