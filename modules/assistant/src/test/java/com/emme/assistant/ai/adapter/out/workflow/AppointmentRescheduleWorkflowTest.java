package com.emme.assistant.ai.adapter.out.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.workflow.WorkflowCommand;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.api.usecase.RescheduleAuthorizedAppointmentUseCase;
import com.emme.appointments.domain.model.AppointmentStatus;
import com.emme.assistant.ai.application.port.out.WorkflowCheckpointRepository;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentRescheduleWorkflowTest {

  @Test
  void waitsForConfirmationBeforeCallingTheAuthorizedAppointmentUseCase() {
    RescheduleAuthorizedAppointmentUseCase rescheduling =
        mock(RescheduleAuthorizedAppointmentUseCase.class);
    WorkflowCheckpointRepository checkpoints = mock(WorkflowCheckpointRepository.class);
    when(checkpoints.claimForResume(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);
    AiExecutionContext context = context();
    WorkflowCommand command =
        new WorkflowCommand(
            context.workflowId(),
            "appointment_reschedule",
            Map.of(
                "appointmentId", UUID.randomUUID().toString(),
                "startsAt", "2030-01-01T10:00:00Z",
                "endsAt", "2030-01-01T11:00:00Z"),
            context.idempotencyKey());

    var workflow = new AppointmentRescheduleWorkflow(rescheduling, checkpoints);

    var handle = workflow.startOrResume(command, context);

    assertThat(handle.workflowId()).isEqualTo(context.workflowId());
    assertThat(handle.status()).isEqualTo(WorkflowStatus.WAITING_FOR_CONFIRMATION);
    verifyNoInteractions(rescheduling);
    verify(checkpoints).record(context, handle);
  }

  @Test
  void invokesTheAuthorizedUseCaseOnlyAfterConfirmation() {
    RescheduleAuthorizedAppointmentUseCase rescheduling =
        mock(RescheduleAuthorizedAppointmentUseCase.class);
    WorkflowCheckpointRepository checkpoints = mock(WorkflowCheckpointRepository.class);
    when(checkpoints.claimForResume(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);
    AiExecutionContext context = context();
    UUID appointmentId = UUID.randomUUID();
    Instant startsAt = Instant.parse("2030-01-01T10:00:00Z");
    Instant endsAt = Instant.parse("2030-01-01T11:00:00Z");
    when(rescheduling.reschedule(org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            new AppointmentDetails(
                appointmentId,
                UUID.randomUUID(),
                "Customer",
                UUID.randomUUID(),
                "Service",
                UUID.randomUUID(),
                "Artist",
                startsAt,
                endsAt,
                AppointmentStatus.CONFIRMED));
    WorkflowCommand command =
        new WorkflowCommand(
            context.workflowId(),
            "appointment_reschedule",
            Map.of(
                "appointmentId", appointmentId.toString(),
                "startsAt", startsAt.toString(),
                "endsAt", endsAt.toString(),
                "confirmed", true),
            context.idempotencyKey());

    var handle =
        new AppointmentRescheduleWorkflow(rescheduling, checkpoints)
            .startOrResume(command, context);

    assertThat(handle.status()).isEqualTo(WorkflowStatus.SUCCEEDED);
    verify(rescheduling)
        .reschedule(
            new com.emme.appointments.api.command.RescheduleAppointmentCommand(
                new com.emme.appointments.api.type.AppointmentActor(
                    context.tenantId(),
                    context.principalId(),
                    context.roles(),
                    context.idempotencyKey()),
                appointmentId,
                startsAt,
                endsAt,
                true));
    verify(checkpoints).record(context, handle);
  }

  @Test
  void doesNotRepeatTheMutationWhenTheConfirmationCheckpointWasAlreadyClaimed() {
    RescheduleAuthorizedAppointmentUseCase rescheduling =
        mock(RescheduleAuthorizedAppointmentUseCase.class);
    WorkflowCheckpointRepository checkpoints = mock(WorkflowCheckpointRepository.class);
    when(checkpoints.claimForResume(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(false);
    AiExecutionContext context = context();
    WorkflowCommand command =
        new WorkflowCommand(
            context.workflowId(),
            "appointment_reschedule",
            Map.of(
                "appointmentId",
                UUID.randomUUID().toString(),
                "startsAt",
                "2030-01-01T10:00:00Z",
                "endsAt",
                "2030-01-01T11:00:00Z",
                "confirmed",
                true),
            context.idempotencyKey());

    var handle =
        new AppointmentRescheduleWorkflow(rescheduling, checkpoints)
            .startOrResume(command, context);

    assertThat(handle.status()).isEqualTo(WorkflowStatus.WAITING_FOR_CONFIRMATION);
    verifyNoInteractions(rescheduling);
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-reschedule",
        "idempotency-reschedule");
  }
}
