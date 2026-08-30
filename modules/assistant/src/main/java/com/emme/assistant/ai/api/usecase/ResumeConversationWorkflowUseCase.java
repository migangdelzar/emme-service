package com.emme.assistant.ai.api.usecase;

import com.emme.assistant.ai.api.command.ResumeConversationWorkflowCommand;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;

/** Resumes a tenant- and principal-bound paused conversation workflow. */
public interface ResumeConversationWorkflowUseCase {

  ConversationWorkflowSnapshot resume(ResumeConversationWorkflowCommand command);
}
