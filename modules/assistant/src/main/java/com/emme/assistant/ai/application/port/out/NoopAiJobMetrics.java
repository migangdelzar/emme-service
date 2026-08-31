package com.emme.assistant.ai.application.port.out;

import java.time.Duration;

/** No-op metrics implementation used when the host has no Micrometer registry. */
public final class NoopAiJobMetrics implements AiJobMetrics {
  public static final NoopAiJobMetrics INSTANCE = new NoopAiJobMetrics();

  private NoopAiJobMetrics() {}

  @Override
  public void recordQueueDepth(int depth) {}

  @Override
  public void recordQueueLag(Duration lag) {}

  @Override
  public void recordClaimDuration(Duration duration) {}

  @Override
  public void recordClaim(String outcome) {}

  @Override
  public void recordFailure() {}

  @Override
  public void recordRetry() {}

  @Override
  public void recordDeadLetter() {}

  @Override
  public void recordTenantFairness() {}
}
