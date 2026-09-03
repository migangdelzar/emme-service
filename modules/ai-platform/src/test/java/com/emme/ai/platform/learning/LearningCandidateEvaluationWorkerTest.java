package com.emme.ai.platform.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.learning.LearningCandidateEvaluationReport;
import com.emme.ai.contracts.learning.LearningCandidateStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LearningCandidateEvaluationWorkerTest {

  private static final AiExecutionContext CONTEXT =
      new AiExecutionContext(
          UUID.randomUUID(),
          UUID.randomUUID(),
          Set.of("ROLE_SYSTEM_EVALUATOR"),
          UUID.randomUUID(),
          UUID.randomUUID(),
          "trace-eval",
          "idempotency-eval");

  @Test
  void requiresTheDurableEvaluationStoreAtConstruction() {
    assertThat(Arrays.stream(LearningCandidateEvaluationWorker.class.getDeclaredConstructors()))
        .noneMatch(constructor -> constructor.getParameterCount() == 1);
  }

  @Test
  void startsAndCompletesAPendingCandidate() {
    UUID candidateId = UUID.randomUUID();
    LearningCandidateLifecycleService lifecycle = mock(LearningCandidateLifecycleService.class);
    LearningCandidateEvaluationStore evaluations = mock(LearningCandidateEvaluationStore.class);
    LearningCandidateState evaluating =
        new LearningCandidateState(candidateId, LearningCandidateStatus.EVALUATING, 1);
    LearningCandidateState approved =
        new LearningCandidateState(candidateId, LearningCandidateStatus.APPROVED, 2);
    LearningCandidateEvaluationReport evaluation = passingEvaluation();
    when(lifecycle.beginEvaluation(candidateId)).thenReturn(evaluating);
    when(lifecycle.completeEvaluation(candidateId, evaluation.toLifecycleEvaluation()))
        .thenReturn(approved);
    LearningCandidateEvaluationWorker worker =
        new LearningCandidateEvaluationWorker(lifecycle, evaluations);

    LearningCandidateState result =
        AiExecutionContextScope.call(context(), () -> worker.process(candidateId, evaluation));

    assertThat(result).isEqualTo(approved);
    verify(lifecycle).beginEvaluation(candidateId);
    verify(evaluations).save(candidateId, evaluation, context());
    verify(lifecycle).completeEvaluation(candidateId, evaluation.toLifecycleEvaluation());
    verify(lifecycle, never()).promote(candidateId, evaluation.toLifecycleEvaluation());
  }

  @Test
  void doesNotReapplyAnEvaluationAfterTheCandidateAlreadyReachedATerminalState() {
    UUID candidateId = UUID.randomUUID();
    LearningCandidateLifecycleService lifecycle = mock(LearningCandidateLifecycleService.class);
    LearningCandidateEvaluationStore evaluations = mock(LearningCandidateEvaluationStore.class);
    LearningCandidateState alreadyApproved =
        new LearningCandidateState(candidateId, LearningCandidateStatus.APPROVED, 2);
    when(lifecycle.beginEvaluation(candidateId)).thenReturn(alreadyApproved);
    LearningCandidateEvaluationReport evaluation = passingEvaluation();
    LearningCandidateEvaluationWorker worker =
        new LearningCandidateEvaluationWorker(lifecycle, evaluations);

    LearningCandidateState result =
        AiExecutionContextScope.call(context(), () -> worker.process(candidateId, evaluation));

    assertThat(result).isEqualTo(alreadyApproved);
    verify(lifecycle).beginEvaluation(candidateId);
    verify(lifecycle, never()).completeEvaluation(candidateId, evaluation.toLifecycleEvaluation());
    verifyNoInteractions(evaluations);
  }

  @Test
  void refusesToProcessWithoutTheBackendExecutionContext() {
    LearningCandidateLifecycleService lifecycle = mock(LearningCandidateLifecycleService.class);
    LearningCandidateEvaluationStore evaluations = mock(LearningCandidateEvaluationStore.class);
    LearningCandidateEvaluationWorker worker =
        new LearningCandidateEvaluationWorker(lifecycle, evaluations);

    assertThatThrownBy(() -> worker.process(UUID.randomUUID(), passingEvaluation()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");

    verifyNoInteractions(lifecycle);
    verifyNoInteractions(evaluations);
  }

  private static LearningCandidateEvaluationReport passingEvaluation() {
    return new LearningCandidateEvaluationReport(
        "eval-1", java.util.Map.of("faithfulness", 0.95), true, true, true, true, false);
  }

  private static AiExecutionContext context() {
    return CONTEXT;
  }
}
