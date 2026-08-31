package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.command.ResumeConversationWorkflowCommand;
import com.emme.assistant.ai.api.usecase.ResumeConversationWorkflowUseCase;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowFinalizationPort;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowPort;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowReviewAuditPort;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowDecision;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application boundary for authenticated resume decisions. */
@Service
@ConditionalOnBean(ConversationWorkflowPort.class)
@Transactional
public class ResumeConversationWorkflowService implements ResumeConversationWorkflowUseCase {

  private static final Set<String> STAFF_ROLES =
      Set.of(
          "tenant_staff",
          "tenant_owner",
          "ROLE_tenant_staff",
          "ROLE_tenant_owner",
          "ROLE_STAFF",
          "ROLE_OWNER",
          "ROLE_ADMIN",
          "ROLE_admin",
          "admin");

  private final ConversationWorkflowPort workflow;
  private final ConversationWorkflowFinalizationPort finalization;
  private final ConversationWorkflowReviewAuditPort audit;

  public ResumeConversationWorkflowService(
      ConversationWorkflowPort workflow,
      ObjectProvider<ConversationWorkflowFinalizationPort> finalization,
      ObjectProvider<ConversationWorkflowReviewAuditPort> audit) {
    this(
        workflow,
        finalization.getIfAvailable(
            () ->
                snapshot -> {
                  throw new IllegalStateException(
                      "Conversation workflow finalization is unavailable");
                }),
        audit.getIfAvailable(() -> (snapshot, command, context) -> {}));
  }

  public ResumeConversationWorkflowService(
      ConversationWorkflowPort workflow,
      ConversationWorkflowFinalizationPort finalization,
      ConversationWorkflowReviewAuditPort audit) {
    this.workflow = Objects.requireNonNull(workflow, "workflow must not be null");
    this.finalization = Objects.requireNonNull(finalization, "finalization must not be null");
    this.audit = Objects.requireNonNull(audit, "audit must not be null");
  }

  @Override
  public ConversationWorkflowSnapshot resume(ResumeConversationWorkflowCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    if (requiresStaffDecision(command.decision())
        && context.roles().stream().noneMatch(STAFF_ROLES::contains)) {
      throw new SecurityException("Staff role is required to resume a conversation workflow");
    }
    if (!context.workflowId().equals(command.workflowId())) {
      throw new SecurityException("Workflow does not match the authenticated AI context");
    }
    if (!context.conversationId().equals(command.conversationId())) {
      throw new SecurityException("Conversation does not match the authenticated AI context");
    }
    ConversationWorkflowSnapshot snapshot = workflow.resume(command, context);
    validateOwnership(snapshot, command, context);
    audit.record(snapshot, command, context);
    if (snapshot.status() == ConversationWorkflowStatus.SUCCEEDED) {
      finalization.finalize(snapshot);
    }
    return snapshot;
  }

  private static boolean requiresStaffDecision(ConversationWorkflowDecision decision) {
    return decision != ConversationWorkflowDecision.PROVIDE_CLARIFICATION;
  }

  private static void validateOwnership(
      ConversationWorkflowSnapshot snapshot,
      ResumeConversationWorkflowCommand command,
      AiExecutionContext context) {
    Objects.requireNonNull(snapshot, "workflow must return a snapshot");
    if (!context.tenantId().equals(snapshot.tenantId())) {
      throw new SecurityException("Workflow does not belong to the authenticated tenant");
    }
    if (!command.workflowId().equals(snapshot.workflowId())) {
      throw new SecurityException("Workflow result does not match the requested workflow");
    }
    if (!command.conversationId().equals(snapshot.conversationId())) {
      throw new SecurityException("Workflow result does not match the requested conversation");
    }
  }
}
