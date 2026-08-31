package com.emme.assistant.ai.domain.workflow;

import java.util.Objects;
import java.util.UUID;

/** Trusted identifiers and current persisted state of a conversation workflow. */
public record ConversationWorkflowSnapshot(
    UUID workflowId,
    UUID conversationId,
    ConversationWorkflowStatus status,
    UUID tenantId,
    UUID principalId,
    String message,
    String idempotencyKey,
    String response) {

  public ConversationWorkflowSnapshot {
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(conversationId, "conversationId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    Objects.requireNonNull(principalId, "principalId must not be null");
    message = requireText(message, "message");
    idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
    if (status == ConversationWorkflowStatus.SUCCEEDED) {
      response = requireText(response, "response");
    }
  }

  public ConversationWorkflowSnapshot(
      UUID workflowId,
      UUID conversationId,
      ConversationWorkflowStatus status,
      UUID tenantId,
      UUID principalId) {
    this(workflowId, conversationId, status, tenantId, principalId, "workflow", "workflow", null);
  }

  public ConversationWorkflowSnapshot(
      UUID workflowId, UUID conversationId, ConversationWorkflowStatus status) {
    this(
        workflowId,
        conversationId,
        status,
        new UUID(0L, 0L),
        new UUID(0L, 0L),
        "workflow",
        "workflow",
        status == ConversationWorkflowStatus.SUCCEEDED ? "workflow" : null);
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
