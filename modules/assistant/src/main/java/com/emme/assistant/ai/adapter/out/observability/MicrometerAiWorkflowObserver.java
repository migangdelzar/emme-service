package com.emme.assistant.ai.adapter.out.observability;

import com.emme.assistant.ai.application.port.out.AiWorkflowObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Objects;

/** Micrometer adapter with bounded workflow and outcome labels. */
public final class MicrometerAiWorkflowObserver implements AiWorkflowObserver {

  private final MeterRegistry registry;

  public MicrometerAiWorkflowObserver(MeterRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry must not be null");
  }

  @Override
  public void workflowStarted(String workflowType) {
    Counter.builder("emme.ai.workflow.started")
        .description("AI workflows started")
        .tag("workflow_type", label(workflowType, "workflowType"))
        .register(registry)
        .increment();
  }

  @Override
  public void workflowFinished(String workflowType, String outcome, Duration duration) {
    Objects.requireNonNull(duration, "duration must not be null");
    if (duration.isNegative()) {
      throw new IllegalArgumentException("duration must not be negative");
    }
    Timer.builder("emme.ai.workflow.duration")
        .description("AI workflow duration")
        .tag("workflow_type", label(workflowType, "workflowType"))
        .tag("outcome", label(outcome, "outcome"))
        .publishPercentileHistogram()
        .register(registry)
        .record(duration);
  }

  private static String label(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank() || value.length() > 64 || !value.matches("[A-Za-z0-9_.-]+")) {
      throw new IllegalArgumentException(field + " must be a bounded metric label");
    }
    return value;
  }
}
