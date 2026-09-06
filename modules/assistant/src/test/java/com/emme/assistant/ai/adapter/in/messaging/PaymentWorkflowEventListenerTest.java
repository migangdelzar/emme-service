package com.emme.assistant.ai.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.ai.contracts.workflow.PaymentWorkflow;
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
    PaymentWorkflowEvent event = event();
    when(contexts.findByWorkflowId(WORKFLOW_ID))
        .thenReturn(
            Optional.of(
                new PaymentWorkflowExecutionContextRepository.WorkflowExecutionContext(
                    PRINCIPAL_ID, CONVERSATION_ID, "workflow-idempotency")));
    PaymentWorkflowEventListener listener = new PaymentWorkflowEventListener(workflow, contexts);
    ArgumentCaptor<AiExecutionContext> captured = ArgumentCaptor.forClass(AiExecutionContext.class);

    listener.onPaymentWorkflow(event);

    verify(workflow).resume(eq(event), captured.capture());
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
    when(contexts.findByWorkflowId(WORKFLOW_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> new PaymentWorkflowEventListener(workflow, contexts).onPaymentWorkflow(event()))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("workflow context");
  }

  private static PaymentWorkflowEvent event() {
    return new PaymentWorkflowEvent(
        TENANT_ID, WORKFLOW_ID, "mock", "event-1", "provider-1", "CAPTURED");
  }
}
