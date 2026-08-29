package com.emme.ai.contracts.graph;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Tenant-independent graph vertex payload; tenant scope is supplied by execution context. */
public record GraphNode(GraphNodeType type, UUID sourceId, Map<String, String> properties) {

  public GraphNode {
    type = Objects.requireNonNull(type, "type must not be null");
    sourceId = Objects.requireNonNull(sourceId, "sourceId must not be null");
    properties = copyProperties(properties);
  }

  public GraphNodeReference reference() {
    return new GraphNodeReference(type, sourceId);
  }

  private static Map<String, String> copyProperties(Map<String, String> value) {
    Objects.requireNonNull(value, "properties must not be null");
    if (value.entrySet().stream()
        .anyMatch(
            entry ->
                entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null)) {
      throw new IllegalArgumentException("properties must contain non-blank keys and values");
    }
    return Map.copyOf(value);
  }
}
