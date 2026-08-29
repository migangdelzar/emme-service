package com.emme.ai.platform.learning;

import java.util.Objects;

/** Deterministic admission decision for a learning candidate. */
public record LearningCandidateDecision(boolean accepted, String reason) {

  public LearningCandidateDecision {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("reason must not be blank");
    }
  }

  public static LearningCandidateDecision admitted() {
    return new LearningCandidateDecision(true, "eligible");
  }

  public static LearningCandidateDecision rejected(String reason) {
    return new LearningCandidateDecision(
        false, Objects.requireNonNull(reason, "reason must not be null"));
  }
}
