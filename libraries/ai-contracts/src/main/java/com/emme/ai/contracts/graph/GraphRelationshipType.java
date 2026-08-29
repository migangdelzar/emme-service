package com.emme.ai.contracts.graph;

/** Allowlisted AGE edge labels used by the Emme derived graph. */
public enum GraphRelationshipType {
  COMPATIBLE_WITH(GraphNodeType.DESIGN, GraphNodeType.SERVICE),
  REQUIRES_PRODUCT(GraphNodeType.SERVICE, GraphNodeType.PRODUCT),
  REQUIRES_SKILL(GraphNodeType.DESIGN, GraphNodeType.SKILL),
  QUALIFIED_ARTIST(GraphNodeType.SKILL, GraphNodeType.STAFF_MEMBER),
  SUBJECT_TO(GraphNodeType.SERVICE, GraphNodeType.SALON_POLICY),
  PREFERS_STYLE(GraphNodeType.CLIENT, GraphNodeType.DESIGN_STYLE),
  BOOKED_SERVICE(GraphNodeType.CLIENT, GraphNodeType.SERVICE),
  APPLIES_TO(GraphNodeType.PROMOTION, GraphNodeType.SERVICE);

  private final GraphNodeType sourceType;
  private final GraphNodeType targetType;

  GraphRelationshipType(GraphNodeType sourceType, GraphNodeType targetType) {
    this.sourceType = sourceType;
    this.targetType = targetType;
  }

  public GraphNodeType sourceType() {
    return sourceType;
  }

  public GraphNodeType targetType() {
    return targetType;
  }
}
