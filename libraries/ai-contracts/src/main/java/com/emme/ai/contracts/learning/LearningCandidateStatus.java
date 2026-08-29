package com.emme.ai.contracts.learning;

/** Durable lifecycle states for a governed learning candidate. */
public enum LearningCandidateStatus {
  PENDING_EVALUATION,
  EVALUATING,
  REJECTED,
  APPROVED,
  PROMOTED,
  ROLLED_BACK
}
