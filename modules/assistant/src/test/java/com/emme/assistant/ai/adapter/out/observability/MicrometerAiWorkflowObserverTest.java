package com.emme.assistant.ai.adapter.out.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MicrometerAiWorkflowObserverTest {

  @Test
  void recordsWorkflowCountersAndDurationWithBoundedLabels() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MicrometerAiWorkflowObserver observer = new MicrometerAiWorkflowObserver(registry);

    observer.workflowStarted("DESIGN_QUOTE");
    observer.workflowFinished("DESIGN_QUOTE", "QUOTE_READY", Duration.ofMillis(25));

    assertThat(
            registry.counter("emme.ai.workflow.started", "workflow_type", "DESIGN_QUOTE").count())
        .isEqualTo(1);
    assertThat(
            registry
                .timer(
                    "emme.ai.workflow.duration",
                    "workflow_type",
                    "DESIGN_QUOTE",
                    "outcome",
                    "QUOTE_READY")
                .count())
        .isEqualTo(1);
  }

  @Test
  void rejectsUnboundedMetricLabelsAndNegativeDurations() {
    MicrometerAiWorkflowObserver observer =
        new MicrometerAiWorkflowObserver(new SimpleMeterRegistry());

    assertThatThrownBy(() -> observer.workflowStarted("tenant-" + "x".repeat(65)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> observer.workflowFinished("DESIGN_QUOTE", "FAILED", Duration.ofSeconds(-1)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
