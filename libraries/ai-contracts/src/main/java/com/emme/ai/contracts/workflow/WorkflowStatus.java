package com.emme.ai.contracts.workflow;

/** Explicit lifecycle states for durable AI workflows. */
public enum WorkflowStatus {
  CREATED,
  RUNNING,
  WAITING_FOR_APPROVAL,
  WAITING_FOR_CONFIRMATION,
  WAITING_FOR_PAYMENT,
  CLARIFICATION_REQUIRED,
  REJECTED,
  SUCCEEDED,
  FAILED;

  public boolean isTerminal() {
    return this == SUCCEEDED || this == FAILED || this == REJECTED;
  }
}
