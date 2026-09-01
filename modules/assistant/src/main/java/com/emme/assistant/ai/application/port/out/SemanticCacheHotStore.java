package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.application.semantic.SemanticCacheInvalidation;
import java.util.List;
import java.util.UUID;

/** Optional low-latency semantic cache projection; PostgreSQL remains authoritative. */
public interface SemanticCacheHotStore {

  /** Finds hot candidates for a query. Implementations must enforce the bound AI context. */
  List<SemanticCachePort.Candidate> find(
      SemanticCachePort.Lookup lookup, String queryText, int limit);

  /** Projects an already-persisted durable cache entry into the hot store. */
  void put(UUID durableCacheId, SemanticCachePort.Put write);

  /** Removes the scoped hot projection when the durable dependency changes. */
  default void invalidate(SemanticCacheInvalidation invalidation) {}
}
