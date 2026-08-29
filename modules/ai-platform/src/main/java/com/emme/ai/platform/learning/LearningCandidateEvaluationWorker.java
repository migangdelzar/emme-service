package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidateEvaluationReport;
import com.emme.ai.contracts.learning.LearningCandidateStatus;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Applies an offline evaluation result through the durable candidate lifecycle. */
public class LearningCandidateEvaluationWorker {

  private final LearningCandidateLifecycleService lifecycle;
  private final LearningCandidateEvaluationStore evaluations;

  public LearningCandidateEvaluationWorker(LearningCandidateLifecycleService lifecycle) {
    this(lifecycle, (candidateId, report, context) -> null);
  }

  public LearningCandidateEvaluationWorker(
      LearningCandidateLifecycleService lifecycle, LearningCandidateEvaluationStore evaluations) {
    this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
    this.evaluations = Objects.requireNonNull(evaluations, "evaluations must not be null");
  }

  /**
   * Processes one result using only the backend-bound tenant context.
   *
   * <p>A repeated delivery after a candidate has already left {@code EVALUATING} is a safe no-op.
   * Promotion is intentionally not part of evaluation processing.
   */
  @Transactional
  public LearningCandidateState process(
      UUID candidateId, LearningCandidateEvaluationReport report) {
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(report, "report must not be null");
    AiExecutionContextScope.requireCurrent();

    LearningCandidateState state = lifecycle.beginEvaluation(candidateId);
    if (state.status() != LearningCandidateStatus.EVALUATING) {
      return state;
    }
    var context = AiExecutionContextScope.requireCurrent();
    evaluations.save(candidateId, report, context);
    return lifecycle.completeEvaluation(candidateId, report.toLifecycleEvaluation());
  }
}
