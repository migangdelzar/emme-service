package com.emme.assistant.ai.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Resolves backend-owned identity needed to resume a payment workflow. */
public interface PaymentWorkflowExecutionContextRepository {

  Optional<WorkflowExecutionContext> findByWorkflowId(UUID workflowId);

  record WorkflowExecutionContext(UUID principalId, UUID conversationId, String idempotencyKey) {

    public WorkflowExecutionContext {
      principalId = java.util.Objects.requireNonNull(principalId, "principalId must not be null");
      conversationId =
          java.util.Objects.requireNonNull(conversationId, "conversationId must not be null");
      if (idempotencyKey == null || idempotencyKey.isBlank()) {
        throw new IllegalArgumentException("idempotencyKey must not be blank");
      }
    }
  }
}
