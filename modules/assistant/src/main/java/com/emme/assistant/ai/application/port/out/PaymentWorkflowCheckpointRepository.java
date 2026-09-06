package com.emme.assistant.ai.application.port.out;

import com.emme.ai.contracts.workflow.WorkflowHandle;
import com.emme.kernel.context.AiExecutionContext;

/** Atomic checkpoint boundary for payment workflow resume and terminal state. */
public interface PaymentWorkflowCheckpointRepository {

  boolean claimForResume(AiExecutionContext context);

  void record(AiExecutionContext context, WorkflowHandle handle);
}
