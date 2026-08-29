package com.emme.ai.platform.learning;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Result of candidate admission and durable capture. */
public record LearningCandidateSubmission(
    boolean accepted, Optional<UUID> candidateId, String reason) {

  public LearningCandidateSubmission {
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(reason, "reason must not be null");
    if (reason.isBlank()) {
      throw new IllegalArgumentException("reason must not be blank");
    }
    if (accepted != candidateId.isPresent()) {
      throw new IllegalArgumentException("accepted submissions must have a candidate id");
    }
  }

  public static LearningCandidateSubmission persisted(UUID candidateId) {
    return new LearningCandidateSubmission(
        true, Optional.of(Objects.requireNonNull(candidateId, "candidateId must not be null")), "pending_evaluation");
  }

  public static LearningCandidateSubmission rejected(String reason) {
    return new LearningCandidateSubmission(
        false, Optional.empty(), Objects.requireNonNull(reason, "reason must not be null"));
  }
}
