package com.emme.assistant.ai.application.port.out;

/** Bounded observability boundary for semantic routing, tool selection, and cache outcomes. */
public interface SemanticMetrics {

  void recordRouting(String outcome);

  void recordToolSelection(String outcome);

  void recordCacheLookup(String outcome);

  void recordCacheWrite(String outcome);
}
