package com.emme.assistant.ai.application.port.out;

import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;

/** Atomic checkpoint boundary for payment workflow resume and terminal state. */
public interface PaymentWorkflowCheckpointRepository extends WorkflowCheckpointRepository {

  boolean claimForResume(AiExecutionContext context);

  @Override
  default boolean claimForResume(AiExecutionContext context, WorkflowStatus expectedStatus) {
    if (expectedStatus != WorkflowStatus.WAITING_FOR_PAYMENT) {
      throw new IllegalArgumentException("Payment checkpoints require WAITING_FOR_PAYMENT");
    }
    return claimForResume(context);
  }
}
