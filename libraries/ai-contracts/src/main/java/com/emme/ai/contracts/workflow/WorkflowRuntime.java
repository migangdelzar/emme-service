package com.emme.ai.contracts.workflow;

import com.emme.kernel.context.AiExecutionContext;

/** Durable workflow orchestration port; graph-library types remain behind the platform adapter. */
public interface WorkflowRuntime {

  WorkflowHandle start(WorkflowCommand command, AiExecutionContext context);

  WorkflowHandle resume(WorkflowCommand command, AiExecutionContext context);

  WorkflowHandle interrupt(
      java.util.UUID workflowId, InterruptReason reason, AiExecutionContext context);
}
