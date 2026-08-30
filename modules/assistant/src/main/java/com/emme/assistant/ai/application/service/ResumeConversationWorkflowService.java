package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.command.ResumeConversationWorkflowCommand;
import com.emme.assistant.ai.api.usecase.ResumeConversationWorkflowUseCase;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowPort;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/** Application boundary for authenticated resume decisions. */
@Service
@ConditionalOnBean(ConversationWorkflowPort.class)
public class ResumeConversationWorkflowService implements ResumeConversationWorkflowUseCase {

  private final ConversationWorkflowPort workflow;

  public ResumeConversationWorkflowService(ConversationWorkflowPort workflow) {
    this.workflow = Objects.requireNonNull(workflow, "workflow must not be null");
  }

  @Override
  public ConversationWorkflowSnapshot resume(ResumeConversationWorkflowCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    if (!context.workflowId().equals(command.workflowId())) {
      throw new SecurityException("Workflow does not match the authenticated AI context");
    }
    if (!context.conversationId().equals(command.conversationId())) {
      throw new SecurityException("Conversation does not match the authenticated AI context");
    }
    return workflow.resume(command, context);
  }
}
