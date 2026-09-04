package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.api.command.ResumeConversationWorkflowCommand;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowReviewAuditPort;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowDecision;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class JdbcConversationWorkflowReviewAuditAdapterTest {

  @Test
  void recordsAnAuthorizedDecisionAsTenantScopedJsonbSql() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcConversationWorkflowReviewAuditAdapter adapter =
        new JdbcConversationWorkflowReviewAuditAdapter(jdbc, new ObjectMapper());
    UUID tenantId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID reviewerId = UUID.randomUUID();
    ConversationWorkflowSnapshot workflow =
        new ConversationWorkflowSnapshot(
            workflowId,
            conversationId,
            ConversationWorkflowStatus.WAITING_FOR_APPROVAL,
            tenantId,
            UUID.randomUUID());
    ResumeConversationWorkflowCommand command =
        new ResumeConversationWorkflowCommand(
            workflowId, conversationId, ConversationWorkflowDecision.APPROVE);
    AiExecutionContext context =
        new AiExecutionContext(
            tenantId,
            reviewerId,
            Set.of("ROLE_STAFF"),
            conversationId,
            workflowId,
            "trace-1",
            "idempotency-1");

    adapter.record(workflow, command, context);

    verify(jdbc).sql(org.mockito.ArgumentMatchers.contains("CAST(:clarification AS jsonb)"));
    verify(statement).param("tenantId", tenantId);
    verify(statement).param("workflowId", workflowId);
    verify(statement).param("conversationId", conversationId);
    verify(statement).param("reviewerId", reviewerId);
  }

  @Test
  void rejectsAReviewThatDoesNotMatchTheAuthenticatedWorkflowContext() {
    ConversationWorkflowReviewAuditPort adapter =
        new JdbcConversationWorkflowReviewAuditAdapter(mock(JdbcClient.class), new ObjectMapper());
    UUID tenantId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    ConversationWorkflowSnapshot workflow =
        new ConversationWorkflowSnapshot(
            workflowId,
            conversationId,
            ConversationWorkflowStatus.WAITING_FOR_APPROVAL,
            tenantId,
            UUID.randomUUID());
    ResumeConversationWorkflowCommand command =
        new ResumeConversationWorkflowCommand(
            UUID.randomUUID(), conversationId, ConversationWorkflowDecision.APPROVE);
    AiExecutionContext context =
        new AiExecutionContext(
            tenantId,
            UUID.randomUUID(),
            Set.of("ROLE_STAFF"),
            conversationId,
            workflowId,
            "trace-1",
            "idempotency-1");

    assertThatThrownBy(() -> adapter.record(workflow, command, context))
        .isInstanceOf(SecurityException.class)
        .hasMessage("Workflow review audit does not match the authenticated context");
  }
}
