package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidate;
import com.emme.ai.contracts.learning.LearningCandidateEvidence;
import java.util.Objects;

/**
 * Applies the fail-closed evidence gate before an example can enter evaluation.
 *
 * <p>This policy only admits candidates; it never promotes or writes an embedding index.
 */
public final class LearningCandidatePolicy {

  public LearningCandidateDecision evaluate(LearningCandidate candidate) {
    Objects.requireNonNull(candidate, "candidate must not be null");
    LearningCandidateEvidence evidence = candidate.evidence();
    if (!evidence.piiRedacted()) {
      return LearningCandidateDecision.rejected("candidate text must be PII-redacted");
    }
    if (evidence.policyViolation()) {
      return LearningCandidateDecision.rejected("policy-violating outcomes are not eligible");
    }
    if (evidence.staffCorrected()) {
      return LearningCandidateDecision.rejected("staff-corrected outcomes are not eligible");
    }
    if (!evidence.routeAccepted()) {
      return LearningCandidateDecision.rejected("route must be accepted");
    }
    if (!evidence.executionSucceeded()) {
      return LearningCandidateDecision.rejected("execution must succeed");
    }
    if (!evidence.outcomeValidated()) {
      return LearningCandidateDecision.rejected("outcome must be validated");
    }
    if (!evidence.acceptedOutcome()) {
      return LearningCandidateDecision.rejected("accepted outcome evidence is required");
    }
    return LearningCandidateDecision.admitted();
  }
}
