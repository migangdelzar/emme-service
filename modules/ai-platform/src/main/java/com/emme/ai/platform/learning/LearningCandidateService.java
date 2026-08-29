package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidate;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;

/** Admits and durably captures learning candidates without promoting them. */
public final class LearningCandidateService {

  private final LearningCandidatePolicy policy;
  private final LearningCandidateStore store;

  public LearningCandidateService(LearningCandidatePolicy policy, LearningCandidateStore store) {
    this.policy = Objects.requireNonNull(policy, "policy must not be null");
    this.store = Objects.requireNonNull(store, "store must not be null");
  }

  public LearningCandidateSubmission submit(LearningCandidate candidate) {
    Objects.requireNonNull(candidate, "candidate must not be null");
    var context = AiExecutionContextScope.requireCurrent();
    LearningCandidateDecision decision = policy.evaluate(candidate);
    if (!decision.accepted()) {
      return LearningCandidateSubmission.rejected(decision.reason());
    }
    return LearningCandidateSubmission.persisted(store.save(candidate, context));
  }
}
