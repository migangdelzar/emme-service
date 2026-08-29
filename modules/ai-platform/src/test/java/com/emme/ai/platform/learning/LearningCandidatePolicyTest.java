package com.emme.ai.platform.learning;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.learning.LearningCandidate;
import com.emme.ai.contracts.learning.LearningCandidateEvidence;
import com.emme.ai.contracts.learning.LearningCandidateKind;
import org.junit.jupiter.api.Test;

class LearningCandidatePolicyTest {

  private final LearningCandidatePolicy policy = new LearningCandidatePolicy();

  @Test
  void admitsOnlyAValidatedRedactedAcceptedOutcome() {
    LearningCandidate candidate = candidate(evidence(true, true, true, true, false, false, true));

    assertThat(policy.evaluate(candidate)).isEqualTo(LearningCandidateDecision.admitted());
  }

  @Test
  void rejectsACompletedRouteThatWasNotAcceptedByTheUserOrWorkflow() {
    LearningCandidate candidate = candidate(evidence(true, true, true, false, false, false, true));

    assertThat(policy.evaluate(candidate))
        .isEqualTo(LearningCandidateDecision.rejected("accepted outcome evidence is required"));
  }

  @Test
  void rejectsAnUnredactedCandidateBeforeItCanReachAnEmbeddingPipeline() {
    LearningCandidate candidate = candidate(evidence(true, true, true, true, false, false, false));

    assertThat(policy.evaluate(candidate))
        .isEqualTo(LearningCandidateDecision.rejected("candidate text must be PII-redacted"));
  }

  @Test
  void rejectsAStaffCorrectedOutcome() {
    LearningCandidate candidate = candidate(evidence(true, true, true, true, true, false, true));

    assertThat(policy.evaluate(candidate))
        .isEqualTo(LearningCandidateDecision.rejected("staff-corrected outcomes are not eligible"));
  }

  @Test
  void rejectsAPlatformPolicyViolationEvenWhenTheOutcomeLooksSuccessful() {
    LearningCandidate candidate = candidate(evidence(true, true, true, true, false, true, true));

    assertThat(policy.evaluate(candidate))
        .isEqualTo(
            LearningCandidateDecision.rejected("policy-violating outcomes are not eligible"));
  }

  @Test
  void rejectsAUnsuccessfulExecutionBeforePromotion() {
    LearningCandidate candidate = candidate(evidence(true, false, true, true, false, false, true));

    assertThat(policy.evaluate(candidate))
        .isEqualTo(LearningCandidateDecision.rejected("execution must succeed"));
  }

  private static LearningCandidate candidate(LearningCandidateEvidence evidence) {
    return new LearningCandidate(
        "intent:es-MX:service-information",
        LearningCandidateKind.INTENT_EXAMPLE,
        "what services do you offer?",
        "es-MX",
        "embeddinggemma:1",
        evidence);
  }

  private static LearningCandidateEvidence evidence(
      boolean routeAccepted,
      boolean executionSucceeded,
      boolean outcomeValidated,
      boolean acceptedOutcome,
      boolean staffCorrected,
      boolean policyViolation,
      boolean piiRedacted) {
    return new LearningCandidateEvidence(
        routeAccepted,
        executionSucceeded,
        outcomeValidated,
        acceptedOutcome,
        staffCorrected,
        policyViolation,
        piiRedacted);
  }
}
