package com.emme.assistant.ai.api.command;

import com.emme.assistant.ai.domain.workflow.ConversationWorkflowDecision;
import java.util.Objects;
import java.util.UUID;

/** Authenticated decision used to resume a persisted conversation workflow. */
public record ResumeConversationWorkflowCommand(
    UUID workflowId,
    UUID conversationId,
    ConversationWorkflowDecision decision,
    WorkflowClarificationCommand clarification) {

  public ResumeConversationWorkflowCommand(
      UUID workflowId, UUID conversationId, ConversationWorkflowDecision decision) {
    this(workflowId, conversationId, decision, null);
  }

  public ResumeConversationWorkflowCommand {
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(conversationId, "conversationId must not be null");
    Objects.requireNonNull(decision, "decision must not be null");
    if (decision == ConversationWorkflowDecision.PROVIDE_CLARIFICATION && clarification == null) {
      throw new IllegalArgumentException("clarification is required when providing clarification");
    }
    if (decision != ConversationWorkflowDecision.PROVIDE_CLARIFICATION && clarification != null) {
      throw new IllegalArgumentException(
          "clarification is only valid when providing clarification");
    }
  }
}
