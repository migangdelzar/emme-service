package com.emme.assistant.ai.api.command;

import com.emme.assistant.ai.domain.workflow.ConversationWorkflowDecision;
import java.util.Objects;
import java.util.UUID;

/** Authenticated decision used to resume a persisted conversation workflow. */
public record ResumeConversationWorkflowCommand(
    UUID workflowId, UUID conversationId, ConversationWorkflowDecision decision) {

  public ResumeConversationWorkflowCommand {
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(conversationId, "conversationId must not be null");
    Objects.requireNonNull(decision, "decision must not be null");
  }
}
