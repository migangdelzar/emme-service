package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emme.assistant.ai.adapter.out.persistence.entity.ConversationWorkflowReviewDecisionEntity;
import com.emme.assistant.ai.adapter.out.persistence.repository.SpringDataConversationWorkflowReviewDecisionRepository;
import com.emme.assistant.ai.api.command.ResumeConversationWorkflowCommand;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowDecision;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JpaConversationWorkflowReviewAuditAdapterTest {

  @Test
  void persistsAnAuthorizedReviewDecisionThroughSpringData() {
    SpringDataConversationWorkflowReviewDecisionRepository repository =
        mock(SpringDataConversationWorkflowReviewDecisionRepository.class);
    JpaConversationWorkflowReviewAuditAdapter adapter =
        new JpaConversationWorkflowReviewAuditAdapter(repository, new ObjectMapper());
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
    AiExecutionContext reviewerContext =
        new AiExecutionContext(
            tenantId,
            reviewerId,
            Set.of("ROLE_STAFF"),
            conversationId,
            workflowId,
            "trace-1",
            "idempotency-1");

    adapter.record(workflow, command, reviewerContext);

    ArgumentCaptor<ConversationWorkflowReviewDecisionEntity> saved =
        ArgumentCaptor.forClass(ConversationWorkflowReviewDecisionEntity.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getTenantId()).isEqualTo(tenantId);
    assertThat(saved.getValue().getWorkflowId()).isEqualTo(workflowId);
    assertThat(saved.getValue().getConversationId()).isEqualTo(conversationId);
    assertThat(saved.getValue().getReviewerId()).isEqualTo(reviewerId);
    assertThat(saved.getValue().getDecision())
        .isEqualTo(ConversationWorkflowDecision.APPROVE.name());
    assertThat(saved.getValue().getClarification()).isEqualTo("null");
  }
}
