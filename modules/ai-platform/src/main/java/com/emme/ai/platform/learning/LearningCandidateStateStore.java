package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidateStatus;
import java.util.Optional;
import java.util.UUID;

/** Tenant-scoped durable state store for governed learning candidates. */
public interface LearningCandidateStateStore {

  Optional<LearningCandidateState> find(UUID candidateId);

  boolean transition(
      UUID candidateId,
      LearningCandidateStatus expectedStatus,
      long expectedVersion,
      LearningCandidateStatus targetStatus,
      String reason);
}
