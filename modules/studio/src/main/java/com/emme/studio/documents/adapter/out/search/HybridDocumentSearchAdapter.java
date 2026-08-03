package com.emme.studio.documents.adapter.out.search;

import com.emme.shared.search.HybridSearch;
import com.emme.shared.search.SearchTarget;
import com.emme.studio.documents.application.port.out.DocumentSearchHit;
import com.emme.studio.documents.application.port.out.DocumentSearchPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adapts the shared PostgreSQL hybrid search capability to Documents. */
@Component
public class HybridDocumentSearchAdapter implements DocumentSearchPort {

  private final HybridSearch search;

  public HybridDocumentSearchAdapter(HybridSearch search) {
    this.search = search;
  }

  @Override
  public List<DocumentSearchHit> search(
      UUID tenantId, List<Float> queryVector, String queryText, int limit) {
    return search
        .search(SearchTarget.DOCUMENT_CHUNK, tenantId, queryVector, queryText, limit)
        .stream()
        .map(result -> new DocumentSearchHit(result.id(), result.score()))
        .toList();
  }
}
