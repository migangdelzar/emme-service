package com.emme.ai.contracts.context;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Backend-resolved identity snapshot used to create an AI execution context. */
public record AuthenticatedPrincipal(UUID principalId, Set<String> roles) {

  public AuthenticatedPrincipal {
    principalId = Objects.requireNonNull(principalId, "principalId must not be null");
    roles = Objects.requireNonNull(roles, "roles must not be null");
    if (roles.stream().anyMatch(role -> role == null || role.isBlank())) {
      throw new IllegalArgumentException("roles must not contain blank values");
    }
    roles = Set.copyOf(roles);
  }
}
