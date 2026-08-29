package com.emme.ai.contracts.learning;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.kernel.context.AiExecutionContext;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LearningCandidateEvaluationRequestTest {

  @Test
  void derivesAllDispatchIdentityFromTheBackendExecutionContext() {
    UUID candidateId = UUID.randomUUID();
    AiExecutionContext context =
        new AiExecutionContext(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Set.of("ROLE_CLIENT"),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace-1",
            "idempotency-1");

    LearningCandidateEvaluationRequest request =
        LearningCandidateEvaluationRequest.from(candidateId, context);

    assertThat(request.candidateId()).isEqualTo(candidateId);
    assertThat(request.tenantId()).isEqualTo(context.tenantId());
    assertThat(request.principalId()).isEqualTo(context.principalId());
    assertThat(request.conversationId()).isEqualTo(context.conversationId());
    assertThat(request.workflowId()).isEqualTo(context.workflowId());
    assertThat(request.traceId()).isEqualTo(context.traceId());
    assertThat(request.idempotencyKey()).isEqualTo(context.idempotencyKey());
  }

  @Test
  void usesAStableEventIdentityForRetriesOfTheSameCandidate() {
    UUID candidateId = UUID.randomUUID();
    AiExecutionContext context =
        new AiExecutionContext(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Set.of("ROLE_CLIENT"),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace-1",
            "idempotency-1");

    assertThat(LearningCandidateEvaluationRequest.from(candidateId, context).eventId())
        .isEqualTo(LearningCandidateEvaluationRequest.from(candidateId, context).eventId());
  }
}
