package com.emme.assistant.ai.adapter.out.observability;

import com.emme.assistant.ai.application.port.out.AiJobMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/** Micrometer adapter with bounded outcome labels and no tenant-cardinality labels. */
public final class MicrometerAiJobMetrics implements AiJobMetrics {
  private final MeterRegistry registry;
  private final AtomicInteger queueDepth = new AtomicInteger();

  public MicrometerAiJobMetrics(MeterRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry must not be null");
    registry.gauge("emme.ai.jobs.queue.depth", queueDepth);
  }

  @Override
  public void recordQueueDepth(int depth) {
    queueDepth.set(Math.max(depth, 0));
  }

  @Override
  public void recordClaim(String outcome) {
    Counter.builder("emme.ai.jobs.claims")
        .description("Durable AI job claim outcomes")
        .tag("outcome", label(outcome, "outcome"))
        .register(registry)
        .increment();
  }

  @Override
  public void recordFailure() {
    counter("emme.ai.jobs.failures", "Durable AI job failures").increment();
  }

  @Override
  public void recordRetry() {
    counter("emme.ai.jobs.retries", "Durable AI job retries").increment();
  }

  @Override
  public void recordDeadLetter() {
    counter("emme.ai.jobs.dead_letter", "AI jobs moved to dead letter").increment();
  }

  @Override
  public void recordTenantFairness() {
    counter("emme.ai.jobs.tenant.fairness", "AI reconciliation tenant scheduling selections")
        .increment();
  }

  private Counter counter(String name, String description) {
    return Counter.builder(name).description(description).register(registry);
  }

  private static String label(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank() || value.length() > 32 || !value.matches("[A-Za-z0-9_.-]+")) {
      throw new IllegalArgumentException(field + " must be a bounded metric label");
    }
    return value;
  }
}
