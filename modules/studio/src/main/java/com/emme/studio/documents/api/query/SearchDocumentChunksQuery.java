package com.emme.studio.documents.api.query;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Requests tenant-scoped hybrid retrieval of document chunks. */
public record SearchDocumentChunksQuery(
    UUID tenantId, List<Float> queryVector, String queryText, int limit) {

  public SearchDocumentChunksQuery {
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    queryVector = queryVector == null ? List.of() : List.copyOf(queryVector);
    if (queryText == null || queryText.isBlank()) {
      throw new IllegalArgumentException("queryText must not be blank");
    }
    if (limit < 1 || limit > 20) {
      throw new IllegalArgumentException("limit must be between 1 and 20");
    }
  }
}
