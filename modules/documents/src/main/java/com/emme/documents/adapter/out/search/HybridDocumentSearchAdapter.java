package com.emme.documents.adapter.out.search;

import com.emme.documents.application.port.out.DocumentSearchHit;
import com.emme.documents.application.port.out.DocumentSearchPort;
import com.emme.shared.search.HybridSearch;
import com.emme.shared.search.SearchTarget;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Adapts the shared PostgreSQL hybrid search capability to Documents. */
@Component
@RequiredArgsConstructor
public class HybridDocumentSearchAdapter implements DocumentSearchPort {

  private final HybridSearch search;

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
