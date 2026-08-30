package com.emme.assistant.ai.domain.workflow;

/** Durable lifecycle states exposed by the generic AI conversation workflow. */
public enum ConversationWorkflowStatus {
  RECEIVED,
  RUNNING,
  WAITING_FOR_CONFIRMATION,
  WAITING_FOR_APPROVAL,
  CLARIFICATION_REQUIRED,
  SUCCEEDED,
  REJECTED,
  FAILED
}
