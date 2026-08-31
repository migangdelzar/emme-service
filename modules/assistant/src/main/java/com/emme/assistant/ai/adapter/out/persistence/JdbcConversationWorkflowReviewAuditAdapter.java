package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.api.command.ResumeConversationWorkflowCommand;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowReviewAuditPort;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.kernel.context.AiExecutionContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Persists every authorized human decision that resumes a durable conversation workflow. */
@Component
public final class JdbcConversationWorkflowReviewAuditAdapter
    implements ConversationWorkflowReviewAuditPort {

  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  public JdbcConversationWorkflowReviewAuditAdapter(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
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
    jdbc.sql(
            """
            INSERT INTO ai_conversation_workflow_review_decision (
                tenant_id, workflow_id, conversation_id, reviewer_id, decision, clarification
            )
            VALUES (
                :tenantId, :workflowId, :conversationId, :reviewerId, :decision,
                CAST(:clarification AS jsonb)
            )
            """)
        .param("tenantId", reviewerContext.tenantId())
        .param("workflowId", workflow.workflowId())
        .param("conversationId", workflow.conversationId())
        .param("reviewerId", reviewerContext.principalId())
        .param("decision", command.decision().name())
        .param("clarification", serialize(command))
        .update();
  }

  private String serialize(ResumeConversationWorkflowCommand command) {
    try {
      return objectMapper.writeValueAsString(command.clarification());
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize workflow clarification", exception);
    }
  }
}
