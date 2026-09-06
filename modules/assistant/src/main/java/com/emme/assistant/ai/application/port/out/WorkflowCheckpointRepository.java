package com.emme.assistant.ai.application.port.out;

import com.emme.ai.contracts.workflow.WorkflowHandle;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;

/** Atomic persistence boundary for durable workflow claims and lifecycle handles. */
public interface WorkflowCheckpointRepository {

  boolean claimForResume(AiExecutionContext context, WorkflowStatus expectedStatus);

  void record(AiExecutionContext context, WorkflowHandle handle);
}
