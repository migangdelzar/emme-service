package com.emme.ai.contracts.graph;

import java.util.Objects;
import java.util.UUID;

/** Stable source-system identity for a derived graph vertex. */
public record GraphNodeReference(GraphNodeType type, UUID sourceId) {

  public GraphNodeReference {
    type = Objects.requireNonNull(type, "type must not be null");
    sourceId = Objects.requireNonNull(sourceId, "sourceId must not be null");
  }
}
