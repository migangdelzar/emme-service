package com.emme.ai.contracts.workflow;

import com.emme.kernel.context.AiExecutionContext;

/** Business-facing quote workflow capability independent of graph runtime types. */
public interface QuoteWorkflow {

  WorkflowHandle start(WorkflowCommand command, AiExecutionContext context);

  WorkflowHandle resume(WorkflowCommand command, AiExecutionContext context);
}
