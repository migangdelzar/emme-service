package com.emme.ai.contracts.graph;

import java.util.Objects;
import java.util.UUID;

/** Tenant-scoped request for one predefined graph traversal. */
public record GraphTraversalQuery(GraphTraversalKind kind, UUID sourceId, int limit) {

  public GraphTraversalQuery {
    kind = Objects.requireNonNull(kind, "kind must not be null");
    sourceId = Objects.requireNonNull(sourceId, "sourceId must not be null");
    if (limit <= 0 || limit > 50) {
      throw new IllegalArgumentException("limit must be between 1 and 50");
    }
  }
}
