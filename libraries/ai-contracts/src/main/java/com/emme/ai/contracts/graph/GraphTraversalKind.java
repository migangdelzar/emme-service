package com.emme.ai.contracts.graph;

/** Curated relationship traversals; callers cannot provide arbitrary Cypher. */
public enum GraphTraversalKind {
  DESIGN_TO_SERVICE(
      GraphNodeType.DESIGN, GraphRelationshipType.COMPATIBLE_WITH, GraphNodeType.SERVICE);

  private final GraphNodeType sourceType;
  private final GraphRelationshipType relationship;
  private final GraphNodeType targetType;

  GraphTraversalKind(
      GraphNodeType sourceType, GraphRelationshipType relationship, GraphNodeType targetType) {
    this.sourceType = sourceType;
    this.relationship = relationship;
    this.targetType = targetType;
  }

  public GraphNodeType sourceType() {
    return sourceType;
  }

  public GraphRelationshipType relationship() {
    return relationship;
  }

  public GraphNodeType targetType() {
    return targetType;
  }
}
