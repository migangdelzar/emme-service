package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidate;
import com.emme.ai.contracts.learning.LearningCandidateEvaluationRequest;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Admits and durably captures learning candidates without promoting them. */
public class LearningCandidateService {

  private final LearningCandidatePolicy policy;
  private final LearningCandidateStore store;
  private final LearningCandidateEvaluationRequester evaluationRequester;

  public LearningCandidateService(LearningCandidatePolicy policy, LearningCandidateStore store) {
    this(policy, store, request -> {});
  }

  public LearningCandidateService(
      LearningCandidatePolicy policy,
      LearningCandidateStore store,
      LearningCandidateEvaluationRequester evaluationRequester) {
    this.policy = Objects.requireNonNull(policy, "policy must not be null");
    this.store = Objects.requireNonNull(store, "store must not be null");
    this.evaluationRequester =
        Objects.requireNonNull(evaluationRequester, "evaluationRequester must not be null");
  }

  @Transactional
  public LearningCandidateSubmission submit(LearningCandidate candidate) {
    Objects.requireNonNull(candidate, "candidate must not be null");
    var context = AiExecutionContextScope.requireCurrent();
    LearningCandidateDecision decision = policy.evaluate(candidate);
    if (!decision.accepted()) {
      return LearningCandidateSubmission.rejected(decision.reason());
    }
    UUID candidateId = store.save(candidate, context);
    evaluationRequester.request(LearningCandidateEvaluationRequest.from(candidateId, context));
    return LearningCandidateSubmission.persisted(candidateId);
  }
}
