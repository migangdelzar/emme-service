package com.emme.ai.contracts.workflow;

import com.emme.kernel.context.AiExecutionContext;

/** Business-facing conversation workflow capability independent of graph runtime types. */
@FunctionalInterface
public interface ConversationWorkflow {

  WorkflowHandle startOrResume(WorkflowCommand command, AiExecutionContext context);
}
