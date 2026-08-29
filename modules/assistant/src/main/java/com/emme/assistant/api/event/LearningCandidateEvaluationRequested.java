package com.emme.assistant.api.event;

import com.emme.ai.contracts.learning.LearningCandidateEvaluationRequest;
import java.util.Objects;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/** Safe application event that starts offline evaluation of a governed learning candidate. */
@Externalized("emme.ai.learning-candidate-evaluation-requested::#{#this.tenantId()}")
public record LearningCandidateEvaluationRequested(LearningCandidateEvaluationRequest request) {

  public LearningCandidateEvaluationRequested {
    Objects.requireNonNull(request, "request must not be null");
  }

  public UUID eventId() {
    return request.eventId();
  }

  public UUID candidateId() {
    return request.candidateId();
  }

  public UUID tenantId() {
    return request.tenantId();
  }

  public UUID principalId() {
    return request.principalId();
  }

  public UUID conversationId() {
    return request.conversationId();
  }

  public UUID workflowId() {
    return request.workflowId();
  }

  public String traceId() {
    return request.traceId();
  }

  public String idempotencyKey() {
    return request.idempotencyKey();
  }
}
