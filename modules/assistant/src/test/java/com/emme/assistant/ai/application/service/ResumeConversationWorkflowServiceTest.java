package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.api.command.ResumeConversationWorkflowCommand;
import com.emme.assistant.ai.api.command.WorkflowClarificationCommand;
import com.emme.assistant.ai.api.result.ProcessConversationResult;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowFinalizationPort;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowPort;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowReviewAuditPort;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowDecision;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResumeConversationWorkflowServiceTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID OWNER_ID = UUID.randomUUID();
  private static final UUID STAFF_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void allowsAnotherAuthorizedStaffMemberToFinalizeTheOwnerConversationTurn() {
    ConversationWorkflowPort workflow = mock(ConversationWorkflowPort.class);
    ConversationWorkflowFinalizationPort finalization =
        mock(ConversationWorkflowFinalizationPort.class);
    ConversationWorkflowReviewAuditPort audit = mock(ConversationWorkflowReviewAuditPort.class);
    ConversationWorkflowSnapshot snapshot = succeededSnapshot();
    when(workflow.resume(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(snapshot);
    when(finalization.finalize(snapshot))
        .thenReturn(new ProcessConversationResult(CONVERSATION_ID, WORKFLOW_ID, "approved quote"));
    ResumeConversationWorkflowService service =
        new ResumeConversationWorkflowService(workflow, finalization, audit);
    ResumeConversationWorkflowCommand command =
        new ResumeConversationWorkflowCommand(
            WORKFLOW_ID, CONVERSATION_ID, ConversationWorkflowDecision.APPROVE);

    var result = AiExecutionContextScope.call(staffContext(), () -> service.resume(command));

    assertThat(result.status()).isEqualTo(ConversationWorkflowStatus.SUCCEEDED);
    verify(workflow).resume(command, staffContext());
    verify(audit).record(snapshot, command, staffContext());
    verify(finalization).finalize(snapshot);
  }

  @Test
  void deniesAClientFromApprovingAnotherUsersWorkflow() {
    ResumeConversationWorkflowService service =
        new ResumeConversationWorkflowService(
            mock(ConversationWorkflowPort.class),
            mock(ConversationWorkflowFinalizationPort.class),
            mock(ConversationWorkflowReviewAuditPort.class));
    ResumeConversationWorkflowCommand command =
        new ResumeConversationWorkflowCommand(
            WORKFLOW_ID, CONVERSATION_ID, ConversationWorkflowDecision.APPROVE);

    assertThatThrownBy(
            () -> AiExecutionContextScope.call(clientContext(), () -> service.resume(command)))
        .isInstanceOf(SecurityException.class)
        .hasMessage("Staff role is required to resume a conversation workflow");
  }

  @Test
  void rejectsAResumeWhenThePersistedWorkflowBelongsToAnotherTenant() {
    ConversationWorkflowPort workflow = mock(ConversationWorkflowPort.class);
    ConversationWorkflowSnapshot foreignTenantSnapshot =
        new ConversationWorkflowSnapshot(
            WORKFLOW_ID,
            CONVERSATION_ID,
            ConversationWorkflowStatus.SUCCEEDED,
            UUID.randomUUID(),
            OWNER_ID,
            "original turn",
            "original-idempotency-key",
            "approved quote");
    when(workflow.resume(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(foreignTenantSnapshot);
    ResumeConversationWorkflowService service =
        new ResumeConversationWorkflowService(
            workflow,
            mock(ConversationWorkflowFinalizationPort.class),
            mock(ConversationWorkflowReviewAuditPort.class));

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    staffContext(),
                    () ->
                        service.resume(
                            new ResumeConversationWorkflowCommand(
                                WORKFLOW_ID,
                                CONVERSATION_ID,
                                ConversationWorkflowDecision.APPROVE))))
        .isInstanceOf(SecurityException.class)
        .hasMessage("Workflow does not belong to the authenticated tenant");
  }

  @Test
  void requiresStructuredClarificationInsteadOfAllowingApprovalToBypassMissingData() {
    ResumeConversationWorkflowCommand command =
        new ResumeConversationWorkflowCommand(
            WORKFLOW_ID,
            CONVERSATION_ID,
            ConversationWorkflowDecision.PROVIDE_CLARIFICATION,
            new WorkflowClarificationCommand(
                "Friday afternoon", java.util.Map.of("date", "Friday")));

    assertThat(command.clarification().answer()).isEqualTo("Friday afternoon");
    assertThat(command.clarification().slots()).containsEntry("date", "Friday");
  }

  private static ConversationWorkflowSnapshot succeededSnapshot() {
    return new ConversationWorkflowSnapshot(
        WORKFLOW_ID,
        CONVERSATION_ID,
        ConversationWorkflowStatus.SUCCEEDED,
        TENANT_ID,
        OWNER_ID,
        "original turn",
        "original-idempotency-key",
        "approved quote");
  }

  private static AiExecutionContext staffContext() {
    return new AiExecutionContext(
        TENANT_ID,
        STAFF_ID,
        Set.of("ROLE_tenant_staff"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-staff",
        "staff-resume-key");
  }

  private static AiExecutionContext clientContext() {
    return new AiExecutionContext(
        TENANT_ID,
        OWNER_ID,
        Set.of("ROLE_CLIENT"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-client",
        "client-resume-key");
  }
}
