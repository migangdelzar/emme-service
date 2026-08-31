package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.api.command.ResumeConversationWorkflowCommand;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.kernel.context.AiExecutionContext;

/** Durable audit boundary for a staff decision that resumes a conversation workflow. */
@FunctionalInterface
public interface ConversationWorkflowReviewAuditPort {

  void record(
      ConversationWorkflowSnapshot workflow,
      ResumeConversationWorkflowCommand command,
      AiExecutionContext reviewerContext);
}
