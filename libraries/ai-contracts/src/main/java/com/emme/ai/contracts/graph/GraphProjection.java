package com.emme.ai.contracts.graph;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Idempotently replayable batch of derived graph vertices and relationships. */
public record GraphProjection(List<GraphNode> nodes, List<GraphEdge> edges) {

  public GraphProjection {
    nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes must not be null"));
    edges = List.copyOf(Objects.requireNonNull(edges, "edges must not be null"));

    Set<GraphNodeReference> declaredNodes = new HashSet<>();
    for (GraphNode node : nodes) {
      if (!declaredNodes.add(node.reference())) {
        throw new IllegalArgumentException("projection nodes must be unique");
      }
    }
    if (edges.stream()
        .anyMatch(
            edge ->
                !declaredNodes.contains(edge.source()) || !declaredNodes.contains(edge.target()))) {
      throw new IllegalArgumentException("projection edge endpoints must be declared nodes");
    }
  }
}
