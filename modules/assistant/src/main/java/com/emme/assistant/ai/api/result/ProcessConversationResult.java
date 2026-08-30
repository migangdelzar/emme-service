package com.emme.assistant.ai.api.result;

import com.emme.assistant.ai.domain.workflow.ConversationWorkflowStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;

/** Persisted identifiers and validated response for one AI conversation turn. */
public record ProcessConversationResult(
    UUID conversationId,
    UUID workflowId,
    String response,
    ConversationWorkflowStatus workflowStatus) {

  public ProcessConversationResult(UUID conversationId, UUID workflowId, String response) {
    this(conversationId, workflowId, response, ConversationWorkflowStatus.SUCCEEDED);
  }

  @JsonIgnore
  public boolean isWaiting() {
    return workflowStatus == ConversationWorkflowStatus.WAITING_FOR_APPROVAL
        || workflowStatus == ConversationWorkflowStatus.WAITING_FOR_CONFIRMATION
        || workflowStatus == ConversationWorkflowStatus.CLARIFICATION_REQUIRED;
  }
}
