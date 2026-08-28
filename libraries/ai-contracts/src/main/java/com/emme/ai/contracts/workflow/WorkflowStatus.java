package com.emme.ai.contracts.workflow;

/** Explicit lifecycle states for durable AI workflows. */
public enum WorkflowStatus {
  CREATED,
  RUNNING,
  WAITING_FOR_APPROVAL,
  CLARIFICATION_REQUIRED,
  REJECTED,
  SUCCEEDED,
  FAILED;

  public boolean isTerminal() {
    return this == SUCCEEDED || this == FAILED || this == REJECTED;
  }
}
