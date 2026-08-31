package com.emme.assistant.ai.application.port.out;

import java.time.Duration;

/** Metrics boundary for durable AI job lifecycle and scheduler fairness telemetry. */
public interface AiJobMetrics {
  void recordQueueDepth(int depth);

  void recordQueueLag(Duration lag);

  void recordClaimDuration(Duration duration);

  void recordClaim(String outcome);

  void recordFailure();

  void recordRetry();

  void recordDeadLetter();

  void recordTenantFairness();
}
