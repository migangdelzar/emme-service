package com.emme.assistant.ai.application.port.out;

import java.time.Duration;

/** Bounded observability boundary for semantic routing, tool selection, and cache outcomes. */
public interface SemanticMetrics {

  void recordRouting(String outcome);

  void recordToolSelection(String outcome);

  void recordCacheLookup(String outcome);

  void recordCacheWrite(String outcome);

  default void recordScores(String operation, double top1, double top2, double margin) {}

  default void recordLatency(String operation, Duration duration) {}

  default void recordFailure(String operation, String reason) {}

  default void recordFallback(String operation, String reason) {}

  default void recordInvalidation(String dependency, String scope) {}
}
