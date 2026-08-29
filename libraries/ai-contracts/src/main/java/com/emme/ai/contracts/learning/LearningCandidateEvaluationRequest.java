package com.emme.ai.contracts.learning;

import com.emme.kernel.context.AiExecutionContext;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/** Trusted, PII-free envelope used to enqueue offline evaluation for a candidate. */
public record LearningCandidateEvaluationRequest(
    UUID eventId,
    UUID candidateId,
    UUID tenantId,
    UUID principalId,
    UUID conversationId,
    UUID workflowId,
    String traceId,
    String idempotencyKey) {

  public LearningCandidateEvaluationRequest {
    eventId = Objects.requireNonNull(eventId, "eventId must not be null");
    candidateId = Objects.requireNonNull(candidateId, "candidateId must not be null");
    tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    principalId = Objects.requireNonNull(principalId, "principalId must not be null");
    conversationId = Objects.requireNonNull(conversationId, "conversationId must not be null");
    workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
    traceId = requireText(traceId, "traceId");
    idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
  }

  /** Builds the envelope exclusively from the backend-bound AI context. */
  public static LearningCandidateEvaluationRequest from(
      UUID candidateId, AiExecutionContext context) {
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(context, "context must not be null");
    UUID eventId =
        UUID.nameUUIDFromBytes(
            ("emme.ai.learning-candidate-evaluation:" + candidateId)
                .getBytes(StandardCharsets.UTF_8));
    return new LearningCandidateEvaluationRequest(
        eventId,
        candidateId,
        context.tenantId(),
        context.principalId(),
        context.conversationId(),
        context.workflowId(),
        context.traceId(),
        context.idempotencyKey());
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
