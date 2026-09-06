package com.emme.assistant.ai.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.ai.contracts.workflow.PaymentWorkflow;
import com.emme.ai.contracts.workflow.WorkflowHandle;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowCheckpointRepository;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowExecutionContextRepository;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaymentWorkflowEventListenerTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();

  @Test
  void restoresTrustedWorkflowContextBeforeResumingPayment() {
    PaymentWorkflow workflow = mock(PaymentWorkflow.class);
    PaymentWorkflowExecutionContextRepository contexts =
        mock(PaymentWorkflowExecutionContextRepository.class);
    PaymentWorkflowCheckpointRepository checkpoints =
        mock(PaymentWorkflowCheckpointRepository.class);
    PaymentWorkflowEvent event = event();
    when(contexts.findByWorkflowId(WORKFLOW_ID))
        .thenReturn(
            Optional.of(
                new PaymentWorkflowExecutionContextRepository.WorkflowExecutionContext(
                    PRINCIPAL_ID, CONVERSATION_ID, "workflow-idempotency")));
    when(checkpoints.claimForResume(any(AiExecutionContext.class))).thenReturn(true);
    when(workflow.resume(eq(event), any(AiExecutionContext.class)))
        .thenReturn(new WorkflowHandle(WORKFLOW_ID, WorkflowStatus.SUCCEEDED, 1));
    PaymentWorkflowEventListener listener =
        new PaymentWorkflowEventListener(workflow, contexts, checkpoints);
    ArgumentCaptor<AiExecutionContext> captured = ArgumentCaptor.forClass(AiExecutionContext.class);

    listener.onPaymentWorkflow(event);

    verify(workflow).resume(eq(event), captured.capture());
    verify(checkpoints)
        .record(
            any(AiExecutionContext.class),
            eq(new WorkflowHandle(WORKFLOW_ID, WorkflowStatus.SUCCEEDED, 1)));
    AiExecutionContext context = captured.getValue();
    assertThat(context.tenantId()).isEqualTo(TENANT_ID);
    assertThat(context.principalId()).isEqualTo(PRINCIPAL_ID);
    assertThat(context.conversationId()).isEqualTo(CONVERSATION_ID);
    assertThat(context.workflowId()).isEqualTo(WORKFLOW_ID);
    assertThat(context.idempotencyKey()).isEqualTo("workflow-idempotency");
    assertThat(context.roles()).isEqualTo(Set.of());
  }

  @Test
  void rejectsCallbacksWithoutPersistedWorkflowOwnership() {
    PaymentWorkflow workflow = mock(PaymentWorkflow.class);
    PaymentWorkflowExecutionContextRepository contexts =
        mock(PaymentWorkflowExecutionContextRepository.class);
    PaymentWorkflowCheckpointRepository checkpoints =
        mock(PaymentWorkflowCheckpointRepository.class);
    when(contexts.findByWorkflowId(WORKFLOW_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new PaymentWorkflowEventListener(workflow, contexts, checkpoints)
                    .onPaymentWorkflow(event()))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("workflow context");
  }

  @Test
  void skipsAResumeWhenTheDurableCheckpointWasAlreadyClaimed() {
    PaymentWorkflow workflow = mock(PaymentWorkflow.class);
    PaymentWorkflowExecutionContextRepository contexts =
        mock(PaymentWorkflowExecutionContextRepository.class);
    PaymentWorkflowCheckpointRepository checkpoints =
        mock(PaymentWorkflowCheckpointRepository.class);
    when(contexts.findByWorkflowId(WORKFLOW_ID))
        .thenReturn(
            Optional.of(
                new PaymentWorkflowExecutionContextRepository.WorkflowExecutionContext(
                    PRINCIPAL_ID, CONVERSATION_ID, "workflow-idempotency")));
    when(checkpoints.claimForResume(any(AiExecutionContext.class))).thenReturn(false);

    new PaymentWorkflowEventListener(workflow, contexts, checkpoints).onPaymentWorkflow(event());

    verifyNoInteractions(workflow);
  }

  @Test
  void releasesTheClaimWhenResumeFailsSoAProviderRetryCanContinue() {
    PaymentWorkflow workflow = mock(PaymentWorkflow.class);
    PaymentWorkflowExecutionContextRepository contexts =
        mock(PaymentWorkflowExecutionContextRepository.class);
    PaymentWorkflowCheckpointRepository checkpoints =
        mock(PaymentWorkflowCheckpointRepository.class);
    when(contexts.findByWorkflowId(WORKFLOW_ID))
        .thenReturn(
            Optional.of(
                new PaymentWorkflowExecutionContextRepository.WorkflowExecutionContext(
                    PRINCIPAL_ID, CONVERSATION_ID, "workflow-idempotency")));
    when(checkpoints.claimForResume(any(AiExecutionContext.class))).thenReturn(true);
    when(workflow.resume(eq(event()), any(AiExecutionContext.class)))
        .thenThrow(new IllegalStateException("appointment confirmation failed"));

    assertThatThrownBy(
            () ->
                new PaymentWorkflowEventListener(workflow, contexts, checkpoints)
                    .onPaymentWorkflow(event()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("appointment confirmation failed");

    verify(checkpoints)
        .record(
            any(AiExecutionContext.class),
            org.mockito.ArgumentMatchers.eq(
                new WorkflowHandle(WORKFLOW_ID, WorkflowStatus.WAITING_FOR_PAYMENT, 0)));
  }

  private static PaymentWorkflowEvent event() {
    return new PaymentWorkflowEvent(
        TENANT_ID, WORKFLOW_ID, "mock", "event-1", "provider-1", "CAPTURED");
  }
}
