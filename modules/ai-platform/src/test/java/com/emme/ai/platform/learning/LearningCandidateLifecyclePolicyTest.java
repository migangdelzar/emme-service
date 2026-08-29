package com.emme.ai.platform.learning;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.learning.LearningCandidateEvaluation;
import com.emme.ai.contracts.learning.LearningCandidateStatus;
import org.junit.jupiter.api.Test;

class LearningCandidateLifecyclePolicyTest {

  private final LearningCandidateLifecyclePolicy policy = new LearningCandidateLifecyclePolicy();

  @Test
  void startsOnlyPendingCandidatesAsEvaluating() {
    assertThat(policy.start(LearningCandidateStatus.PENDING_EVALUATION))
        .isEqualTo(
            LearningCandidateLifecycleDecision.transitioned(
                LearningCandidateStatus.EVALUATING, "evaluation started"));
  }

  @Test
  void approvesOnlyCandidatesThatPassEveryOfflineGate() {
    assertThat(policy.completeEvaluation(LearningCandidateStatus.EVALUATING, passingEvaluation()))
        .isEqualTo(
            LearningCandidateLifecycleDecision.transitioned(
                LearningCandidateStatus.APPROVED, "evaluation passed"));
  }

  @Test
  void rejectsAnIncompleteDatasetBeforeAnyPromotion() {
    LearningCandidateEvaluation evaluation =
        new LearningCandidateEvaluation("eval-1", false, true, true, true, true);

    assertThat(policy.completeEvaluation(LearningCandidateStatus.EVALUATING, evaluation))
        .isEqualTo(
            LearningCandidateLifecycleDecision.rejected(
                LearningCandidateStatus.REJECTED, "evaluation dataset is incomplete"));
  }

  @Test
  void rejectsSafetyRegressionEvenWhenQualityGatesPass() {
    LearningCandidateEvaluation evaluation =
        new LearningCandidateEvaluation("eval-1", true, false, true, true, true);

    assertThat(policy.completeEvaluation(LearningCandidateStatus.EVALUATING, evaluation))
        .isEqualTo(
            LearningCandidateLifecycleDecision.rejected(
                LearningCandidateStatus.REJECTED, "deterministic safety checks failed"));
  }

  @Test
  void requiresASeparateCanaryBeforePromotion() {
    LearningCandidateEvaluation evaluation =
        new LearningCandidateEvaluation("eval-1", true, true, true, true, false);

    assertThat(policy.promote(LearningCandidateStatus.APPROVED, evaluation))
        .isEqualTo(
            LearningCandidateLifecycleDecision.rejected(
                LearningCandidateStatus.APPROVED, "canary evaluation failed"));
  }

  @Test
  void doesNotPromoteWhenThePromotionEvaluationLacksEarlierGates() {
    LearningCandidateEvaluation evaluation =
        new LearningCandidateEvaluation("eval-1", false, false, false, false, true);

    assertThat(policy.promote(LearningCandidateStatus.APPROVED, evaluation))
        .isEqualTo(
            LearningCandidateLifecycleDecision.rejected(
                LearningCandidateStatus.APPROVED, "evaluation gates are incomplete"));
  }

  @Test
  void rollsBackOnlyPromotedCandidates() {
    assertThat(policy.rollback(LearningCandidateStatus.PROMOTED))
        .isEqualTo(
            LearningCandidateLifecycleDecision.transitioned(
                LearningCandidateStatus.ROLLED_BACK, "promotion rolled back"));
  }

  private static LearningCandidateEvaluation passingEvaluation() {
    return new LearningCandidateEvaluation("eval-1", true, true, true, true, true);
  }
}
