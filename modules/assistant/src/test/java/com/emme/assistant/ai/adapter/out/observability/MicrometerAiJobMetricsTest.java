package com.emme.assistant.ai.adapter.out.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MicrometerAiJobMetricsTest {

  @Test
  void recordsBoundedJobLifecycleCountersQueueDepthAndTenantFairness() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MicrometerAiJobMetrics metrics = new MicrometerAiJobMetrics(registry);

    metrics.recordQueueDepth(4);
    metrics.recordClaim("claimed");
    metrics.recordClaim("not_available");
    metrics.recordFailure();
    metrics.recordRetry();
    metrics.recordDeadLetter();
    metrics.recordTenantFairness();

    assertThat(registry.get("emme.ai.jobs.queue.depth").gauge().value()).isEqualTo(4.0);
    assertThat(registry.counter("emme.ai.jobs.claims", "outcome", "claimed").count()).isEqualTo(1);
    assertThat(registry.counter("emme.ai.jobs.claims", "outcome", "not_available").count())
        .isEqualTo(1);
    assertThat(registry.counter("emme.ai.jobs.failures").count()).isEqualTo(1);
    assertThat(registry.counter("emme.ai.jobs.retries").count()).isEqualTo(1);
    assertThat(registry.counter("emme.ai.jobs.dead_letter").count()).isEqualTo(1);
    assertThat(registry.counter("emme.ai.jobs.tenant.fairness").count()).isEqualTo(1);
  }
}
