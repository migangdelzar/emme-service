package com.emme.ai.contracts.graph;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Bounded graph result suitable for recommendation explanation, never authoritative mutation. */
public record GraphRecommendation(
    UUID sourceId,
    UUID targetId,
    GraphNodeType targetType,
    GraphRelationshipType relationship,
    Map<String, String> properties,
    Instant projectedAt) {

  public GraphRecommendation {
    sourceId = Objects.requireNonNull(sourceId, "sourceId must not be null");
    targetId = Objects.requireNonNull(targetId, "targetId must not be null");
    targetType = Objects.requireNonNull(targetType, "targetType must not be null");
    relationship = Objects.requireNonNull(relationship, "relationship must not be null");
    properties = Map.copyOf(Objects.requireNonNull(properties, "properties must not be null"));
    projectedAt = Objects.requireNonNull(projectedAt, "projectedAt must not be null");
  }
}
