package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidateStatus;
import java.util.Objects;

/** Result of a deterministic learning-candidate lifecycle transition check. */
public record LearningCandidateLifecycleDecision(
    boolean transitioned, LearningCandidateStatus status, String reason) {

  public LearningCandidateLifecycleDecision {
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(reason, "reason must not be null");
    if (reason.isBlank()) {
      throw new IllegalArgumentException("reason must not be blank");
    }
  }

  public static LearningCandidateLifecycleDecision transitioned(
      LearningCandidateStatus status, String reason) {
    return new LearningCandidateLifecycleDecision(true, status, reason);
  }

  public static LearningCandidateLifecycleDecision rejected(
      LearningCandidateStatus status, String reason) {
    return new LearningCandidateLifecycleDecision(false, status, reason);
  }
}
