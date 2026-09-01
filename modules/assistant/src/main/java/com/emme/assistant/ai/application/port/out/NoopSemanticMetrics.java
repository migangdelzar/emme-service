package com.emme.assistant.ai.application.port.out;

/** No-op semantic metrics used when the host does not expose Micrometer. */
public final class NoopSemanticMetrics implements SemanticMetrics {
  public static final NoopSemanticMetrics INSTANCE = new NoopSemanticMetrics();

  private NoopSemanticMetrics() {}

  @Override
  public void recordRouting(String outcome) {}

  @Override
  public void recordToolSelection(String outcome) {}

  @Override
  public void recordCacheLookup(String outcome) {}

  @Override
  public void recordCacheWrite(String outcome) {}

  @Override
  public void recordScores(String operation, double top1, double top2, double margin) {}

  @Override
  public void recordLatency(String operation, java.time.Duration duration) {}

  @Override
  public void recordFailure(String operation, String reason) {}

  @Override
  public void recordFallback(String operation, String reason) {}

  @Override
  public void recordInvalidation(String dependency, String scope) {}
}
