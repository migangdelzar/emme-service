package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidateEvaluation;
import com.emme.ai.contracts.learning.LearningCandidateStatus;
import java.util.Objects;

/** Deterministic state and promotion gates for offline learning candidates. */
public final class LearningCandidateLifecyclePolicy {

  public LearningCandidateLifecycleDecision start(LearningCandidateStatus currentStatus) {
    Objects.requireNonNull(currentStatus, "currentStatus must not be null");
    if (currentStatus != LearningCandidateStatus.PENDING_EVALUATION) {
      return LearningCandidateLifecycleDecision.rejected(
          currentStatus, "candidate is not pending evaluation");
    }
    return LearningCandidateLifecycleDecision.transitioned(
        LearningCandidateStatus.EVALUATING, "evaluation started");
  }

  public LearningCandidateLifecycleDecision completeEvaluation(
      LearningCandidateStatus currentStatus, LearningCandidateEvaluation evaluation) {
    Objects.requireNonNull(currentStatus, "currentStatus must not be null");
    Objects.requireNonNull(evaluation, "evaluation must not be null");
    if (currentStatus != LearningCandidateStatus.EVALUATING) {
      return LearningCandidateLifecycleDecision.rejected(
          currentStatus, "candidate is not being evaluated");
    }
    if (!evaluation.datasetComplete()) {
      return LearningCandidateLifecycleDecision.rejected(
          LearningCandidateStatus.REJECTED, "evaluation dataset is incomplete");
    }
    if (!evaluation.safetyPassed()) {
      return LearningCandidateLifecycleDecision.rejected(
          LearningCandidateStatus.REJECTED, "deterministic safety checks failed");
    }
    if (!evaluation.regressionPassed()) {
      return LearningCandidateLifecycleDecision.rejected(
          LearningCandidateStatus.REJECTED, "regression evaluation failed");
    }
    if (!evaluation.shadowComparisonPassed()) {
      return LearningCandidateLifecycleDecision.rejected(
          LearningCandidateStatus.REJECTED, "shadow comparison failed");
    }
    return LearningCandidateLifecycleDecision.transitioned(
        LearningCandidateStatus.APPROVED, "evaluation passed");
  }

  public LearningCandidateLifecycleDecision promote(
      LearningCandidateStatus currentStatus, LearningCandidateEvaluation evaluation) {
    Objects.requireNonNull(currentStatus, "currentStatus must not be null");
    Objects.requireNonNull(evaluation, "evaluation must not be null");
    if (currentStatus != LearningCandidateStatus.APPROVED) {
      return LearningCandidateLifecycleDecision.rejected(
          currentStatus, "candidate is not approved");
    }
    if (!evaluation.datasetComplete()
        || !evaluation.safetyPassed()
        || !evaluation.regressionPassed()
        || !evaluation.shadowComparisonPassed()) {
      return LearningCandidateLifecycleDecision.rejected(
          LearningCandidateStatus.APPROVED, "evaluation gates are incomplete");
    }
    if (!evaluation.canaryPassed()) {
      return LearningCandidateLifecycleDecision.rejected(
          LearningCandidateStatus.APPROVED, "canary evaluation failed");
    }
    return LearningCandidateLifecycleDecision.transitioned(
        LearningCandidateStatus.PROMOTED, "promotion passed canary");
  }

  public LearningCandidateLifecycleDecision rollback(LearningCandidateStatus currentStatus) {
    Objects.requireNonNull(currentStatus, "currentStatus must not be null");
    if (currentStatus != LearningCandidateStatus.PROMOTED) {
      return LearningCandidateLifecycleDecision.rejected(
          currentStatus, "candidate is not promoted");
    }
    return LearningCandidateLifecycleDecision.transitioned(
        LearningCandidateStatus.ROLLED_BACK, "promotion rolled back");
  }
}
