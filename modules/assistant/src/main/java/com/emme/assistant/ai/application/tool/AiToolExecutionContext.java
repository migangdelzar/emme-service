package com.emme.assistant.ai.application.tool;

import java.util.Set;
import java.util.UUID;

/** Backend-created context passed to a tool handler; no tenant or identity comes from the model. */
public record AiToolExecutionContext(
    UUID tenantId,
    UUID principalId,
    Set<String> roles,
    UUID conversationId,
    UUID workflowId,
    String traceId,
    String idempotencyKey) {

  public AiToolExecutionContext {
    if (tenantId == null) throw new NullPointerException("tenantId must not be null");
    if (principalId == null) throw new NullPointerException("principalId must not be null");
    roles = Set.copyOf(roles == null ? Set.of() : roles);
    if (conversationId == null) throw new NullPointerException("conversationId must not be null");
    if (workflowId == null) throw new NullPointerException("workflowId must not be null");
    requireText(traceId, "traceId");
    requireText(idempotencyKey, "idempotencyKey");
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
