package com.emme.assistant.ai.adapter.out.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.ai.contracts.payment.PaymentLink;
import com.emme.ai.contracts.workflow.WorkflowCommand;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.appointments.api.usecase.CreateAppointmentHoldUseCase;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowCheckpointRepository;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.payment.api.usecase.CreatePaymentLinkUseCase;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentBookingWorkflowTest {

  @Test
  void waitsForConfirmationBeforeCreatingAMutation() {
    CreateAppointmentHoldUseCase holds = mock(CreateAppointmentHoldUseCase.class);
    CreatePaymentLinkUseCase links = mock(CreatePaymentLinkUseCase.class);
    PaymentWorkflowCheckpointRepository checkpoints =
        mock(PaymentWorkflowCheckpointRepository.class);
    AiExecutionContext context = context();
    WorkflowCommand command =
        new WorkflowCommand(
            context.workflowId(),
            "appointment_booking",
            Map.of("appointmentId", UUID.randomUUID().toString(), "requiresPayment", true),
            context.idempotencyKey());

    var workflow = new AppointmentBookingWorkflow(holds, links, checkpoints);
    var handle = workflow.startOrResume(command, context);

    assertThat(handle.status()).isEqualTo(WorkflowStatus.WAITING_FOR_CONFIRMATION);
    assertThat(handle.version()).isZero();
    verifyNoInteractions(holds, links);
    verify(checkpoints).record(context, handle);
  }

  @Test
  void createsAHoldAndPaymentLinkOnlyAfterConfirmation() {
    CreateAppointmentHoldUseCase holds = mock(CreateAppointmentHoldUseCase.class);
    CreatePaymentLinkUseCase links = mock(CreatePaymentLinkUseCase.class);
    PaymentWorkflowCheckpointRepository checkpoints =
        mock(PaymentWorkflowCheckpointRepository.class);
    AiExecutionContext context = context();
    UUID appointmentId = UUID.randomUUID();
    UUID holdId = UUID.randomUUID();
    when(holds.create(anyHold(appointmentId, context.idempotencyKey())))
        .thenReturn(
            new AppointmentHold(
                holdId,
                appointmentId,
                Instant.parse("2030-01-01T09:15:00Z"),
                context.idempotencyKey()));
    when(links.create(anyPaymentLink(context.workflowId(), holdId)))
        .thenReturn(
            new PaymentLink(
                UUID.randomUUID(),
                context.workflowId(),
                "mock",
                "https://pay.test/1",
                Instant.parse("2030-01-01T09:15:00Z")));
    WorkflowCommand command =
        new WorkflowCommand(
            context.workflowId(),
            "appointment_booking",
            Map.of(
                "appointmentId", appointmentId.toString(),
                "confirmed", true,
                "requiresPayment", true),
            context.idempotencyKey());

    var workflow = new AppointmentBookingWorkflow(holds, links, checkpoints);
    var handle = workflow.startOrResume(command, context);

    assertThat(handle.status()).isEqualTo(WorkflowStatus.WAITING_FOR_PAYMENT);
    assertThat(handle.version()).isEqualTo(1);
    verify(holds).create(anyHold(appointmentId, context.idempotencyKey()));
    verify(links).create(anyPaymentLink(context.workflowId(), holdId));
    verify(checkpoints).record(context, handle);
  }

  private static com.emme.appointments.api.command.CreateAppointmentHoldCommand anyHold(
      UUID appointmentId, String idempotencyKey) {
    return new com.emme.appointments.api.command.CreateAppointmentHoldCommand(
        appointmentId, idempotencyKey);
  }

  private static com.emme.payment.api.command.CreatePaymentLinkCommand anyPaymentLink(
      UUID workflowId, UUID holdId) {
    return new com.emme.payment.api.command.CreatePaymentLinkCommand(
        workflowId, holdId, "workflow-payment-" + workflowId);
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
