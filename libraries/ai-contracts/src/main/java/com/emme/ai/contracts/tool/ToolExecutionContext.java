package com.emme.ai.contracts.tool;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Backend-created context; tenant, principal, and roles are never model-controlled arguments. */
public record ToolExecutionContext(
    UUID tenantId,
    UUID principalId,
    Set<String> roles,
    UUID conversationId,
    UUID workflowId,
    String traceId,
    String idempotencyKey) {

  public ToolExecutionContext {
    tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    principalId = Objects.requireNonNull(principalId, "principalId must not be null");
    roles = Objects.requireNonNull(roles, "roles must not be null");
    if (roles.stream().anyMatch(role -> role == null || role.isBlank())) {
      throw new IllegalArgumentException("roles must not contain blank values");
    }
    roles = Set.copyOf(roles);
    conversationId = Objects.requireNonNull(conversationId, "conversationId must not be null");
    workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
    traceId = requireText(traceId, "traceId");
    idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
