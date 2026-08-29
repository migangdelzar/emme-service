package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidateStatus;
import java.util.Objects;
import java.util.UUID;

/** Durable candidate state and optimistic-lock version. */
public record LearningCandidateState(
    UUID candidateId, LearningCandidateStatus status, long version) {

  public LearningCandidateState {
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    if (version < 0) {
      throw new IllegalArgumentException("version must not be negative");
    }
  }
}
