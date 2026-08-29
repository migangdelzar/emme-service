package com.emme.ai.contracts.graph;

/** Allowlisted AGE vertex labels used by the Emme derived graph. */
public enum GraphNodeType {
  DESIGN("Design"),
  SERVICE("Service"),
  PRODUCT("Product"),
  SKILL("Skill"),
  STAFF_MEMBER("StaffMember"),
  SALON_POLICY("SalonPolicy"),
  CLIENT("Client"),
  PROMOTION("Promotion"),
  DESIGN_STYLE("DesignStyle");

  private final String ageLabel;

  GraphNodeType(String ageLabel) {
    this.ageLabel = ageLabel;
  }

  public String ageLabel() {
    return ageLabel;
  }
}
