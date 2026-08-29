package com.emme.ai.platform.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.learning.LearningCandidate;
import com.emme.ai.contracts.learning.LearningCandidateEvaluationRequest;
import com.emme.ai.contracts.learning.LearningCandidateEvidence;
import com.emme.ai.contracts.learning.LearningCandidateKind;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LearningCandidateServiceTest {

  private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID PRINCIPAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID CONVERSATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000003");
  private static final UUID WORKFLOW_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

  private final LearningCandidatePolicy policy = new LearningCandidatePolicy();

  @Test
  void persistsAnAdmittedCandidateWithTheAuthenticatedExecutionContext() {
    LearningCandidate candidate = candidate(true, true, true, true, false, false, true);
    LearningCandidateStore store = mock(LearningCandidateStore.class);
    LearningCandidateEvaluationRequester requester =
        mock(LearningCandidateEvaluationRequester.class);
    UUID candidateId = UUID.randomUUID();
    when(store.save(candidate, context())).thenReturn(candidateId);
    LearningCandidateService service = new LearningCandidateService(policy, store, requester);

    LearningCandidateSubmission submission =
        AiExecutionContextScope.call(context(), () -> service.submit(candidate));

    assertThat(submission.accepted()).isTrue();
    assertThat(submission.candidateId()).contains(candidateId);
    verify(store).save(candidate, context());
    ArgumentCaptor<LearningCandidateEvaluationRequest> request =
        ArgumentCaptor.forClass(LearningCandidateEvaluationRequest.class);
    verify(requester).request(request.capture());
    assertThat(request.getValue().candidateId()).isEqualTo(candidateId);
    assertThat(request.getValue().tenantId()).isEqualTo(TENANT_ID);
    assertThat(request.getValue().principalId()).isEqualTo(PRINCIPAL_ID);
    assertThat(request.getValue().conversationId()).isEqualTo(CONVERSATION_ID);
    assertThat(request.getValue().workflowId()).isEqualTo(WORKFLOW_ID);
  }

  @Test
  void rejectsACandidateBeforePersistenceWhenTheEvidenceGateFails() {
    LearningCandidate candidate = candidate(true, true, true, false, false, false, true);
    LearningCandidateStore store = mock(LearningCandidateStore.class);
    LearningCandidateEvaluationRequester requester =
        mock(LearningCandidateEvaluationRequester.class);
    LearningCandidateService service = new LearningCandidateService(policy, store, requester);

    LearningCandidateSubmission submission =
        AiExecutionContextScope.call(context(), () -> service.submit(candidate));

    assertThat(submission.accepted()).isFalse();
    assertThat(submission.candidateId()).isEmpty();
    assertThat(submission.reason()).isEqualTo("accepted outcome evidence is required");
    verifyNoInteractions(store);
    verifyNoInteractions(requester);
  }

  @Test
  void refusesSubmissionWithoutTheBackendExecutionContext() {
    LearningCandidateService service =
        new LearningCandidateService(policy, mock(LearningCandidateStore.class));

    assertThatThrownBy(() -> service.submit(candidate(true, true, true, true, false, false, true)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  private static LearningCandidate candidate(
      boolean routeAccepted,
      boolean executionSucceeded,
      boolean outcomeValidated,
      boolean acceptedOutcome,
      boolean staffCorrected,
      boolean policyViolation,
      boolean piiRedacted) {
    return new LearningCandidate(
        "intent:es-MX:service-information",
        LearningCandidateKind.INTENT_EXAMPLE,
        "what services do you offer?",
        "es-MX",
        "embeddinggemma:1",
        new LearningCandidateEvidence(
            routeAccepted,
            executionSucceeded,
            outcomeValidated,
            acceptedOutcome,
            staffCorrected,
            policyViolation,
            piiRedacted));
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        TENANT_ID,
        PRINCIPAL_ID,
        Set.of("ROLE_CLIENT"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-1",
        "idem-1");
  }
}
