package com.emme.studio.documents.application.port.out;

import java.util.List;
import java.util.UUID;

/** Search capability required by Documents without exposing the search engine. */
public interface DocumentSearchPort {

  List<DocumentSearchHit> search(
      UUID tenantId, List<Float> queryVector, String queryText, int limit);
}
