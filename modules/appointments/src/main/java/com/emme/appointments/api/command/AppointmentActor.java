package com.emme.appointments.api.command;

import java.util.Set;
import java.util.UUID;

public record AppointmentActor(
    UUID tenantId, UUID principalId, Set<String> roles, String idempotencyKey) {
  public AppointmentActor {
    if (tenantId == null || principalId == null)
      throw new NullPointerException("actor identity must not be null");
    roles = Set.copyOf(roles == null ? Set.of() : roles);
    if (idempotencyKey == null || idempotencyKey.isBlank())
      throw new IllegalArgumentException("idempotencyKey must not be blank");
  }

  public boolean hasRole(String role) {
    return roles.contains(role) || roles.contains("ROLE_" + role);
  }

  public boolean isStaff() {
    return hasRole("tenant_staff") || hasRole("tenant_owner") || hasRole("admin");
  }
}
