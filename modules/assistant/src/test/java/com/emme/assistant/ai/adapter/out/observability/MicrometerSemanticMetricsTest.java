package com.emme.assistant.ai.adapter.out.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MicrometerSemanticMetricsTest {

  @Test
  void recordsSemanticOutcomesWithBoundedLabels() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MicrometerSemanticMetrics metrics = new MicrometerSemanticMetrics(registry);

    metrics.recordRouting("accepted");
    metrics.recordToolSelection("abstained");
    metrics.recordCacheLookup("hit");
    metrics.recordCacheWrite("rejected");

    assertThat(
            registry.get("emme.ai.semantic.routing").tag("outcome", "accepted").counter().count())
        .isEqualTo(1.0);
    assertThat(
            registry
                .get("emme.ai.semantic.tool_selection")
                .tag("outcome", "abstained")
                .counter()
                .count())
        .isEqualTo(1.0);
    assertThat(
            registry.get("emme.ai.semantic.cache.lookup").tag("outcome", "hit").counter().count())
        .isEqualTo(1.0);
    assertThat(
            registry
                .get("emme.ai.semantic.cache.write")
                .tag("outcome", "rejected")
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  @Test
  void rejectsUnboundedMetricLabels() {
    MicrometerSemanticMetrics metrics = new MicrometerSemanticMetrics(new SimpleMeterRegistry());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> metrics.recordRouting("tenant-" + "x".repeat(100)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void recordsScoresMarginsLatencyFailuresFallbacksAndInvalidations() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MicrometerSemanticMetrics metrics = new MicrometerSemanticMetrics(registry);

    metrics.recordScores("cache", 0.93, 0.81, 0.12);
    metrics.recordLatency("cache.lookup", Duration.ofMillis(17));
    metrics.recordFailure("cache", "redis_unavailable");
    metrics.recordFallback("chat", "semantic_cache_failure");
    metrics.recordInvalidation("PRICE", "tenant");

    assertThat(
            registry.get("emme.ai.semantic.score.top1").tag("operation", "cache").summary().count())
        .isEqualTo(1);
    assertThat(
            registry
                .get("emme.ai.semantic.score.margin")
                .tag("operation", "cache")
                .summary()
                .mean())
        .isEqualTo(0.12);
    assertThat(
            registry
                .get("emme.ai.semantic.latency")
                .tag("operation", "cache.lookup")
                .timer()
                .count())
        .isEqualTo(1);
    assertThat(
            registry
                .get("emme.ai.semantic.failure")
                .tag("operation", "cache")
                .tag("reason", "redis_unavailable")
                .counter()
                .count())
        .isEqualTo(1);
    assertThat(
            registry
                .get("emme.ai.semantic.fallback")
                .tag("operation", "chat")
                .tag("reason", "semantic_cache_failure")
                .counter()
                .count())
        .isEqualTo(1);
    assertThat(
            registry
                .get("emme.ai.semantic.invalidation")
                .tag("dependency", "PRICE")
                .tag("scope", "tenant")
                .counter()
                .count())
        .isEqualTo(1);
  }
}
