package com.emme.assistant.ai.adapter.out.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.assistant.ai.api.command.ProcessConversationCommand;
import com.emme.assistant.ai.api.command.ResumeConversationWorkflowCommand;
import com.emme.assistant.ai.api.command.WorkflowClarificationCommand;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowDecision;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Set;
import java.util.UUID;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.junit.jupiter.api.Test;

class LangGraphConversationWorkflowAdapterTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void startsATrustedWorkflowAndReturnsTheCompletedSnapshot() throws Exception {
    LangGraphConversationWorkflowAdapter adapter = adapter();
    AiExecutionContext context = context();

    var snapshot =
        AiExecutionContextScope.call(
            context,
            () ->
                adapter.startOrResume(
                    new ProcessConversationCommand(CONVERSATION_ID, "hello", "idempotency-2"),
                    context));

    assertThat(snapshot.workflowId()).isEqualTo(WORKFLOW_ID);
    assertThat(snapshot.conversationId()).isEqualTo(CONVERSATION_ID);
    assertThat(snapshot.status()).isEqualTo(ConversationWorkflowStatus.SUCCEEDED);
  }

  @Test
  void rejectsACommandOutsideTheTrustedConversationContext() throws Exception {
    LangGraphConversationWorkflowAdapter adapter = adapter();
    AiExecutionContext context = context();

    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context,
                    () ->
                        adapter.startOrResume(
                            new ProcessConversationCommand(
                                UUID.randomUUID(), "hello", "idempotency-2"),
                            context)))
        .withMessage("conversationId does not match AI execution context");
  }

  @Test
  void resumesAnAuthenticatedApprovalCheckpointWithoutStartingTheWorkflowAgain() throws Exception {
    LangGraphConversationWorkflowAdapter adapter = adapter(approvalCapabilities());
    AiExecutionContext context = context();
    ProcessConversationCommand command =
        new ProcessConversationCommand(CONVERSATION_ID, "please approve this", "idempotency-2");

    var paused =
        AiExecutionContextScope.call(context, () -> adapter.startOrResume(command, context));
    assertThat(paused.status()).isEqualTo(ConversationWorkflowStatus.WAITING_FOR_APPROVAL);

    AiExecutionContext staffContext = staffContext();
    var resumed =
        AiExecutionContextScope.call(
            staffContext,
            () ->
                adapter.resume(
                    new ResumeConversationWorkflowCommand(
                        WORKFLOW_ID, CONVERSATION_ID, ConversationWorkflowDecision.APPROVE),
                    staffContext));

    assertThat(resumed.status()).isEqualTo(ConversationWorkflowStatus.SUCCEEDED);
    assertThat(resumed.workflowId()).isEqualTo(WORKFLOW_ID);
    assertThat(resumed.conversationId()).isEqualTo(CONVERSATION_ID);
  }

  @Test
  void requiresOwnerProvidedClarificationAndDoesNotAllowApprovalToBypassIt() throws Exception {
    LangGraphConversationWorkflowAdapter adapter = adapter(clarificationCapabilities());
    AiExecutionContext owner = context();
    ProcessConversationCommand command =
        new ProcessConversationCommand(CONVERSATION_ID, "need an appointment", "idempotency-2");

    var paused = AiExecutionContextScope.call(owner, () -> adapter.startOrResume(command, owner));
    assertThat(paused.status()).isEqualTo(ConversationWorkflowStatus.CLARIFICATION_REQUIRED);

    AiExecutionContext staff = staffContext();
    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    staff,
                    () ->
                        adapter.resume(
                            new ResumeConversationWorkflowCommand(
                                WORKFLOW_ID, CONVERSATION_ID, ConversationWorkflowDecision.APPROVE),
                            staff)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Clarification is required");

    var resumed =
        AiExecutionContextScope.call(
            owner,
            () ->
                adapter.resume(
                    new ResumeConversationWorkflowCommand(
                        WORKFLOW_ID,
                        CONVERSATION_ID,
                        ConversationWorkflowDecision.PROVIDE_CLARIFICATION,
                        new WorkflowClarificationCommand(
                            "Friday afternoon", java.util.Map.of("day", "Friday"))),
                    owner));
    assertThat(resumed.status()).isEqualTo(ConversationWorkflowStatus.SUCCEEDED);
  }

  @Test
  void rejectsAClientTryingToApproveAStaffWorkflowDecision() throws Exception {
    LangGraphConversationWorkflowAdapter adapter = adapter(approvalCapabilities());
    AiExecutionContext owner = context();
    ProcessConversationCommand command =
        new ProcessConversationCommand(CONVERSATION_ID, "please approve", "idempotency-2");
    AiExecutionContextScope.call(owner, () -> adapter.startOrResume(command, owner));

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    owner,
                    () ->
                        adapter.resume(
                            new ResumeConversationWorkflowCommand(
                                WORKFLOW_ID, CONVERSATION_ID, ConversationWorkflowDecision.APPROVE),
                            owner)))
        .isInstanceOf(SecurityException.class)
        .hasMessage("Staff role is required to resume a conversation workflow");
  }

  private static LangGraphConversationWorkflowAdapter adapter() throws Exception {
    return adapter(ConversationWorkflowCapabilities.defaults());
  }

  private static LangGraphConversationWorkflowAdapter adapter(
      ConversationWorkflowCapabilities capabilities) throws Exception {
    return new LangGraphConversationWorkflowAdapter(
        new ConversationWorkflowGraph(
                new TenantAwareCheckpointSaver(new MemorySaver()), capabilities)
            .compile());
  }

  private static ConversationWorkflowCapabilities approvalCapabilities() {
    ConversationWorkflowCapabilities defaults = ConversationWorkflowCapabilities.defaults();
    return new ConversationWorkflowCapabilities(
        defaults.intentDetection(),
        defaults.decomposition(),
        defaults.semanticRouting(),
        defaults.slotExtraction(),
        defaults.retrieval(),
        defaults.toolExecution(),
        request ->
            new ConversationWorkflowCapabilities.WorkflowStep(
                java.util.Map.of(), true, false, null),
        defaults.responseComposition(),
        defaults.quoteWorkflow());
  }

  private static ConversationWorkflowCapabilities clarificationCapabilities() {
    ConversationWorkflowCapabilities defaults = ConversationWorkflowCapabilities.defaults();
    return new ConversationWorkflowCapabilities(
        defaults.intentDetection(),
        defaults.decomposition(),
        defaults.semanticRouting(),
        defaults.slotExtraction(),
        defaults.retrieval(),
        defaults.toolExecution(),
        request ->
            request.state().containsKey(ConversationWorkflowGraph.CLARIFICATION_ANSWER)
                ? ConversationWorkflowCapabilities.WorkflowStep.empty()
                : new ConversationWorkflowCapabilities.WorkflowStep(
                    java.util.Map.of(), false, false, "CLARIFICATION_REQUIRED"),
        defaults.responseComposition(),
        defaults.quoteWorkflow());
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        TENANT_ID,
        PRINCIPAL_ID,
        Set.of("CLIENT"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-2",
        "idempotency-2");
  }

  private static AiExecutionContext staffContext() {
    return new AiExecutionContext(
        TENANT_ID,
        UUID.randomUUID(),
        Set.of("ROLE_tenant_staff"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-staff",
        "staff-idempotency");
  }
}
