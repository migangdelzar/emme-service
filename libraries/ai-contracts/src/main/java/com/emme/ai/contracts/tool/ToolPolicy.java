package com.emme.ai.contracts.tool;

import java.util.Objects;
import java.util.Set;

/** Backend policy governing who may invoke a tool and under which confirmation gates. */
public record ToolPolicy(
    Set<String> allowedRoles,
    ToolRisk risk,
    boolean userConfirmationRequired,
    boolean staffApprovalRequired) {

  public ToolPolicy {
    Objects.requireNonNull(allowedRoles, "allowedRoles must not be null");
    if (allowedRoles.isEmpty()
        || allowedRoles.stream().anyMatch(role -> role == null || role.isBlank())) {
      throw new IllegalArgumentException("allowedRoles must contain nonblank values");
    }
    allowedRoles = Set.copyOf(allowedRoles);
    risk = Objects.requireNonNull(risk, "risk must not be null");
  }

  public boolean isAuthorized(Set<String> roles) {
    return roles != null && roles.stream().anyMatch(allowedRoles::contains);
  }

  public boolean canRunProactively() {
    return risk == ToolRisk.READ_ONLY && !userConfirmationRequired && !staffApprovalRequired;
  }
}
