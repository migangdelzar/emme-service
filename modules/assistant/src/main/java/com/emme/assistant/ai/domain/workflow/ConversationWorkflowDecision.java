package com.emme.assistant.ai.domain.workflow;

/** Explicit human or client decision that continues a paused conversation workflow. */
public enum ConversationWorkflowDecision {
  APPROVE,
  REQUEST_CONFIRMATION,
  REQUEST_CLARIFICATION,
  REJECT
}
