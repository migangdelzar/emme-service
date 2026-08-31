package com.emme.assistant.ai.adapter.out.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
}
