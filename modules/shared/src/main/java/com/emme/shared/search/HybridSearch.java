package com.emme.shared.search;

import java.util.List;
import java.util.UUID;

/** Provider-neutral hybrid retrieval capability. */
public interface HybridSearch {

  record Scored(UUID id, double score) {}

  List<Scored> search(
      SearchTarget target, UUID tenantId, List<Float> queryVector, String queryText, int limit);

  int updateEmbedding(SearchTarget target, UUID tenantId, UUID rowId, List<Float> vector);

  List<UUID> idsMissingEmbedding(SearchTarget target, UUID tenantId, int limit);

  long countMissingEmbedding(SearchTarget target, UUID tenantId);
}
