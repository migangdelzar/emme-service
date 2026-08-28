package com.emme.ai.contracts.workflow;

/** Controlled reasons for pausing a workflow. */
public enum InterruptReason {
  HUMAN_APPROVAL_REQUIRED,
  CLARIFICATION_REQUIRED,
  EXTERNAL_DEPENDENCY_UNAVAILABLE
}
