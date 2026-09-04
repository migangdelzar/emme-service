package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.adapter.out.persistence.entity.ConversationWorkflowReviewDecisionEntity;
import com.emme.assistant.ai.adapter.out.persistence.repository.SpringDataConversationWorkflowReviewDecisionRepository;
import com.emme.assistant.ai.api.command.ResumeConversationWorkflowCommand;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowReviewAuditPort;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.kernel.context.AiExecutionContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** JPA adapter for the append-only conversation workflow review audit. */
@Component
public final class JpaConversationWorkflowReviewAuditAdapter
    implements ConversationWorkflowReviewAuditPort {

  private final SpringDataConversationWorkflowReviewDecisionRepository repository;
  private final ObjectMapper objectMapper;

  public JpaConversationWorkflowReviewAuditAdapter(
      SpringDataConversationWorkflowReviewDecisionRepository repository,
      ObjectMapper objectMapper) {
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public void record(
      ConversationWorkflowSnapshot workflow,
      ResumeConversationWorkflowCommand command,
      AiExecutionContext reviewerContext) {
    if (!reviewerContext.tenantId().equals(workflow.tenantId())
        || !command.workflowId().equals(workflow.workflowId())
        || !command.conversationId().equals(workflow.conversationId())) {
      throw new SecurityException("Workflow review audit does not match the authenticated context");
    }
    repository.save(
        new ConversationWorkflowReviewDecisionEntity(
            reviewerContext.tenantId(),
            workflow.workflowId(),
            workflow.conversationId(),
            reviewerContext.principalId(),
            command.decision().name(),
            serialize(command)));
  }

  private String serialize(ResumeConversationWorkflowCommand command) {
    try {
      return objectMapper.writeValueAsString(command.clarification());
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize workflow clarification", exception);
    }
  }
}
