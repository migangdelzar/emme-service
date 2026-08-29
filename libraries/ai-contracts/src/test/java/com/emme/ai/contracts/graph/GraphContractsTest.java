package com.emme.ai.contracts.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GraphContractsTest {

  @Test
  void copiesProjectionCollectionsAndProperties() {
    UUID sourceId = UUID.randomUUID();
    GraphNode source = new GraphNode(GraphNodeType.DESIGN, sourceId, Map.of("name", "minimalist"));
    GraphProjection projection = new GraphProjection(List.of(source), List.of());

    assertThat(projection.nodes()).containsExactly(source);
    assertThat(projection.edges()).isEmpty();
    assertThat(projection.nodes()).isUnmodifiable();
    assertThat(projection.nodes().getFirst().properties()).isUnmodifiable();
  }

  @Test
  void rejectsAnEdgeWhoseEndpointsAreNotDeclaredInTheProjection() {
    UUID sourceId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    GraphNode source = new GraphNode(GraphNodeType.DESIGN, sourceId, Map.of());
    GraphEdge edge =
        new GraphEdge(
            GraphRelationshipType.COMPATIBLE_WITH,
            new GraphNodeReference(GraphNodeType.DESIGN, sourceId),
            new GraphNodeReference(GraphNodeType.SERVICE, targetId),
            Map.of());

    assertThatThrownBy(() -> new GraphProjection(List.of(source), List.of(edge)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("projection edge endpoints must be declared nodes");
  }

  @Test
  void exposesOnlyCuratedTraversalKinds() {
    GraphTraversalQuery query =
        new GraphTraversalQuery(GraphTraversalKind.DESIGN_TO_SERVICE, UUID.randomUUID(), 3);

    assertThat(query.kind()).isEqualTo(GraphTraversalKind.DESIGN_TO_SERVICE);
    assertThat(query.limit()).isEqualTo(3);
  }
}
