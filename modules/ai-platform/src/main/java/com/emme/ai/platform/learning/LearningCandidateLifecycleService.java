package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidateEvaluation;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.UUID;

/** Applies deterministic candidate lifecycle decisions with optimistic persistence. */
public final class LearningCandidateLifecycleService {

  private final LearningCandidateLifecyclePolicy policy;
  private final LearningCandidateStateStore stateStore;

  public LearningCandidateLifecycleService(
      LearningCandidateLifecyclePolicy policy, LearningCandidateStateStore stateStore) {
    this.policy = Objects.requireNonNull(policy, "policy must not be null");
    this.stateStore = Objects.requireNonNull(stateStore, "stateStore must not be null");
  }

  public LearningCandidateState beginEvaluation(UUID candidateId) {
    return apply(candidateId, state -> policy.start(state.status()));
  }

  public LearningCandidateState completeEvaluation(
      UUID candidateId, LearningCandidateEvaluation evaluation) {
    Objects.requireNonNull(evaluation, "evaluation must not be null");
    return apply(candidateId, state -> policy.completeEvaluation(state.status(), evaluation));
  }

  public LearningCandidateState promote(UUID candidateId, LearningCandidateEvaluation evaluation) {
    Objects.requireNonNull(evaluation, "evaluation must not be null");
    return apply(candidateId, state -> policy.promote(state.status(), evaluation));
  }

  public LearningCandidateState rollback(UUID candidateId) {
    return apply(candidateId, state -> policy.rollback(state.status()));
  }

  private LearningCandidateState apply(
      UUID candidateId,
      java.util.function.Function<LearningCandidateState, LearningCandidateLifecycleDecision>
          decisionFunction) {
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    AiExecutionContextScope.requireCurrent();
    LearningCandidateState current =
        stateStore
            .find(candidateId)
            .orElseThrow(() -> new IllegalArgumentException("Learning candidate was not found"));
    LearningCandidateLifecycleDecision decision = decisionFunction.apply(current);
    if (decision.status() == current.status()) {
      return current;
    }
    if (!stateStore.transition(
        candidateId, current.status(), current.version(), decision.status(), decision.reason())) {
      throw new LearningCandidateConcurrencyException();
    }
    return new LearningCandidateState(candidateId, decision.status(), current.version() + 1);
  }
}
