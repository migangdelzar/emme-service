package com.emme.ai.platform.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.learning.LearningCandidateEvaluation;
import com.emme.ai.contracts.learning.LearningCandidateStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LearningCandidateLifecycleServiceTest {

  private static final UUID CANDIDATE_ID = UUID.randomUUID();
  private final LearningCandidateStateStore stateStore = mock(LearningCandidateStateStore.class);
  private final LearningCandidateLifecycleService service =
      new LearningCandidateLifecycleService(new LearningCandidateLifecyclePolicy(), stateStore);

  @Test
  void beginsEvaluationUsingTheCurrentVersionAndPersistsTheTransition() {
    LearningCandidateState pending =
        new LearningCandidateState(CANDIDATE_ID, LearningCandidateStatus.PENDING_EVALUATION, 3);
    when(stateStore.find(CANDIDATE_ID)).thenReturn(Optional.of(pending));
    when(stateStore.transition(
            CANDIDATE_ID,
            LearningCandidateStatus.PENDING_EVALUATION,
            3,
            LearningCandidateStatus.EVALUATING,
            "evaluation started"))
        .thenReturn(true);

    LearningCandidateState result =
        AiExecutionContextScope.call(context(), () -> service.beginEvaluation(CANDIDATE_ID));

    assertThat(result)
        .isEqualTo(new LearningCandidateState(CANDIDATE_ID, LearningCandidateStatus.EVALUATING, 4));
    verify(stateStore)
        .transition(
            CANDIDATE_ID,
            LearningCandidateStatus.PENDING_EVALUATION,
            3,
            LearningCandidateStatus.EVALUATING,
            "evaluation started");
  }

  @Test
  void persistsEvaluationFailureAsRejected() {
    LearningCandidateState evaluating =
        new LearningCandidateState(CANDIDATE_ID, LearningCandidateStatus.EVALUATING, 4);
    when(stateStore.find(CANDIDATE_ID)).thenReturn(Optional.of(evaluating));
    when(stateStore.transition(
            CANDIDATE_ID,
            LearningCandidateStatus.EVALUATING,
            4,
            LearningCandidateStatus.REJECTED,
            "regression evaluation failed"))
        .thenReturn(true);

    LearningCandidateState result =
        AiExecutionContextScope.call(
            context(),
            () ->
                service.completeEvaluation(
                    CANDIDATE_ID,
                    new LearningCandidateEvaluation("eval-1", true, true, false, true, true)));

    assertThat(result)
        .isEqualTo(new LearningCandidateState(CANDIDATE_ID, LearningCandidateStatus.REJECTED, 5));
  }

  @Test
  void doesNotWriteWhenPromotionCanaryFails() {
    LearningCandidateState approved =
        new LearningCandidateState(CANDIDATE_ID, LearningCandidateStatus.APPROVED, 5);
    when(stateStore.find(CANDIDATE_ID)).thenReturn(Optional.of(approved));

    LearningCandidateState result =
        AiExecutionContextScope.call(
            context(),
            () ->
                service.promote(
                    CANDIDATE_ID,
                    new LearningCandidateEvaluation("eval-1", true, true, true, true, false)));

    assertThat(result).isEqualTo(approved);
  }

  @Test
  void failsWhenAnOptimisticTransitionLosesTheRace() {
    LearningCandidateState pending =
        new LearningCandidateState(CANDIDATE_ID, LearningCandidateStatus.PENDING_EVALUATION, 3);
    when(stateStore.find(CANDIDATE_ID)).thenReturn(Optional.of(pending));
    when(stateStore.transition(
            CANDIDATE_ID,
            LearningCandidateStatus.PENDING_EVALUATION,
            3,
            LearningCandidateStatus.EVALUATING,
            "evaluation started"))
        .thenReturn(false);

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(), () -> service.beginEvaluation(CANDIDATE_ID)))
        .isInstanceOf(LearningCandidateConcurrencyException.class);
  }

  @Test
  void requiresTheCandidateToExist() {
    when(stateStore.find(CANDIDATE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(), () -> service.beginEvaluation(CANDIDATE_ID)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Learning candidate was not found");
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        java.util.Set.of("ROLE_owner"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-learning",
        "idem-learning");
  }
}
