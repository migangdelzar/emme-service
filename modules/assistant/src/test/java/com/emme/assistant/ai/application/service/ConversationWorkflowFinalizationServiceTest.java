package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.ConversationMemoryPort;
import com.emme.assistant.ai.application.port.out.ConversationTurnIdempotencyPort;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationWorkflowFinalizationServiceTest {

  @Test
  void persistsTheCompletedTurnUnderTheOriginalOwnerInsteadOfTheReviewingStaffMember() {
    UUID tenantId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID staffId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();
    ConversationMemoryPort memory = mock(ConversationMemoryPort.class);
    ConversationTurnIdempotencyPort idempotency = mock(ConversationTurnIdempotencyPort.class);
    ConversationWorkflowFinalizationService service =
        new ConversationWorkflowFinalizationService(memory, idempotency);
    ConversationWorkflowSnapshot workflow =
        new ConversationWorkflowSnapshot(
            workflowId,
            conversationId,
            ConversationWorkflowStatus.SUCCEEDED,
            tenantId,
            ownerId,
            "I want this design",
            "turn-1",
            "The approved quote is $500 MXN.");
    AiExecutionContext staffContext =
        new AiExecutionContext(
            tenantId,
            staffId,
            Set.of("ROLE_tenant_staff"),
            conversationId,
            workflowId,
            "trace-review",
            "review-turn");
    AiExecutionContext ownerContext =
        new AiExecutionContext(
            tenantId, ownerId, Set.of(), conversationId, workflowId, "trace-review", "turn-1");
    when(idempotency.find(conversationId, "turn-1")).thenReturn(Optional.empty());
    when(memory.findUserMessage(conversationId, "turn-1", ownerContext))
        .thenReturn(Optional.empty());
    when(memory.findAssistantResponse(conversationId, "turn-1", ownerContext))
        .thenReturn(Optional.empty());

    var result = AiExecutionContextScope.call(staffContext, () -> service.finalize(workflow));

    assertThat(result.response()).isEqualTo("The approved quote is $500 MXN.");
    verify(memory).appendUserMessage(conversationId, "I want this design", "turn-1", ownerContext);
    verify(memory)
        .appendAssistantMessage(
            conversationId, "The approved quote is $500 MXN.", "turn-1", ownerContext);
    verify(idempotency).complete(eq(conversationId), eq("turn-1"), eq(result));
  }
}
