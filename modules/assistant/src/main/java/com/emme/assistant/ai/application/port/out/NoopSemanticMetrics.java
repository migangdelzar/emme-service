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
}
