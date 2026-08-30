package com.emme.assistant.ai.domain.workflow;

import java.util.Objects;
import java.util.UUID;

/** Trusted identifiers and current persisted state of a conversation workflow. */
public record ConversationWorkflowSnapshot(
    UUID workflowId, UUID conversationId, ConversationWorkflowStatus status) {

  public ConversationWorkflowSnapshot {
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(conversationId, "conversationId must not be null");
    Objects.requireNonNull(status, "status must not be null");
  }
}
