package com.emme.ai.contracts.guardrail;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Trusted identity facts presented to the context guard. */
public record ContextRequest(
    UUID tenantId, UUID principalId, Set<String> roles, String traceId, Instant deadline) {

  public ContextRequest {
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    Objects.requireNonNull(principalId, "principalId must not be null");
    Objects.requireNonNull(roles, "roles must not be null");
    if (roles.stream().anyMatch(role -> role == null || role.isBlank())) {
      throw new IllegalArgumentException("roles must not contain blank values");
    }
    roles = Set.copyOf(roles);
    requireText(traceId, "traceId");
    Objects.requireNonNull(deadline, "deadline must not be null");
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
  }
}
