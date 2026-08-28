package com.emme.assistant.ai.domain.workflow;

/** Durable states of the design-quote workflow, including its HITL pause. */
public enum QuoteWorkflowState {
  RECEIVED,
  EXTRACTING,
  QUOTE_CALCULATED,
  NEEDS_STAFF_REVIEW,
  WAITING_FOR_STAFF,
  STAFF_APPROVED,
  STAFF_EDITED,
  QUOTE_READY,
  SENT_TO_CLIENT,
  FAILED
}
