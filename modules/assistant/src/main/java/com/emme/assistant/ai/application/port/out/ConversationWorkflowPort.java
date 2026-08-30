package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.api.command.ProcessConversationCommand;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.kernel.context.AiExecutionContext;

/** Starts or returns the persisted generic workflow for an authenticated conversation turn. */
public interface ConversationWorkflowPort {

  ConversationWorkflowSnapshot startOrResume(
      ProcessConversationCommand command, AiExecutionContext context);
}
