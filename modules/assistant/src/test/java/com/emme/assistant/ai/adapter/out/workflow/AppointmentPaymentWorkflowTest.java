package com.emme.assistant.ai.adapter.out.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.ai.contracts.workflow.WorkflowHandle;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.appointments.api.usecase.ConfirmAppointmentUseCase;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowAppointmentRepository;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentPaymentWorkflowTest {

  @Test
  void waitsForPaymentUntilTheVerifiedEventIsCaptured() {
    AiExecutionContext context = context();
    AppointmentPaymentWorkflow workflow =
        new AppointmentPaymentWorkflow(
            mock(ConfirmAppointmentUseCase.class),
            mock(PaymentWorkflowAppointmentRepository.class));

    WorkflowHandle handle =
        workflow.resume(
            new PaymentWorkflowEvent(
                context.tenantId(),
                context.workflowId(),
                "mock",
                "event-1",
                "provider-1",
                "PENDING"),
            context);

    assertThat(handle.status()).isEqualTo(WorkflowStatus.WAITING_FOR_PAYMENT);
    assertThat(handle.version()).isZero();
  }

  @Test
  void completesAfterTheVerifiedEventIsCaptured() {
    AiExecutionContext context = context();
    UUID appointmentId = UUID.randomUUID();
    ConfirmAppointmentUseCase confirmations = mock(ConfirmAppointmentUseCase.class);
    PaymentWorkflowAppointmentRepository appointments =
        mock(PaymentWorkflowAppointmentRepository.class);
    when(appointments.findAppointmentIdByWorkflowId(context.workflowId()))
        .thenReturn(java.util.Optional.of(appointmentId));
    WorkflowHandle handle =
        new AppointmentPaymentWorkflow(confirmations, appointments)
            .resume(
                new PaymentWorkflowEvent(
                    context.tenantId(),
                    context.workflowId(),
                    "mock",
                    "event-1",
                    "provider-1",
                    "CAPTURED"),
                context);

    assertThat(handle.status()).isEqualTo(WorkflowStatus.SUCCEEDED);
    assertThat(handle.version()).isEqualTo(1);
    verify(confirmations).confirm(appointmentId);
  }

  @Test
  void rejectsAnEventForAnotherWorkflow() {
    AiExecutionContext context = context();

    assertThatThrownBy(
            () ->
                new AppointmentPaymentWorkflow(
                        mock(ConfirmAppointmentUseCase.class),
                        mock(PaymentWorkflowAppointmentRepository.class))
                    .resume(
                        new PaymentWorkflowEvent(
                            context.tenantId(),
                            UUID.randomUUID(),
                            "mock",
                            "event-1",
                            "provider-1",
                            "CAPTURED"),
                        context))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("workflowId");
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-1",
        "workflow-1");
  }
}
