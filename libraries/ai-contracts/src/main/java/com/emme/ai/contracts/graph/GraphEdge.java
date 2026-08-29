package com.emme.ai.contracts.graph;

import java.util.Map;
import java.util.Objects;

/** Allowlisted directed graph relationship between two source-system vertices. */
public record GraphEdge(
    GraphRelationshipType relationship,
    GraphNodeReference source,
    GraphNodeReference target,
    Map<String, String> properties) {

  public GraphEdge {
    relationship = Objects.requireNonNull(relationship, "relationship must not be null");
    source = Objects.requireNonNull(source, "source must not be null");
    target = Objects.requireNonNull(target, "target must not be null");
    properties = copyProperties(properties);
    if (source.type() != relationship.sourceType() || target.type() != relationship.targetType()) {
      throw new IllegalArgumentException("relationship endpoints do not match the allowlist");
    }
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
