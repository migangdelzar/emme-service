package com.emme.assistant.ai.adapter.out.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.workflow.WorkflowCommand;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.api.type.AppointmentStatus;
import com.emme.appointments.api.usecase.CancelAuthorizedAppointmentUseCase;
import com.emme.assistant.ai.application.port.out.WorkflowCheckpointRepository;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentCancellationWorkflowTest {

  @Test
  void waitsForConfirmationBeforeCallingTheAuthorizedAppointmentUseCase() {
    CancelAuthorizedAppointmentUseCase cancellation =
        mock(CancelAuthorizedAppointmentUseCase.class);
    WorkflowCheckpointRepository checkpoints = mock(WorkflowCheckpointRepository.class);
    when(checkpoints.claimForResume(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);
    AiExecutionContext context = context();
    WorkflowCommand command =
        new WorkflowCommand(
            context.workflowId(),
            "appointment_cancellation",
            Map.of("appointmentId", UUID.randomUUID().toString()),
            context.idempotencyKey());

    var workflow = new AppointmentCancellationWorkflow(cancellation, checkpoints);

    var handle = workflow.startOrResume(command, context);

    assertThat(handle.workflowId()).isEqualTo(context.workflowId());
    assertThat(handle.status()).isEqualTo(WorkflowStatus.WAITING_FOR_CONFIRMATION);
    verifyNoInteractions(cancellation);
    verify(checkpoints).record(context, handle);
  }

  @Test
  void invokesTheAuthorizedUseCaseOnlyAfterConfirmation() {
    CancelAuthorizedAppointmentUseCase cancellation =
        mock(CancelAuthorizedAppointmentUseCase.class);
    WorkflowCheckpointRepository checkpoints = mock(WorkflowCheckpointRepository.class);
    when(checkpoints.claimForResume(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);
    AiExecutionContext context = context();
    UUID appointmentId = UUID.randomUUID();
    when(cancellation.cancel(org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            new AppointmentDetails(
                appointmentId,
                UUID.randomUUID(),
                "Customer",
                UUID.randomUUID(),
                "Service",
                UUID.randomUUID(),
                "Artist",
                Instant.parse("2030-01-01T10:00:00Z"),
                Instant.parse("2030-01-01T11:00:00Z"),
                AppointmentStatus.CANCELLED));
    WorkflowCommand command =
        new WorkflowCommand(
            context.workflowId(),
            "appointment_cancellation",
            Map.of("appointmentId", appointmentId.toString(), "confirmed", true),
            context.idempotencyKey());

    var handle =
        new AppointmentCancellationWorkflow(cancellation, checkpoints)
            .startOrResume(command, context);

    assertThat(handle.status()).isEqualTo(WorkflowStatus.SUCCEEDED);
    verify(cancellation)
        .cancel(
            new com.emme.appointments.api.command.CancelAppointmentCommand(
                new com.emme.appointments.api.type.AppointmentActor(
                    context.tenantId(),
                    context.principalId(),
                    context.roles(),
                    context.idempotencyKey()),
                appointmentId,
                true));
    verify(checkpoints).record(context, handle);
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-cancellation",
        "idempotency-cancellation");
  }
}
